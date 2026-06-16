package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.InterviewAnswerRequest;
import com.aihiringplatform.backend.dto.InterviewEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewEvaluationResponse;
import com.aihiringplatform.backend.dto.InterviewQuestionBreakdownItem;
import com.aihiringplatform.backend.dto.InterviewResultResponse;
import com.aihiringplatform.backend.dto.MonitoringEventDto;
import com.aihiringplatform.backend.dto.QuestionResponse;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.InterviewSessionResponse;
import com.aihiringplatform.backend.dto.InterviewSessionStartRequest;
import com.aihiringplatform.backend.dto.InterviewTranscriptItem;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.MockInterviewSession;
import com.aihiringplatform.backend.entity.MockInterviewStatus;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.MockInterviewSessionRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MockInterviewService {

    private static final Logger logger = LoggerFactory.getLogger(MockInterviewService.class);
    private static final String BUSY_FALLBACK_MESSAGE = "AI provider temporarily busy. Using fallback analysis.";

    @Autowired
    private MockInterviewSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private PdfReportService pdfReportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public InterviewSessionResponse startSession(InterviewSessionStartRequest request, String candidateEmail) {
        User candidate = getCandidate(candidateEmail);
        Application application = resolveApplication(request, candidateEmail);
        Job job = resolveJob(request, application);
        String jobRole = resolveJobRole(request, job);
        List<String> skills = normalizeSkills(request.getSkills(), job);
        String difficulty = normalizeOption(request.getDifficulty(), "Intermediate");
        String interviewType = normalizeOption(request.getInterviewType(), "Mixed");
        String monitoringMode = normalizeOption(request.getMonitoringMode(), "Camera + Voice");
        int duration = normalizeDuration(request.getEstimatedDurationMinutes());
        QuestionResponse questionResponse = buildQuestions(job, jobRole, skills, difficulty, interviewType, duration);
        List<String> questions = questionResponse.getQuestions();

        MockInterviewSession session = new MockInterviewSession();
        session.setCandidate(candidate);
        session.setApplication(application);
        session.setJob(job);
        session.setJobRole(jobRole);
        session.setDifficulty(difficulty);
        session.setInterviewType(interviewType);
        session.setMonitoringMode(monitoringMode);
        session.setEstimatedDurationMinutes(duration);
        session.setSkills(String.join(", ", skills));
        session.setStatus(MockInterviewStatus.IN_PROGRESS);
        session.setQuestionsJson(writeList(questions));
        session.setAnswersJson(writeAnswers(new LinkedHashMap<>()));
        session.setFallbackUsed(Boolean.TRUE.equals(questionResponse.getFallbackUsed()));
        session.setCurrentQuestionIndex(0);
        session.setElapsedSeconds(0);
        session.setStartedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        activityLogService.logSuccess(candidateEmail, "CANDIDATE", "MOCK_INTERVIEW_START", "Candidate started a new mock interview for role: " + jobRole, null);

        return toSessionResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(Long sessionId, String candidateEmail) {
        return toSessionResponse(getOwnedSession(sessionId, candidateEmail));
    }

    @Transactional
    public InterviewSessionResponse saveAnswer(Long sessionId, InterviewAnswerRequest request, String candidateEmail) {
        MockInterviewSession session = getOwnedSession(sessionId, candidateEmail);

        if (session.getStatus() == MockInterviewStatus.COMPLETED) {
            throw new RuntimeException("This interview has already been submitted.");
        }

        List<String> questions = readList(session.getQuestionsJson());
        int questionIndex = request.getQuestionIndex() == null ? -1 : request.getQuestionIndex();

        if (questionIndex < 0 || questionIndex >= questions.size()) {
            throw new RuntimeException("Question index is not valid for this interview.");
        }

        Map<Integer, String> answers = readAnswers(session.getAnswersJson());
        answers.put(questionIndex, sanitizeAnswer(request.getAnswer()));
        session.setAnswersJson(writeAnswers(answers));

        // Store voice transcript if provided
        if (request.getTranscript() != null && !request.getTranscript().isBlank()) {
            Map<Integer, String> transcripts = readAnswers(session.getTranscriptJson());
            transcripts.put(questionIndex, request.getTranscript().trim());
            session.setTranscriptJson(writeAnswers(transcripts));
        }

        // Process monitoring events
        processMonitoringEvents(session, request.getMonitoringEvents());

        // Track device usage
        if (Boolean.TRUE.equals(request.getCameraUsed())) {
            session.setCameraUsed(true);
        }
        if (Boolean.TRUE.equals(request.getMicrophoneUsed())) {
            session.setMicrophoneUsed(true);
        }

        if (request.getCurrentQuestionIndex() != null) {
            session.setCurrentQuestionIndex(clamp(request.getCurrentQuestionIndex(), 0, Math.max(questions.size() - 1, 0)));
        }

        if (request.getElapsedSeconds() != null) {
            session.setElapsedSeconds(Math.max(0, request.getElapsedSeconds()));
        }

        session.setUpdatedAt(LocalDateTime.now());
        return toSessionResponse(sessionRepository.save(session));
    }

    @Transactional
    public InterviewSessionResponse submitSession(Long sessionId, InterviewAnswerRequest request, String candidateEmail) {
        MockInterviewSession session = getOwnedSession(sessionId, candidateEmail);

        if (session.getStatus() != MockInterviewStatus.COMPLETED && request != null && request.getQuestionIndex() != null) {
            saveAnswer(sessionId, request, candidateEmail);
            session = getOwnedSession(sessionId, candidateEmail);
        }

        List<String> questions = readList(session.getQuestionsJson());
        Map<Integer, String> answers = readAnswers(session.getAnswersJson());
        long answeredCount = answers.values().stream().filter(answer -> !answer.isBlank()).count();

        if (answeredCount == 0) {
            throw new RuntimeException("Answer at least one question before submitting the interview.");
        }

        // Process final monitoring events
        if (request != null) {
            processMonitoringEvents(session, request.getMonitoringEvents());
            if (Boolean.TRUE.equals(request.getCameraUsed())) session.setCameraUsed(true);
            if (Boolean.TRUE.equals(request.getMicrophoneUsed())) session.setMicrophoneUsed(true);
        }

        applyResult(session, questions, answers);
        calculateMonitoringScores(session);
        session.setStatus(MockInterviewStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        if (request != null && request.getElapsedSeconds() != null) {
            session.setElapsedSeconds(Math.max(0, request.getElapsedSeconds()));
        }

        logger.info("Interview session {} completed — confidence={}, fluency={}, tabSwitches={}, faceWarnings={}, suspicious={}",
                session.getId(), session.getConfidenceScore(), session.getFluencyScore(),
                session.getTabSwitchCount(), session.getFaceWarningCount(), session.getSuspiciousActivityCount());

        activityLogService.logSuccess(candidateEmail, "CANDIDATE", "MOCK_INTERVIEW_SUBMIT", "Candidate completed mock interview for role: " + session.getJobRole(), null);

        return toSessionResponse(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public InterviewResultResponse getResult(Long sessionId, String candidateEmail) {
        MockInterviewSession session = getOwnedSession(sessionId, candidateEmail);

        if (session.getStatus() != MockInterviewStatus.COMPLETED) {
            throw new RuntimeException("Interview results are available after submission.");
        }

        return toResultResponse(session);
    }
    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getCandidateHistory(String candidateEmail) {
        User candidate = getCandidate(candidateEmail);
        return sessionRepository.findTop20ByCandidateOrderByUpdatedAtDesc(candidate).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getRecruiterReviewSessions(String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can review candidate interviews.");
        }

        List<Job> recruiterJobs = jobRepository.findAllByRecruiterOrderByCreatedAtDesc(recruiter);
        List<InterviewSessionResponse> responses = new ArrayList<>();
        for (Job job : recruiterJobs) {
            sessionRepository.findAllByJobOrderByCompletedAtDesc(job).stream()
                    .filter(session -> session.getStatus() == MockInterviewStatus.COMPLETED)
                    .map(this::toSessionResponse)
                    .forEach(responses::add);
        }
        return responses.stream().limit(100).toList();
    }

    @Transactional(readOnly = true)
    public InterviewResultResponse getRecruiterReviewResult(Long sessionId, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter not found."));
        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can review candidate interviews.");
        }

        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session not found."));

        if (session.getJob() == null || session.getJob().getRecruiter() == null || !session.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this interview result.");
        }
        if (session.getStatus() != MockInterviewStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview results are available after submission.");
        }
        return toResultResponse(session);
    }

    @Transactional(readOnly = true)
    public byte[] downloadInterviewReport(Long sessionId, String actorEmail) {
        MockInterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session not found."));

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        boolean candidateOwnsSession = session.getCandidate() != null && session.getCandidate().getEmail() != null
                && session.getCandidate().getEmail().equalsIgnoreCase(actorEmail);
        boolean recruiterOwnsJob = actor.getRole() == Role.RECRUITER && session.getJob() != null && session.getJob().getRecruiter() != null
                && session.getJob().getRecruiter().getId().equals(actor.getId());

        if (!candidateOwnsSession && !recruiterOwnsJob) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to download this interview report.");
        }
        if (session.getStatus() != MockInterviewStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview report is available after submission.");
        }

        try {
            InterviewResultResponse result = toResultResponse(session);
            return pdfReportService.generateInterviewReport(
                    session.getCandidate() == null ? null : session.getCandidate().getName(),
                    session.getCandidate() == null ? null : session.getCandidate().getEmail(),
                    session.getJobRole(),
                    session.getElapsedSeconds(),
                    result
            );
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate interview PDF report.");
        }
    }

    private User getCandidate(String candidateEmail) {
        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateEmail));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new RuntimeException("Only candidates can use the AI mock interview.");
        }

        return candidate;
    }

    private MockInterviewSession getOwnedSession(Long sessionId, String candidateEmail) {
        return sessionRepository.findByIdAndCandidateEmail(sessionId, candidateEmail)
                .orElseThrow(() -> new RuntimeException("Interview session not found."));
    }

    private Application resolveApplication(InterviewSessionStartRequest request, String candidateEmail) {
        if (request.getApplicationId() == null) {
            return null;
        }

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new RuntimeException("Application not found."));

        if (application.getCandidate() == null
                || application.getCandidate().getEmail() == null
                || !application.getCandidate().getEmail().equalsIgnoreCase(candidateEmail)) {
            throw new RuntimeException("You can only start interviews for your own applications.");
        }

        return application;
    }

    private Job resolveJob(InterviewSessionStartRequest request, Application application) {
        if (application != null && application.getJob() != null) {
            return application.getJob();
        }

        if (request.getJobId() == null) {
            return null;
        }

        return jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found."));
    }

    private String resolveJobRole(InterviewSessionStartRequest request, Job job) {
        if (request.getJobRole() != null && !request.getJobRole().trim().isBlank()) {
            return request.getJobRole().trim();
        }

        if (job != null && job.getTitle() != null && !job.getTitle().trim().isBlank()) {
            return job.getTitle().trim();
        }

        return "Software Engineer";
    }

    private List<String> normalizeSkills(List<String> requestSkills, Job job) {
        Set<String> skills = new LinkedHashSet<>();

        if (requestSkills != null) {
            for (String skill : requestSkills) {
                addSkill(skills, skill);
            }
        }

        if (skills.isEmpty() && job != null && job.getSkills() != null) {
            Arrays.stream(job.getSkills().split(",")).forEach(skill -> addSkill(skills, skill));
        }

        if (skills.isEmpty()) {
            skills.add("Problem Solving");
            skills.add("Communication");
        }

        return new ArrayList<>(skills);
    }

    private void addSkill(Set<String> skills, String skill) {
        if (skill != null && !skill.trim().isBlank()) {
            skills.add(skill.trim());
        }
    }

    private String normalizeOption(String value, String fallback) {
        if (value == null || value.trim().isBlank()) {
            return fallback;
        }

        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    private int normalizeDuration(Integer duration) {
        if (duration == null) {
            return 20;
        }

        return clamp(duration, 10, 60);
    }

    private QuestionResponse buildQuestions(Job job, String jobRole, List<String> skills, String difficulty, String interviewType, int duration) {
        InterviewQuestionGenerationRequest request = new InterviewQuestionGenerationRequest();
        request.setJobRole(jobRole);
        request.setDifficulty(difficulty);
        request.setInterviewType(interviewType);
        request.setSkills(skills);
        request.setJobDescription(buildQuestionContext(job, jobRole, skills, difficulty, interviewType));
        QuestionResponse questionResponse = aiService.generateInterviewQuestions(request);
        List<String> generated = questionResponse.getQuestions() == null ? List.of() : questionResponse.getQuestions();
        List<String> supplemental = getSupplementalQuestions(jobRole, skills, interviewType, difficulty);
        int targetCount = duration <= 15 ? 5 : duration <= 30 ? 6 : 8;
        List<String> questions = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String question : generated) {
            addQuestion(questions, seen, question);
        }

        for (String question : supplemental) {
            addQuestion(questions, seen, question);
        }

        while (questions.size() < targetCount) {
            addQuestion(
                    questions,
                    seen,
                    "Share one example that shows your readiness for a " + jobRole + " role."
            );
        }

        QuestionResponse normalized = new QuestionResponse();
        normalized.setJobRole(jobRole);
        normalized.setDifficulty(difficulty);
        normalized.setInterviewType(interviewType);
        normalized.setSkills(skills);
        normalized.setQuestions(questions.subList(0, Math.min(targetCount, questions.size())));
        normalized.setFallbackUsed(Boolean.TRUE.equals(questionResponse.getFallbackUsed()));
        normalized.setMessage(Boolean.TRUE.equals(questionResponse.getFallbackUsed()) ? BUSY_FALLBACK_MESSAGE : questionResponse.getMessage());
        return normalized;
    }

    private String buildQuestionContext(Job job, String jobRole, List<String> skills, String difficulty, String interviewType) {
        StringBuilder context = new StringBuilder();
        context.append(jobRole).append(" interview. ");
        context.append("Difficulty: ").append(difficulty).append(". ");
        context.append("Interview type: ").append(interviewType).append(". ");
        context.append("Skills: ").append(String.join(", ", skills)).append(". ");

        if (job != null && job.getDescription() != null) {
            context.append(job.getDescription());
        }

        return context.toString();
    }

    private List<String> getSupplementalQuestions(String jobRole, List<String> skills, String interviewType, String difficulty) {
        String primarySkill = skills.isEmpty() ? "your core skill set" : skills.get(0);
        List<String> questions = new ArrayList<>();
        String normalizedType = interviewType.toLowerCase(Locale.ROOT);

        if (!normalizedType.equals("hr")) {
            questions.add("Walk me through a recent project where you used " + primarySkill + " to solve a real problem.");
            questions.add("How would you debug a production issue in a " + jobRole + " workflow?");
            questions.add("What tradeoffs would you consider before choosing a technical approach for this role?");
        }

        if (!normalizedType.equals("technical")) {
            questions.add("Tell me about a time you received difficult feedback and how you responded.");
            questions.add("How do you communicate progress when a task is blocked?");
            questions.add("Why are you interested in this " + jobRole + " opportunity?");
        }

        if (difficulty.equalsIgnoreCase("Advanced")) {
            questions.add("Describe a high-impact decision you made with incomplete information.");
        }

        return questions;
    }

    private void addQuestion(List<String> questions, Set<String> seen, String question) {
        if (question == null || question.trim().isBlank()) {
            return;
        }

        String cleaned = question.trim();
        String key = cleaned.toLowerCase(Locale.ROOT);

        if (seen.add(key)) {
            questions.add(cleaned);
        }
    }

    private void applyResult(MockInterviewSession session, List<String> questions, Map<Integer, String> answers) {
        List<String> expectedSkills = readSkills(session.getSkills());
        InterviewEvaluationRequest request = new InterviewEvaluationRequest();
        request.setJobRole(session.getJobRole());
        request.setDifficulty(session.getDifficulty());
        request.setSkills(expectedSkills);
        request.setTranscript(buildTranscript(questions, answers));

        InterviewEvaluationResponse evaluation = aiService.evaluateInterview(request);
        List<InterviewQuestionBreakdownItem> breakdown = evaluation.getQuestionBreakdown() == null
                ? List.of()
                : evaluation.getQuestionBreakdown();

        session.setOverallScore(evaluation.getOverallScore());
        session.setCommunicationScore(evaluation.getCommunicationScore());
        session.setTechnicalScore(evaluation.getTechnicalScore());
        session.setStrengthsJson(writeList(evaluation.getStrengths()));
        session.setWeaknessesJson(writeList(evaluation.getWeaknesses()));
        session.setImprovementSuggestionsJson(writeList(evaluation.getImprovementSuggestions()));
        session.setCommunicationFeedback(buildCommunicationNarrative(breakdown, evaluation.getCommunicationScore(), evaluation.getFallbackUsed()));
        session.setTechnicalFeedback(buildTechnicalNarrative(breakdown, evaluation.getTechnicalScore(), expectedSkills, evaluation.getFallbackUsed()));
        session.setResultSummary(buildSummary(evaluation));
        session.setFinalFeedback(evaluation.getFinalFeedback());
        session.setQuestionBreakdownJson(writeQuestionBreakdown(breakdown));
        session.setFallbackUsed(Boolean.TRUE.equals(evaluation.getFallbackUsed()));
    }

    private String sanitizeAnswer(String answer) {
        return answer == null ? "" : answer.trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private InterviewSessionResponse toSessionResponse(MockInterviewSession session) {
        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setId(session.getId());
        response.setJobId(session.getJob() == null ? null : session.getJob().getId());
        response.setApplicationId(session.getApplication() == null ? null : session.getApplication().getId());
        response.setJobRole(session.getJobRole());
        response.setDifficulty(session.getDifficulty());
        response.setSkills(readSkills(session.getSkills()));
        response.setInterviewType(session.getInterviewType());
        response.setMonitoringMode(session.getMonitoringMode());
        response.setEstimatedDurationMinutes(session.getEstimatedDurationMinutes());
        response.setStatus(session.getStatus() == null ? null : session.getStatus().name());
        response.setQuestions(readList(session.getQuestionsJson()));
        response.setAnswers(readAnswers(session.getAnswersJson()));
        response.setCurrentQuestionIndex(session.getCurrentQuestionIndex());
        response.setElapsedSeconds(session.getElapsedSeconds());
        response.setStartedAt(session.getStartedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        response.setCompletedAt(session.getCompletedAt());
        response.setFallbackUsed(session.getFallbackUsed());
        response.setMessage(Boolean.TRUE.equals(session.getFallbackUsed()) ? BUSY_FALLBACK_MESSAGE : "");
        response.setTabSwitchCount(session.getTabSwitchCount());
        response.setFaceWarningCount(session.getFaceWarningCount());
        response.setSuspiciousActivityCount(session.getSuspiciousActivityCount());
        response.setCameraUsed(session.getCameraUsed());
        response.setMicrophoneUsed(session.getMicrophoneUsed());

        if (session.getStatus() == MockInterviewStatus.COMPLETED) {
            response.setResult(toResultResponse(session));
        }

        return response;
    }

    private InterviewResultResponse toResultResponse(MockInterviewSession session) {
        List<String> questions = readList(session.getQuestionsJson());
        Map<Integer, String> answers = readAnswers(session.getAnswersJson());
        int answeredQuestions = (int) answers.values().stream().filter(answer -> answer != null && !answer.isBlank()).count();
        InterviewResultResponse response = new InterviewResultResponse();
        response.setSessionId(session.getId());
        response.setMonitoringMode(session.getMonitoringMode());
        response.setOverallScore(session.getOverallScore());
        response.setCommunicationScore(session.getCommunicationScore());
        response.setTechnicalScore(session.getTechnicalScore());
        response.setStrengths(readList(session.getStrengthsJson()));
        response.setWeaknesses(readList(session.getWeaknessesJson()));
        response.setImprovementSuggestions(readList(session.getImprovementSuggestionsJson()));
        response.setCommunicationEvaluation(session.getCommunicationFeedback());
        response.setTechnicalRelevance(session.getTechnicalFeedback());
        response.setSummary(session.getResultSummary());
        response.setFinalFeedback(session.getFinalFeedback());
        response.setFallbackUsed(session.getFallbackUsed());
        response.setMessage(Boolean.TRUE.equals(session.getFallbackUsed()) ? BUSY_FALLBACK_MESSAGE : "");
        response.setQuestionBreakdown(readQuestionBreakdown(session.getQuestionBreakdownJson()));
        response.setTotalQuestions(questions.size());
        response.setAnsweredQuestions(answeredQuestions);
        response.setCompletedAt(session.getCompletedAt());
        response.setConfidenceScore(session.getConfidenceScore());
        response.setFluencyScore(session.getFluencyScore());
        response.setTabSwitchCount(session.getTabSwitchCount());
        response.setFaceWarningCount(session.getFaceWarningCount());
        response.setSuspiciousActivityCount(session.getSuspiciousActivityCount());
        response.setIntegrityStatus(deriveIntegrityStatus(session));
        response.setMonitoringEvents(readMonitoringEvents(session.getMonitoringEventsJson()));
        return response;
    }

    private List<String> readSkills(String skillsCsv) {
        if (skillsCsv == null || skillsCsv.trim().isBlank()) {
            return List.of();
        }

        return Arrays.stream(skillsCsv.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .toList();
    }

    private List<String> readList(String json) {
        if (json == null || json.trim().isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception error) {
            return List.of();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception error) {
            return "[]";
        }
    }

    private Map<Integer, String> readAnswers(String json) {
        if (json == null || json.trim().isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<Integer, String>>() {});
        } catch (Exception error) {
            return new LinkedHashMap<>();
        }
    }

    private String writeAnswers(Map<Integer, String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Map.of() : values);
        } catch (Exception error) {
            return "{}";
        }
    }

    private List<InterviewTranscriptItem> buildTranscript(List<String> questions, Map<Integer, String> answers) {
        List<InterviewTranscriptItem> transcript = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            InterviewTranscriptItem item = new InterviewTranscriptItem();
            item.setQuestionNumber(index + 1);
            item.setQuestion(questions.get(index));
            item.setAnswer(sanitizeAnswer(answers.get(index)));
            transcript.add(item);
        }
        return transcript;
    }

    private List<InterviewQuestionBreakdownItem> readQuestionBreakdown(String json) {
        if (json == null || json.trim().isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<InterviewQuestionBreakdownItem>>() {});
        } catch (Exception error) {
            return List.of();
        }
    }

    private String writeQuestionBreakdown(List<InterviewQuestionBreakdownItem> breakdown) {
        try {
            return objectMapper.writeValueAsString(breakdown == null ? List.of() : breakdown);
        } catch (Exception error) {
            return "[]";
        }
    }

    private String buildCommunicationNarrative(
        List<InterviewQuestionBreakdownItem> breakdown,
        Integer communicationScore,
        Boolean fallbackUsed
    ) {
        int clarityAverage = averageMetric(breakdown, InterviewQuestionBreakdownItem::getClarity);
        int confidenceAverage = averageMetric(breakdown, InterviewQuestionBreakdownItem::getConfidence);
        String source = Boolean.TRUE.equals(fallbackUsed) ? "Local SmartATS analysis" : "AI analysis";

        if (communicationScore != null && communicationScore >= 80) {
            return source + " indicates strong communication with clear structure and confident delivery across most answers.";
        }
        if (communicationScore != null && communicationScore >= 65) {
            return source + " indicates understandable communication, with the main opportunity being sharper structure and more concise delivery.";
        }
        if (clarityAverage >= 60 || confidenceAverage >= 60) {
            return source + " indicates communication was understandable in parts, but several answers need clearer organization and stronger ownership language.";
        }
        return source + " indicates communication needs more structure, fuller explanation, and more direct ownership of the work described.";
    }

    private String buildTechnicalNarrative(
            List<InterviewQuestionBreakdownItem> breakdown,
        Integer technicalScore,
        List<String> expectedSkills,
        Boolean fallbackUsed
    ) {
        int roleAverage = averageMetric(breakdown, InterviewQuestionBreakdownItem::getRoleRelevance);
        String skillSuffix = expectedSkills.isEmpty()
                ? ""
                : " The clearest role signals came from skills such as " + String.join(", ", expectedSkills.stream().limit(3).toList()) + ".";
        String source = Boolean.TRUE.equals(fallbackUsed) ? "Local SmartATS analysis" : "AI analysis";

        if (technicalScore != null && technicalScore >= 80) {
            return source + " indicates strong technical alignment, with relevant concepts, decisions, and role-specific examples." + skillSuffix;
        }
        if (technicalScore != null && technicalScore >= 65) {
            return source + " indicates a solid technical baseline, but several answers would be stronger with deeper tool choices, tradeoffs, or measurable outcomes." + skillSuffix;
        }
        if (roleAverage >= 60) {
            return source + " indicates partial technical alignment, but the interview needs more precise technical depth and clearer explanation of implementation decisions." + skillSuffix;
        }
        return source + " indicates technical relevance was inconsistent and should be reinforced with clearer skills, architecture choices, and problem-solving detail." + skillSuffix;
    }

    private String buildSummary(InterviewEvaluationResponse evaluation) {
        if (evaluation.getFinalFeedback() != null && !evaluation.getFinalFeedback().trim().isBlank()) {
            return evaluation.getFinalFeedback().trim();
        }

        int overallScore = evaluation.getOverallScore() == null ? 0 : evaluation.getOverallScore();
        if (overallScore >= 85) {
            return "Strong interview performance with clear readiness signals.";
        }
        if (overallScore >= 70) {
            return "Good foundation with room to make answers more specific and outcome-driven.";
        }
        return "Keep practicing with fuller examples and stronger role-specific detail.";
    }

    private int averageMetric(List<InterviewQuestionBreakdownItem> breakdown, java.util.function.Function<InterviewQuestionBreakdownItem, Integer> extractor) {
        if (breakdown == null || breakdown.isEmpty()) {
            return 0;
        }
        return clamp((int) Math.round(breakdown.stream()
                .map(extractor)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0)), 0, 100);
    }

    // ── Monitoring helpers ────────────────────────────────────────

    private void processMonitoringEvents(MockInterviewSession session, List<MonitoringEventDto> events) {
        if (events == null || events.isEmpty()) return;

        List<MonitoringEventDto> existing = readMonitoringEvents(session.getMonitoringEventsJson());
        List<MonitoringEventDto> merged = new ArrayList<>(existing);
        int tabSwitches = session.getTabSwitchCount() == null ? 0 : session.getTabSwitchCount();
        int faceWarnings = session.getFaceWarningCount() == null ? 0 : session.getFaceWarningCount();
        int suspicious = session.getSuspiciousActivityCount() == null ? 0 : session.getSuspiciousActivityCount();

        for (MonitoringEventDto event : events) {
            if (event == null || event.getType() == null) continue;
            merged.add(event);
            switch (event.getType().toUpperCase(Locale.ROOT)) {
                case "TAB_SWITCH" -> { tabSwitches++; suspicious++; }
                case "FACE_WARNING", "FACE_NOT_DETECTED", "MULTIPLE_FACE", "LOOKING_AWAY" -> { faceWarnings++; suspicious++; }
                case "COPY_PASTE", "CAMERA_BLOCKED", "MIC_BLOCKED", "DEVTOOLS_OPEN", "FULLSCREEN_EXIT" -> suspicious++;
                default -> {}
            }
        }

        session.setMonitoringEventsJson(writeMonitoringEvents(merged));
        session.setTabSwitchCount(tabSwitches);
        session.setFaceWarningCount(faceWarnings);
        session.setSuspiciousActivityCount(suspicious);
    }

    private void calculateMonitoringScores(MockInterviewSession session) {
        Map<Integer, String> transcripts = readAnswers(session.getTranscriptJson());
        Map<Integer, String> answers = readAnswers(session.getAnswersJson());

        if (transcripts.isEmpty() && answers.isEmpty()) {
            session.setConfidenceScore(null);
            session.setFluencyScore(null);
            return;
        }

        // Merge transcripts + typed answers for analysis
        Map<Integer, String> allText = new LinkedHashMap<>(answers);
        transcripts.forEach((k, v) -> { if (v != null && !v.isBlank()) allText.put(k, v); });

        int totalWords = 0;
        int totalFillers = 0;
        int answeredCount = 0;

        Set<String> fillerWords = Set.of("um", "uh", "like", "you know", "basically", "actually", "so yeah", "i mean", "kind of", "sort of");

        for (String text : allText.values()) {
            if (text == null || text.isBlank()) continue;
            answeredCount++;
            String[] words = text.toLowerCase(Locale.ROOT).split("\\s+");
            totalWords += words.length;
            for (String word : words) {
                if (fillerWords.contains(word)) totalFillers++;
            }
        }

        if (answeredCount == 0) {
            session.setConfidenceScore(50);
            session.setFluencyScore(50);
            return;
        }

        // Fluency: based on word count per answer and filler ratio
        double avgWords = (double) totalWords / answeredCount;
        double fillerRatio = totalWords > 0 ? (double) totalFillers / totalWords : 0;
        int fluency = clamp((int) Math.round(Math.min(avgWords / 80.0, 1.0) * 70 + (1.0 - Math.min(fillerRatio * 10, 1.0)) * 30), 0, 100);

        // Confidence: based on presence of confident language patterns
        long confidentPhrases = allText.values().stream()
                .filter(t -> t != null && !t.isBlank())
                .filter(t -> {
                    String lower = t.toLowerCase(Locale.ROOT);
                    return lower.contains("i built") || lower.contains("i designed") || lower.contains("i led")
                            || lower.contains("i implemented") || lower.contains("i improved") || lower.contains("i resolved");
                }).count();
        int confidence = clamp((int) Math.round(50 + (confidentPhrases * 10.0) - (fillerRatio * 50)), 30, 100);

        session.setConfidenceScore(confidence);
        session.setFluencyScore(fluency);
    }

    private String deriveIntegrityStatus(MockInterviewSession session) {
        int total = (session.getSuspiciousActivityCount() == null ? 0 : session.getSuspiciousActivityCount());
        if (total == 0) return "Clean";
        if (total <= 3) return "Flagged";
        return "Suspicious";
    }

    private List<MonitoringEventDto> readMonitoringEvents(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<MonitoringEventDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeMonitoringEvents(List<MonitoringEventDto> events) {
        try {
            return objectMapper.writeValueAsString(events == null ? List.of() : events);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Accepts a batch of monitoring events from the frontend and persists them.
     */
    @Transactional
    public InterviewSessionResponse reportMonitoringEvents(Long sessionId, List<MonitoringEventDto> events, String candidateEmail) {
        MockInterviewSession session = getOwnedSession(sessionId, candidateEmail);
        if (session.getStatus() == MockInterviewStatus.COMPLETED) {
            return toSessionResponse(session);
        }
        processMonitoringEvents(session, events);
        session.setUpdatedAt(LocalDateTime.now());
        return toSessionResponse(sessionRepository.save(session));
    }
}



