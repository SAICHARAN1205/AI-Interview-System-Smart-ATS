package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationResponse;
import com.aihiringplatform.backend.dto.InterviewEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewEvaluationResponse;
import com.aihiringplatform.backend.dto.InterviewQuestionBreakdownItem;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.InterviewQuestionItem;
import com.aihiringplatform.backend.dto.InterviewTranscriptItem;
import com.aihiringplatform.backend.dto.JobMatchRequest;
import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.dto.QuestionResponse;
import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import com.aihiringplatform.backend.service.ai.AIGatewayResult;
import com.aihiringplatform.backend.service.ai.AIGatewayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private static final String BUSY_FALLBACK_MESSAGE = "AI service temporarily unavailable. Using fallback evaluation.";

    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z][A-Za-z+#.]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "before", "because", "between", "candidate", "company", "could",
            "their", "there", "these", "those", "would", "should", "where", "which", "while",
            "experience", "worked", "using", "skills", "skill", "technical", "interview",
            "question", "answer", "role", "with", "from", "into", "your", "have", "this",
            "that", "will", "been", "were", "they", "them", "also", "only", "more", "than"
    );
    private static final List<String> TECHNICAL_SIGNAL_TERMS = List.of(
            "api", "service", "database", "sql", "nosql", "backend", "frontend", "architecture",
            "design", "implementation", "optimize", "performance", "cache", "deploy", "testing",
            "debug", "bug", "incident", "integration", "algorithm", "security", "spring", "java",
            "python", "react", "node", "docker", "kubernetes", "cloud"
    );
    private static final List<String> PROBLEM_SOLVING_TERMS = List.of(
            "because", "debug", "diagnose", "root cause", "tradeoff", "decision", "resolved",
            "improved", "optimized", "measured", "result", "impact", "issue", "problem", "fix"
    );
    private static final List<String> STRUCTURE_TERMS = List.of(
            "first", "then", "next", "finally", "because", "so that", "result", "outcome"
    );
    private static final List<String> CONFIDENT_TERMS = List.of(
            "i built", "i designed", "i led", "i implemented", "i improved", "i resolved", "i owned"
    );
    private static final List<String> TENTATIVE_TERMS = List.of(
            "maybe", "probably", "i think", "kind of", "sort of", "not sure", "perhaps"
    );
    private static final List<String> OUTCOME_TERMS = List.of(
            "result", "outcome", "impact", "reduced", "increased", "improved", "delivered"
    );

    private final AIGatewayService gatewayService;
    private final GeminiProperties properties;
    private final AiInputSanitizer sanitizer;
    private final PromptBuilderService promptBuilderService;
    private final AiResponseCacheService responseCacheService;
    private final ObjectMapper objectMapper;
    private final MatchingService matchingService;

    public AIService(
            AIGatewayService gatewayService,
            GeminiProperties properties,
            AiInputSanitizer sanitizer,
            PromptBuilderService promptBuilderService,
            AiResponseCacheService responseCacheService,
            ObjectMapper objectMapper,
            MatchingService matchingService
    ) {
        this.gatewayService = gatewayService;
        this.properties = properties;
        this.sanitizer = sanitizer;
        this.promptBuilderService = promptBuilderService;
        this.responseCacheService = responseCacheService;
        this.objectMapper = objectMapper;
        this.matchingService = matchingService;
    }

    public List<String> generateQuestions(String jobDescription) {
        InterviewQuestionGenerationRequest request = new InterviewQuestionGenerationRequest();
        request.setJobRole("Software Engineer");
        request.setDifficulty("Intermediate");
        request.setInterviewType("Mixed");
        request.setSkills(extractKeywords(jobDescription, 5));
        request.setJobDescription(jobDescription);
        return generateQuestions(request);
    }

    public List<String> generateQuestions(InterviewQuestionGenerationRequest request) {
        return generateInterviewQuestions(request).getQuestions();
    }

    public QuestionResponse generateInterviewQuestions(InterviewQuestionGenerationRequest request) {
        String jobRole = sanitizer.sanitizeOption(request.getJobRole(), "Software Engineer", 100);
        String difficulty = sanitizer.sanitizeOption(request.getDifficulty(), "Intermediate", 30);
        String interviewType = sanitizer.sanitizeOption(request.getInterviewType(), "Mixed", 30);
        List<String> skills = sanitizer.sanitizeSkills(request.getSkills(), properties.getMaxSkills(), 60);
        String jobDescription = sanitizer.sanitizeText(request.getJobDescription(), properties.getMaxJobDescriptionChars());

        return responseCacheService.getOrCompute(
                "interview-question-generation",
                cacheKey(orderedMap(
                        "jobRole", jobRole,
                        "difficulty", difficulty,
                        "interviewType", interviewType,
                        "skills", skills,
                        "jobDescription", jobDescription
                )),
                QuestionResponse.class,
                () -> {
                    AIGatewayResult<QuestionGenerationPayload> result = gatewayService.generateStructuredResponse(
                            promptBuilderService.buildQuestionGenerationPrompt(
                                    jobRole,
                                    difficulty,
                                    interviewType,
                                    skills,
                                    jobDescription,
                                    sanitizer
                            ),
                            QuestionGenerationPayload.class,
                            () -> null
                    );
                    return normalizeQuestionResponse(
                            result.payload(),
                            jobRole,
                            difficulty,
                            interviewType,
                            skills,
                            result.localFallbackUsed()
                    );
                }
        );
    }

    public InterviewAnswerEvaluationResponse evaluateAnswer(InterviewAnswerEvaluationRequest request) {
        String question = sanitizer.sanitizeText(request.getQuestion(), 1200);
        String answer = sanitizer.sanitizeText(request.getCandidateAnswer(), properties.getMaxInputChars());
        String jobRole = sanitizer.sanitizeOption(request.getJobRole(), "Software Engineer", 100);
        List<String> expectedSkills = sanitizer.sanitizeSkills(request.getExpectedSkills(), properties.getMaxSkills(), 60);

        return responseCacheService.getOrCompute(
                "interview-answer-evaluation",
                cacheKey(orderedMap(
                        "question", question,
                        "answer", answer,
                        "jobRole", jobRole,
                        "expectedSkills", expectedSkills
                )),
                InterviewAnswerEvaluationResponse.class,
                () -> {
                    AIGatewayResult<InterviewAnswerEvaluationResponse> result = gatewayService.generateStructuredResponse(
                            promptBuilderService.buildAnswerEvaluationPrompt(
                                    question,
                                    answer,
                                    jobRole,
                                    expectedSkills,
                                    sanitizer
                            ),
                            InterviewAnswerEvaluationResponse.class,
                            () -> buildFallbackEvaluation(question, answer, jobRole, expectedSkills)
                    );

                    return normalizeEvaluation(
                            result.payload(),
                            question,
                            answer,
                            jobRole,
                            expectedSkills,
                            result.localFallbackUsed()
                    );
                }
        );
    }

    public InterviewEvaluationResponse evaluateInterview(InterviewEvaluationRequest request) {
        String jobRole = sanitizer.sanitizeOption(request.getJobRole(), "Software Engineer", 100);
        String difficulty = sanitizer.sanitizeOption(request.getDifficulty(), "Intermediate", 30);
        List<String> skills = sanitizer.sanitizeSkills(request.getSkills(), properties.getMaxSkills(), 60);
        List<InterviewTranscriptItem> transcript = sanitizeTranscript(request.getTranscript());

        return responseCacheService.getOrCompute(
                "interview-evaluation",
                cacheKey(orderedMap(
                        "jobRole", jobRole,
                        "difficulty", difficulty,
                        "skills", skills,
                        "transcript", transcript
                )),
                InterviewEvaluationResponse.class,
                () -> {
                    AIGatewayResult<InterviewEvaluationResponse> result = gatewayService.generateStructuredResponse(
                            promptBuilderService.buildInterviewEvaluationPrompt(
                                    jobRole,
                                    difficulty,
                                    skills,
                                    buildTranscriptLines(transcript),
                                    sanitizer
                            ),
                            InterviewEvaluationResponse.class,
                            () -> buildFallbackInterviewEvaluation(transcript, jobRole, difficulty, skills)
                    );
                    return normalizeInterviewEvaluation(
                            result.payload(),
                            transcript,
                            jobRole,
                            difficulty,
                            skills,
                            result.localFallbackUsed()
                    );
                }
        );
    }

    public ResumeAnalysisResponse analyzeResume(ResumeAnalysisRequest request) {
        String resumeText = sanitizer.sanitizeText(request.getResumeText(), properties.getMaxResumeChars());
        String targetRole = sanitizer.sanitizeOption(request.getTargetRole(), "Software Engineer", 100);
        String jobDescription = sanitizer.sanitizeText(request.getJobDescription(), properties.getMaxJobDescriptionChars());

        return responseCacheService.getOrCompute(
                "resume-analysis",
                cacheKey(orderedMap(
                        "resumeText", resumeText,
                        "targetRole", targetRole,
                        "jobDescription", jobDescription
                )),
                ResumeAnalysisResponse.class,
                () -> {
                    AIGatewayResult<ResumeAnalysisResponse> result = gatewayService.generateStructuredResponse(
                            promptBuilderService.buildResumeAnalysisPrompt(
                                    resumeText,
                                    targetRole,
                                    jobDescription,
                                    sanitizer
                            ),
                            ResumeAnalysisResponse.class,
                            () -> buildFallbackResumeAnalysis(resumeText, targetRole, jobDescription)
                    );
                    return normalizeResumeAnalysis(
                            result.payload(),
                            resumeText,
                            targetRole,
                            jobDescription,
                            result.localFallbackUsed()
                    );
                }
        );
    }

    public MatchResponse analyzeJobMatch(JobMatchRequest request) {
        String resumeText = sanitizer.sanitizeText(request.getResumeText(), properties.getMaxResumeChars());
        String candidateProfile = sanitizer.sanitizeText(request.getCandidateProfile(), properties.getMaxJobDescriptionChars());
        String targetRole = sanitizer.sanitizeOption(request.getTargetRole(), "Software Engineer", 100);
        String jobDescription = sanitizer.sanitizeText(request.getJobDescription(), properties.getMaxJobDescriptionChars());

        return responseCacheService.getOrCompute(
                "job-match-analysis",
                cacheKey(orderedMap(
                        "resumeText", resumeText,
                        "candidateProfile", candidateProfile,
                        "targetRole", targetRole,
                        "jobDescription", jobDescription,
                        "jobId", request.getJobId()
                )),
                MatchResponse.class,
                () -> {
                    AIGatewayResult<MatchResponse> result = gatewayService.generateStructuredResponse(
                            promptBuilderService.buildJobMatchPrompt(
                                    resumeText,
                                    candidateProfile,
                                    targetRole,
                                    jobDescription,
                                    sanitizer
                            ),
                            MatchResponse.class,
                            () -> buildFallbackJobMatch(resumeText, candidateProfile, jobDescription, targetRole)
                    );
                    MatchResponse response = normalizeMatchResponse(
                            result.payload(),
                            resumeText,
                            jobDescription,
                            targetRole,
                            result.localFallbackUsed()
                    );
                    response.setJobId(request.getJobId());
                    response.setTargetRole(targetRole);
                    return response;
                }
        );
    }

    private QuestionResponse normalizeQuestionResponse(
            QuestionGenerationPayload payload,
            String jobRole,
            String difficulty,
            String interviewType,
            List<String> skills,
            boolean forceFallback
    ) {
        List<InterviewQuestionItem> structuredQuestions = new ArrayList<>();
        Set<String> deduplicated = new LinkedHashSet<>();
        boolean fallbackUsed = forceFallback;

        if (payload != null && payload.getQuestions() != null) {
            for (QuestionGenerationPayload.QuestionEntry entry : payload.getQuestions()) {
                String question = sanitizer.sanitizeText(entry == null ? "" : entry.getQuestion(), 240);
                if (question.isBlank()) {
                    continue;
                }

                String dedupeKey = question.toLowerCase(Locale.ROOT);
                if (!deduplicated.add(dedupeKey)) {
                    fallbackUsed = true;
                    continue;
                }

                structuredQuestions.add(buildQuestionItem(
                        structuredQuestions.size() + 1,
                        sanitizeCategory(entry == null ? null : entry.getCategory()),
                        question
                ));
            }
        }

        if (structuredQuestions.size() < 5) {
            fallbackUsed = true;
            for (InterviewQuestionItem item : buildFallbackQuestions(jobRole, difficulty, interviewType, skills)) {
                String dedupeKey = item.getQuestion().toLowerCase(Locale.ROOT);
                if (deduplicated.add(dedupeKey)) {
                    structuredQuestions.add(buildQuestionItem(
                            structuredQuestions.size() + 1,
                            item.getCategory(),
                            item.getQuestion()
                    ));
                }
                if (structuredQuestions.size() >= 6) {
                    break;
                }
            }
        }

        while (structuredQuestions.size() < 6) {
            String question = "Share one example that shows your readiness for a " + jobRole + " role.";
            String dedupeKey = question.toLowerCase(Locale.ROOT);
            if (deduplicated.add(dedupeKey)) {
                structuredQuestions.add(buildQuestionItem(
                        structuredQuestions.size() + 1,
                        "Role-specific",
                        question
                ));
            } else {
                structuredQuestions.add(buildQuestionItem(
                        structuredQuestions.size() + 1,
                        "Role-specific",
                        question + " Focus on a different project or outcome."
                ));
            }
        }

        QuestionResponse response = new QuestionResponse();
        response.setJobRole(jobRole);
        response.setDifficulty(difficulty);
        response.setInterviewType(interviewType);
        response.setSkills(skills);
        response.setStructuredQuestions(structuredQuestions);
        response.setQuestions(structuredQuestions.stream().map(InterviewQuestionItem::getQuestion).toList());
        response.setFallbackUsed(fallbackUsed);
        response.setMessage(fallbackUsed
                ? BUSY_FALLBACK_MESSAGE
                : "Interview questions generated successfully.");
        return response;
    }

    private InterviewAnswerEvaluationResponse normalizeEvaluation(
            InterviewAnswerEvaluationResponse response,
            String question,
            String answer,
            String jobRole,
            List<String> expectedSkills,
            boolean fallbackUsed
    ) {
        InterviewAnswerEvaluationResponse fallback = buildFallbackEvaluation(question, answer, jobRole, expectedSkills);
        if (response == null) {
            fallback.setFallbackUsed(fallbackUsed);
            fallback.setMessage(fallbackUsed ? BUSY_FALLBACK_MESSAGE : "Answer evaluation completed successfully.");
            return fallback;
        }

        response.setScore(blendScore(response.getScore(), fallback.getScore(), fallback.getScore()));
        response.setCommunicationScore(blendScore(response.getCommunicationScore(), fallback.getCommunicationScore(), fallback.getCommunicationScore()));
        response.setTechnicalScore(blendScore(response.getTechnicalScore(), fallback.getTechnicalScore(), fallback.getTechnicalScore()));
        response.setOverallScore(response.getScore());
        response.setStrengths(cleanList(response.getStrengths(), fallback.getStrengths().get(0)));
        response.setWeaknesses(cleanList(response.getWeaknesses(), fallback.getWeaknesses().get(0)));
        response.setMissingConcepts(cleanListOrFallback(response.getMissingConcepts(), fallback.getMissingConcepts()));
        response.setImprovementSuggestions(cleanList(response.getImprovementSuggestions(), fallback.getImprovementSuggestions().get(0)));
        response.setCommunicationEvaluation(sanitizer.sanitizeOption(
                response.getCommunicationEvaluation(),
                fallback.getCommunicationEvaluation(),
                400
        ));
        response.setTechnicalRelevance(sanitizer.sanitizeOption(
                response.getTechnicalRelevance(),
                fallback.getTechnicalRelevance(),
                400
        ));
        response.setSummary(sanitizer.sanitizeOption(
                response.getSummary(),
                fallback.getSummary(),
                400
        ));
        response.setFinalFeedback(sanitizer.sanitizeOption(
                response.getFinalFeedback(),
                fallback.getFinalFeedback(),
                400
        ));
        response.setFallbackUsed(fallbackUsed);
        response.setMessage(fallbackUsed
                ? BUSY_FALLBACK_MESSAGE
                : "Answer evaluation completed successfully.");
        return response;
    }

    private InterviewEvaluationResponse normalizeInterviewEvaluation(
            InterviewEvaluationResponse response,
            List<InterviewTranscriptItem> transcript,
            String jobRole,
            String difficulty,
            List<String> skills,
            boolean fallbackUsed
    ) {
        InterviewEvaluationResponse fallback = buildFallbackInterviewEvaluation(transcript, jobRole, difficulty, skills);
        if (response == null) {
            fallback.setFallbackUsed(fallbackUsed);
            fallback.setMessage(fallbackUsed ? BUSY_FALLBACK_MESSAGE : "Interview evaluation completed successfully.");
            return fallback;
        }

        List<InterviewQuestionBreakdownItem> normalizedBreakdown = normalizeQuestionBreakdown(
                response.getQuestionBreakdown(),
                transcript,
                jobRole,
                skills
        );

        int averageScore = averageQuestionMetric(normalizedBreakdown, InterviewQuestionBreakdownItem::getScore);
        int averageCommunication = averageQuestionMetric(normalizedBreakdown, InterviewQuestionBreakdownItem::getCommunication);
        int averageTechnical = averageQuestionMetric(normalizedBreakdown, InterviewQuestionBreakdownItem::getTechnicalAccuracy);

        InterviewEvaluationResponse normalized = new InterviewEvaluationResponse();
        normalized.setOverallScore(blendScore(response.getOverallScore(), averageScore, fallback.getOverallScore()));
        normalized.setCommunicationScore(blendScore(response.getCommunicationScore(), averageCommunication, fallback.getCommunicationScore()));
        normalized.setTechnicalScore(blendScore(response.getTechnicalScore(), averageTechnical, fallback.getTechnicalScore()));
        normalized.setStrengths(cleanListOrFallback(response.getStrengths(), fallback.getStrengths()));
        normalized.setWeaknesses(cleanListOrFallback(response.getWeaknesses(), fallback.getWeaknesses()));
        normalized.setImprovementSuggestions(cleanListOrFallback(response.getImprovementSuggestions(), fallback.getImprovementSuggestions()));
        normalized.setQuestionBreakdown(normalizedBreakdown);
        normalized.setFinalFeedback(sanitizer.sanitizeOption(
                response.getFinalFeedback(),
                fallback.getFinalFeedback(),
                500
        ));
        normalized.setFallbackUsed(fallbackUsed);
        normalized.setMessage(fallbackUsed
                ? BUSY_FALLBACK_MESSAGE
                : "Interview evaluation completed successfully.");
        return normalized;
    }

    private ResumeAnalysisResponse normalizeResumeAnalysis(
            ResumeAnalysisResponse response,
            String resumeText,
            String targetRole,
            String jobDescription,
            boolean fallbackUsed
    ) {
        ResumeAnalysisResponse fallback = buildFallbackResumeAnalysis(resumeText, targetRole, jobDescription);
        if (response == null) {
            fallback.setFallbackUsed(fallbackUsed);
            fallback.setMessage(fallbackUsed ? BUSY_FALLBACK_MESSAGE : "ATS analysis completed successfully.");
            return fallback;
        }

        // ENFORCE SINGLE SCORING ENGINE: Use the strict score from MatchingService (via fallback) instead of Gemini's arbitrary score.
        response.setAtsScore(fallback.getAtsScore());
        response.setKeywordMatch(fallback.getKeywordMatch());
        response.setAtsCompatibility(sanitizer.sanitizeOption(
                response.getAtsCompatibility(),
                fallback.getAtsCompatibility(),
                120
        ));
        response.setRecruiterImpression(sanitizer.sanitizeOption(
                response.getRecruiterImpression(),
                fallback.getRecruiterImpression(),
                220
        ));
        response.setFormattingQuality(sanitizer.sanitizeOption(
                response.getFormattingQuality(),
                fallback.getFormattingQuality(),
                180
        ));
        response.setFormattingIssues(cleanListOrFallback(response.getFormattingIssues(), fallback.getFormattingIssues()));
        response.setProjectQuality(sanitizer.sanitizeOption(
                response.getProjectQuality(),
                fallback.getProjectQuality(),
                180
        ));
        response.setStrengths(cleanListOrFallback(response.getStrengths(), fallback.getStrengths()));
        response.setWeaknesses(cleanListOrFallback(response.getWeaknesses(), fallback.getWeaknesses()));
        response.setMissingSkills(cleanListOrFallback(response.getMissingSkills(), fallback.getMissingSkills()));
        response.setImprovementSuggestions(cleanListOrFallback(response.getImprovementSuggestions(), fallback.getImprovementSuggestions()));
        List<String> candidateOptimizationTips = response.getOptimizationTips();
        if ((candidateOptimizationTips == null || candidateOptimizationTips.isEmpty())
                && response.getImprovementSuggestions() != null) {
            candidateOptimizationTips = response.getImprovementSuggestions();
        }
        response.setOptimizationTips(cleanList(
                candidateOptimizationTips,
                fallback.getOptimizationTips().get(0)
        ));
        response.setOptimizationFeedback(response.getOptimizationTips());
        // ENFORCE SINGLE SCORING ENGINE: Ensure keywords match exactly what the strict engine found
        response.setMissingKeywords(fallback.getMissingKeywords());
        response.setMatchedKeywords(fallback.getMatchedKeywords());
        response.setSummary(sanitizer.sanitizeOption(
                response.getSummary(),
                fallback.getSummary(),
                400
        ));
        response.setFinalVerdict(sanitizer.sanitizeOption(
                response.getFinalVerdict(),
                fallback.getFinalVerdict(),
                400
        ));
        response.setFallbackUsed(fallbackUsed);
        response.setMessage(fallbackUsed
                ? BUSY_FALLBACK_MESSAGE
                : "ATS analysis completed successfully.");
        return response;
    }

    private MatchResponse normalizeMatchResponse(
            MatchResponse response,
            String resumeText,
            String jobDescription,
            String targetRole,
            boolean fallbackUsed
    ) {
        MatchResponse fallback = buildFallbackJobMatch(resumeText, "", jobDescription, targetRole);
        if (response == null) {
            fallback.setFallbackUsed(fallbackUsed);
            fallback.setMessage(fallbackUsed ? BUSY_FALLBACK_MESSAGE : "Job match scoring completed successfully.");
            return fallback;
        }

        Integer candidateScore = response.getMatchPercentage() == null ? response.getScore() : response.getMatchPercentage();
        int safeMatch = blendScore(candidateScore, fallback.getMatchPercentage(), fallback.getMatchPercentage());
        response.setMatchPercentage(safeMatch);
        response.setScore(safeMatch);
        response.setMatchedSkills(cleanListOrFallback(response.getMatchedSkills(), fallback.getMatchedSkills()));
        response.setMissingSkills(cleanListOrFallback(response.getMissingSkills(), fallback.getMissingSkills()));
        response.setRecruiterSummary(sanitizer.sanitizeOption(
                response.getRecruiterSummary(),
                fallback.getRecruiterSummary(),
                400
        ));
        response.setFallbackUsed(fallbackUsed);
        response.setMessage(fallbackUsed
                ? BUSY_FALLBACK_MESSAGE
                : "Job match scoring completed successfully.");
        return response;
    }

    private List<String> cleanList(List<String> values, String fallbackValue) {
        List<String> cleaned = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                String sanitizedValue = sanitizer.sanitizeText(value, 240);
                if (!sanitizedValue.isBlank() && !cleaned.contains(sanitizedValue)) {
                    cleaned.add(sanitizedValue);
                }
            }
        }

        if (cleaned.isEmpty() && fallbackValue != null) {
            cleaned.add(fallbackValue);
        }

        return cleaned.stream().limit(6).toList();
    }

    private List<String> cleanListOrFallback(List<String> values, List<String> fallbackValues) {
        List<String> cleaned = cleanList(values, null);
        if (!cleaned.isEmpty()) {
            return cleaned;
        }
        return fallbackValues == null ? List.of() : fallbackValues.stream().limit(6).toList();
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        int safeValue = value == null ? fallback : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    private String sanitizeCategory(String category) {
        String cleaned = sanitizer.sanitizeOption(category, "Role-specific", 40);
        String normalized = cleaned.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("hr") || normalized.contains("behavior")) {
            return "HR";
        }
        if (normalized.contains("technical")) {
            return "Technical";
        }
        return "Role-specific";
    }

    private InterviewQuestionItem buildQuestionItem(int number, String category, String question) {
        InterviewQuestionItem item = new InterviewQuestionItem();
        item.setNumber(number);
        item.setCategory(category);
        item.setQuestion(question);
        return item;
    }

    private List<InterviewQuestionItem> buildFallbackQuestions(String jobRole, String difficulty, String interviewType, List<String> skills) {
        String primarySkill = skills.isEmpty() ? "your strongest technical skill" : skills.get(0);
        String normalizedType = interviewType.toLowerCase(Locale.ROOT);
        List<InterviewQuestionItem> questions = new ArrayList<>();

        if (!normalizedType.equals("hr")) {
            questions.add(buildQuestionItem(1, "Technical", "How have you used " + primarySkill + " in a recent " + jobRole + " project?"));
            questions.add(buildQuestionItem(2, "Technical", "How would you troubleshoot a production issue in a " + jobRole + " workflow?"));
        }

        questions.add(buildQuestionItem(3, "Role-specific", "What tradeoffs would you consider before selecting an implementation approach for this role?"));

        if (!normalizedType.equals("technical")) {
            questions.add(buildQuestionItem(4, "HR", "Tell me about a time you handled difficult feedback while working on a team."));
            questions.add(buildQuestionItem(5, "HR", "How do you communicate progress and blockers to stakeholders?"));
        }

        questions.add(buildQuestionItem(6, "Role-specific", "Why are you interested in this " + jobRole + " role?"));

        if ("advanced".equalsIgnoreCase(difficulty)) {
            questions.add(buildQuestionItem(7, "Role-specific", "Describe a high-impact decision you made with incomplete information."));
        }

        return questions;
    }

    private InterviewAnswerEvaluationResponse buildFallbackEvaluation(
            String question,
            String answer,
            String jobRole,
            List<String> expectedSkills
    ) {
        InterviewTranscriptItem transcriptItem = new InterviewTranscriptItem();
        transcriptItem.setQuestionNumber(1);
        transcriptItem.setQuestion(question);
        transcriptItem.setAnswer(answer);

        InterviewQuestionBreakdownItem breakdown = evaluateTranscriptItem(transcriptItem, jobRole, expectedSkills);
        InterviewAnswerEvaluationResponse response = new InterviewAnswerEvaluationResponse();
        response.setScore(breakdown.getScore());
        response.setOverallScore(breakdown.getScore());
        response.setCommunicationScore(breakdown.getCommunication());
        response.setTechnicalScore(breakdown.getTechnicalAccuracy());
        response.setStrengths(cleanListOrFallback(breakdown.getStrengths(), List.of("The answer provides a usable baseline for evaluation.")));
        response.setWeaknesses(cleanListOrFallback(breakdown.getWeaknesses(), List.of("The answer needs more specific role-relevant detail.")));
        response.setMissingConcepts(buildMissingConcepts(breakdown, expectedSkills));
        response.setImprovementSuggestions(buildSingleAnswerSuggestions(breakdown, expectedSkills));
        response.setCommunicationEvaluation(buildSingleAnswerCommunicationNarrative(breakdown));
        response.setTechnicalRelevance(buildSingleAnswerTechnicalNarrative(breakdown, expectedSkills));
        response.setSummary(buildSingleAnswerSummary(breakdown, jobRole));
        response.setFinalFeedback(response.getSummary());
        response.setFallbackUsed(true);
        response.setMessage("AI service temporarily unavailable.");
        return response;
    }

    private InterviewEvaluationResponse buildFallbackInterviewEvaluation(
            List<InterviewTranscriptItem> transcript,
            String jobRole,
            String difficulty,
            List<String> skills
    ) {
        List<InterviewQuestionBreakdownItem> breakdown = transcript.stream()
                .map(item -> evaluateTranscriptItem(item, jobRole, skills))
                .sorted(Comparator.comparingInt(item -> item.getQuestionNumber() == null ? 0 : item.getQuestionNumber()))
                .toList();

        int overallScore = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getScore);
        int communicationScore = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getCommunication);
        int technicalScore = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getTechnicalAccuracy);

        InterviewEvaluationResponse response = new InterviewEvaluationResponse();
        response.setOverallScore(overallScore);
        response.setCommunicationScore(communicationScore);
        response.setTechnicalScore(technicalScore);
        response.setStrengths(buildInterviewStrengths(breakdown, skills));
        response.setWeaknesses(buildInterviewWeaknesses(breakdown));
        response.setImprovementSuggestions(buildInterviewSuggestions(breakdown, skills));
        response.setQuestionBreakdown(breakdown);
        response.setFinalFeedback(buildInterviewFinalFeedback(overallScore, communicationScore, technicalScore, difficulty, breakdown));
        response.setFallbackUsed(true);
        response.setMessage("AI service temporarily unavailable.");
        return response;
    }

    private ResumeAnalysisResponse buildFallbackResumeAnalysis(String resumeText, String targetRole, String jobDescription) {
        List<String> targetKeywords = extractKeywords(targetRole + " " + jobDescription, 12);
        String normalizedResume = resumeText.toLowerCase(Locale.ROOT);
        boolean hasSections = normalizedResume.contains("experience") || normalizedResume.contains("education") || normalizedResume.contains("skills");
        
        // ENFORCE SINGLE SCORING ENGINE
        MatchResponse matchResponse = matchingService.calculateDetailedMatch(resumeText, targetRole, targetKeywords);
        int atsScore = matchResponse.getScore();
        List<String> matchedKeywords = matchResponse.getMatchedSkills() != null ? matchResponse.getMatchedSkills() : new ArrayList<>();
        List<String> missingKeywords = matchResponse.getMissingSkills() != null ? matchResponse.getMissingSkills() : new ArrayList<>();

        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setAtsScore(atsScore);
        response.setKeywordMatch(targetKeywords.isEmpty()
                ? atsScore
                : clamp((int) Math.round((matchedKeywords.size() * 100.0) / targetKeywords.size()), 0, 100, atsScore));
        response.setMatchedKeywords(matchedKeywords);
        response.setMissingKeywords(missingKeywords);
        response.setMissingSkills(missingKeywords.stream().limit(8).toList());
        response.setAtsCompatibility(atsScore >= 80 ? "High ATS compatibility" : atsScore >= 65 ? "Moderate ATS compatibility" : "Low ATS compatibility");
        response.setRecruiterImpression(atsScore >= 80
                ? "Recruiters are likely to see strong relevance quickly."
                : atsScore >= 65
                ? "Recruiters will see some relevant signal, but the resume needs sharper alignment."
                : "Recruiters may struggle to connect this resume to the target role without clearer tailoring.");
        response.setFormattingQuality(hasSections
                ? "Formatting appears readable for ATS parsers with recognizable section headings."
                : "Formatting likely needs clearer section headings and simpler structure for ATS parsing.");
        response.setFormattingIssues(hasSections
                ? List.of()
                : List.of("Add standard section headers to improve ATS readability."));
        response.setProjectQuality(normalizedResume.contains("project")
                ? "Projects are present, but clearer technical ownership and outcomes would strengthen them."
                : "Project work is not clearly surfaced and should be highlighted with technical outcomes.");
        response.setStrengths(List.of(
                matchedKeywords.isEmpty()
                        ? "The resume has baseline ATS-readable content."
                        : "The resume already reflects some role-relevant keywords."
        ));

        List<String> weaknesses = new ArrayList<>();
        if (!hasSections) {
            weaknesses.add("The resume needs standard section headers for better ATS parsing.");
        }
        if (!missingKeywords.isEmpty()) {
            weaknesses.add("Important target-role keywords are still missing.");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("The resume can still be tailored more tightly to the target job description.");
        }
        response.setWeaknesses(weaknesses.stream().limit(4).toList());

        List<String> feedback = new ArrayList<>();
        if (!matchedKeywords.isEmpty()) {
            feedback.add("Keep emphasizing proven keywords such as " + String.join(", ", matchedKeywords.stream().limit(4).toList()) + ".");
        }
        if (!missingKeywords.isEmpty()) {
            feedback.add("Add role-relevant keywords where truthful, especially " + String.join(", ", missingKeywords.stream().limit(4).toList()) + ".");
        }
        if (!hasSections) {
            feedback.add("Use standard section headers like Experience, Skills, and Education.");
        }
        feedback.add("Quantify outcomes with metrics to improve ranking and recruiter confidence.");
        response.setOptimizationTips(feedback.stream().limit(5).toList());
        response.setImprovementSuggestions(response.getOptimizationTips());
        response.setOptimizationFeedback(response.getOptimizationTips());
        response.setSummary(
                "Fallback ATS analysis for the " + targetRole + " role found "
                        + matchedKeywords.size()
                        + " matched keywords and "
                        + missingKeywords.size()
                        + " gaps to improve."
        );
        response.setFinalVerdict(atsScore >= 75
                ? "Strong ATS foundation with a few optimization opportunities."
                : atsScore >= 60
                ? "Moderate ATS alignment that should be improved before applying broadly."
                : "Low ATS alignment. Resume updates are recommended before applying.");
        response.setFallbackUsed(true);
        response.setMessage("AI service temporarily unavailable.");
        return response;
    }

    private MatchResponse buildFallbackJobMatch(
            String resumeText,
            String candidateProfile,
            String jobDescription,
            String targetRole
    ) {
        String normalizedResume = (resumeText + " " + candidateProfile).toLowerCase(Locale.ROOT);
        List<String> targetKeywords = extractKeywords(targetRole + " " + jobDescription, 12);
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String keyword : targetKeywords) {
            if (normalizedResume.contains(keyword.toLowerCase(Locale.ROOT))) {
                matchedSkills.add(keyword);
            } else {
                missingSkills.add(keyword);
            }
        }

        int matchPercentage = targetKeywords.isEmpty()
                ? 55
                : clamp((int) Math.round((matchedSkills.size() * 100.0) / targetKeywords.size()), 0, 100, 55);

        MatchResponse response = new MatchResponse();
        response.setScore(matchPercentage);
        response.setMatchPercentage(matchPercentage);
        response.setMatchedSkills(matchedSkills.isEmpty() ? List.of("General role alignment") : matchedSkills.stream().limit(6).toList());
        response.setMissingSkills(missingSkills.stream().limit(6).toList());
        response.setTargetRole(targetRole);
        response.setRecruiterSummary(
                "Fallback match scoring indicates "
                        + matchPercentage
                        + "% alignment for the "
                        + targetRole
                        + " role, with "
                        + matchedSkills.size()
                        + " matched keywords and "
                        + missingSkills.size()
                        + " gaps."
        );
        response.setFallbackUsed(true);
        response.setMessage("AI service temporarily unavailable.");
        return response;
    }

    private List<InterviewTranscriptItem> sanitizeTranscript(List<InterviewTranscriptItem> transcript) {
        List<InterviewTranscriptItem> cleaned = new ArrayList<>();
        if (transcript == null) {
            return cleaned;
        }

        int maxAnswerChars = Math.min(properties.getMaxInputChars(), 1800);
        for (InterviewTranscriptItem item : transcript) {
            if (item == null) {
                continue;
            }

            InterviewTranscriptItem normalized = new InterviewTranscriptItem();
            normalized.setQuestionNumber(item.getQuestionNumber() == null ? cleaned.size() + 1 : Math.max(1, item.getQuestionNumber()));
            normalized.setQuestion(sanitizer.sanitizeText(item.getQuestion(), 240));
            normalized.setAnswer(sanitizer.sanitizeText(item.getAnswer(), maxAnswerChars));
            if (normalized.getQuestion().isBlank()) {
                continue;
            }
            cleaned.add(normalized);
        }
        return cleaned;
    }

    private List<String> buildTranscriptLines(List<InterviewTranscriptItem> transcript) {
        List<String> lines = new ArrayList<>();
        for (InterviewTranscriptItem item : transcript) {
            lines.add("Q" + item.getQuestionNumber() + ": " + item.getQuestion());
            lines.add("A" + item.getQuestionNumber() + ": " + (item.getAnswer().isBlank() ? "No answer submitted." : item.getAnswer()));
        }
        return lines;
    }

    private List<InterviewQuestionBreakdownItem> normalizeQuestionBreakdown(
            List<InterviewQuestionBreakdownItem> questionBreakdown,
            List<InterviewTranscriptItem> transcript,
            String jobRole,
            List<String> skills
    ) {
        Map<Integer, InterviewQuestionBreakdownItem> provided = new LinkedHashMap<>();
        if (questionBreakdown != null) {
            for (InterviewQuestionBreakdownItem item : questionBreakdown) {
                if (item == null || item.getQuestionNumber() == null) {
                    continue;
                }
                provided.putIfAbsent(item.getQuestionNumber(), item);
            }
        }

        List<InterviewQuestionBreakdownItem> normalized = new ArrayList<>();
        for (InterviewTranscriptItem transcriptItem : transcript) {
            InterviewQuestionBreakdownItem fallback = evaluateTranscriptItem(transcriptItem, jobRole, skills);
            InterviewQuestionBreakdownItem candidate = provided.get(transcriptItem.getQuestionNumber());
            normalized.add(normalizeBreakdownItem(candidate, fallback));
        }
        return normalized;
    }

    private InterviewQuestionBreakdownItem normalizeBreakdownItem(
            InterviewQuestionBreakdownItem candidate,
            InterviewQuestionBreakdownItem fallback
    ) {
        if (candidate == null) {
            return fallback;
        }

        InterviewQuestionBreakdownItem normalized = new InterviewQuestionBreakdownItem();
        normalized.setQuestionNumber(fallback.getQuestionNumber());
        normalized.setQuestion(sanitizer.sanitizeOption(candidate.getQuestion(), fallback.getQuestion(), 240));
        normalized.setScore(clamp(candidate.getScore(), 0, 100, fallback.getScore()));
        normalized.setTechnicalAccuracy(clamp(candidate.getTechnicalAccuracy(), 0, 100, fallback.getTechnicalAccuracy()));
        normalized.setCommunication(clamp(candidate.getCommunication(), 0, 100, fallback.getCommunication()));
        normalized.setConfidence(clamp(candidate.getConfidence(), 0, 100, fallback.getConfidence()));
        normalized.setClarity(clamp(candidate.getClarity(), 0, 100, fallback.getClarity()));
        normalized.setCompleteness(clamp(candidate.getCompleteness(), 0, 100, fallback.getCompleteness()));
        normalized.setRoleRelevance(clamp(candidate.getRoleRelevance(), 0, 100, fallback.getRoleRelevance()));
        normalized.setProblemSolving(clamp(candidate.getProblemSolving(), 0, 100, fallback.getProblemSolving()));
        normalized.setFeedback(sanitizer.sanitizeOption(candidate.getFeedback(), fallback.getFeedback(), 320));
        normalized.setStrengths(cleanListOrFallback(candidate.getStrengths(), fallback.getStrengths()));
        normalized.setWeaknesses(cleanListOrFallback(candidate.getWeaknesses(), fallback.getWeaknesses()));
        return normalized;
    }

    private InterviewQuestionBreakdownItem evaluateTranscriptItem(
            InterviewTranscriptItem item,
            String jobRole,
            List<String> expectedSkills
    ) {
        String question = sanitizer.sanitizeText(item.getQuestion(), 240);
        String answer = sanitizer.sanitizeText(item.getAnswer(), Math.min(properties.getMaxInputChars(), 1800));
        String normalizedAnswer = answer.toLowerCase(Locale.ROOT);
        List<String> questionKeywords = extractKeywords(question, 6);
        List<String> roleKeywords = extractKeywords(jobRole, 4);

        InterviewQuestionBreakdownItem breakdown = new InterviewQuestionBreakdownItem();
        breakdown.setQuestionNumber(item.getQuestionNumber());
        breakdown.setQuestion(question);

        if (normalizedAnswer.isBlank()) {
            breakdown.setScore(8);
            breakdown.setTechnicalAccuracy(8);
            breakdown.setCommunication(12);
            breakdown.setConfidence(10);
            breakdown.setClarity(15);
            breakdown.setCompleteness(5);
            breakdown.setRoleRelevance(8);
            breakdown.setProblemSolving(5);
            breakdown.setStrengths(List.of("The question itself is clear and ready for a more complete response."));
            breakdown.setWeaknesses(List.of("No answer was submitted for this question."));
            breakdown.setFeedback("No answer was submitted, so this question could not be evaluated beyond completion.");
            return breakdown;
        }

        int wordCount = countWords(answer);
        int sentenceCount = countSentences(answer);
        int skillMatches = countMatches(normalizedAnswer, expectedSkills);
        int questionMatches = countMatches(normalizedAnswer, questionKeywords);
        int roleMatches = countMatches(normalizedAnswer, roleKeywords);
        int technicalSignals = countTermMatches(normalizedAnswer, TECHNICAL_SIGNAL_TERMS);
        int problemSignals = countTermMatches(normalizedAnswer, PROBLEM_SOLVING_TERMS);
        int structureSignals = countTermMatches(normalizedAnswer, STRUCTURE_TERMS);
        int confidentSignals = countTermMatches(normalizedAnswer, CONFIDENT_TERMS);
        int tentativeSignals = countTermMatches(normalizedAnswer, TENTATIVE_TERMS);
        int outcomeSignals = countTermMatches(normalizedAnswer, OUTCOME_TERMS);
        double averageWordsPerSentence = sentenceCount == 0 ? wordCount : (double) wordCount / sentenceCount;

        int technicalAccuracy = boundedScore(35 + (skillMatches * 12) + (questionMatches * 6) + (technicalSignals * 4)
                + Math.min(15, wordCount / 10) - (wordCount < 18 ? 18 : 0));
        int communication = boundedScore(38 + Math.min(20, wordCount / 5) + (structureSignals * 7)
                + (sentenceCount >= 2 ? 8 : 0) - (wordCount < 15 ? 16 : 0));
        int confidence = boundedScore(44 + (confidentSignals * 10) + (outcomeSignals * 5)
                - (tentativeSignals * 9) + (wordCount >= 35 ? 6 : 0));
        int clarity = boundedScore(40 + (structureSignals * 8) + (sentenceCount >= 2 ? 10 : 0)
                + (averageWordsPerSentence >= 8 && averageWordsPerSentence <= 28 ? 8 : 0)
                - (averageWordsPerSentence > 35 ? 8 : 0));
        int completeness = boundedScore(25 + Math.min(20, wordCount / 4) + (questionMatches * 6)
                + (outcomeSignals * 8) + (problemSignals * 4) - (wordCount < 18 ? 18 : 0));
        int roleRelevance = boundedScore(35 + (skillMatches * 12) + (roleMatches * 8) + (questionMatches * 5));
        int problemSolving = boundedScore(30 + (problemSignals * 10) + (outcomeSignals * 7)
                + (normalizedAnswer.contains("because") ? 6 : 0));

        int score = boundedScore(Math.round(
                (technicalAccuracy * 0.24f)
                        + (communication * 0.16f)
                        + (confidence * 0.10f)
                        + (clarity * 0.14f)
                        + (completeness * 0.14f)
                        + (roleRelevance * 0.12f)
                        + (problemSolving * 0.10f)
        ));

        breakdown.setScore(score);
        breakdown.setTechnicalAccuracy(technicalAccuracy);
        breakdown.setCommunication(communication);
        breakdown.setConfidence(confidence);
        breakdown.setClarity(clarity);
        breakdown.setCompleteness(completeness);
        breakdown.setRoleRelevance(roleRelevance);
        breakdown.setProblemSolving(problemSolving);
        breakdown.setStrengths(buildQuestionStrengths(skillMatches, structureSignals, outcomeSignals, score, clarity, roleRelevance));
        breakdown.setWeaknesses(buildQuestionWeaknesses(wordCount, technicalAccuracy, completeness, confidence, skillMatches));
        breakdown.setFeedback(buildQuestionFeedback(question, technicalAccuracy, clarity, completeness, roleRelevance, problemSolving));
        return breakdown;
    }

    private List<String> buildQuestionStrengths(
            int skillMatches,
            int structureSignals,
            int outcomeSignals,
            int score,
            int clarity,
            int roleRelevance
    ) {
        List<String> strengths = new ArrayList<>();
        if (clarity >= 70 || structureSignals >= 2) {
            strengths.add("The response is easy to follow and has a clear flow.");
        }
        if (roleRelevance >= 70 || skillMatches > 0) {
            strengths.add("The answer connects well to role-relevant tools or responsibilities.");
        }
        if (outcomeSignals > 0 || score >= 75) {
            strengths.add("There is useful signal about ownership, decisions, or outcomes.");
        }
        if (strengths.isEmpty()) {
            strengths.add("The answer provides enough context to evaluate the main idea.");
        }
        return strengths.stream().limit(3).toList();
    }

    private List<String> buildQuestionWeaknesses(
            int wordCount,
            int technicalAccuracy,
            int completeness,
            int confidence,
            int skillMatches
    ) {
        List<String> weaknesses = new ArrayList<>();
        if (wordCount < 25) {
            weaknesses.add("The answer is brief and needs a fuller example.");
        }
        if (technicalAccuracy < 60) {
            weaknesses.add("Technical details are too light to fully validate the approach.");
        }
        if (completeness < 60) {
            weaknesses.add("The response does not fully explain actions, tradeoffs, or results.");
        }
        if (confidence < 55) {
            weaknesses.add("The delivery sounds tentative instead of clearly owning the work.");
        }
        if (skillMatches == 0) {
            weaknesses.add("The answer does not explicitly name the most relevant tools or skills.");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("The answer would still benefit from sharper metrics or outcomes.");
        }
        return weaknesses.stream().limit(3).toList();
    }

    private String buildQuestionFeedback(
            String question,
            int technicalAccuracy,
            int clarity,
            int completeness,
            int roleRelevance,
            int problemSolving
    ) {
        List<String> positives = new ArrayList<>();
        if (technicalAccuracy >= 70) {
            positives.add("good technical signal");
        }
        if (clarity >= 70) {
            positives.add("clear communication");
        }
        if (roleRelevance >= 70) {
            positives.add("strong role alignment");
        }
        if (problemSolving >= 70) {
            positives.add("visible problem-solving");
        }

        List<String> gaps = new ArrayList<>();
        if (completeness < 60) {
            gaps.add("a fuller explanation of actions and outcomes");
        }
        if (technicalAccuracy < 60) {
            gaps.add("more precise technical detail");
        }
        if (roleRelevance < 60) {
            gaps.add("a clearer link back to the role");
        }

        String positiveText = positives.isEmpty()
                ? "The answer addresses the question"
                : "The answer shows " + joinNaturalLanguage(positives);
        String gapText = gaps.isEmpty()
                ? "and could be made even stronger with more measurable impact."
                : "and would be stronger with " + joinNaturalLanguage(gaps) + ".";
        return positiveText + " " + gapText;
    }

    private List<String> buildInterviewStrengths(List<InterviewQuestionBreakdownItem> breakdown, List<String> skills) {
        List<String> strengths = new ArrayList<>();
        int averageClarity = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getClarity);
        int averageCommunication = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getCommunication);
        int averageTechnical = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getTechnicalAccuracy);
        int averageProblemSolving = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getProblemSolving);
        long answeredCount = breakdown.stream().filter(item -> item.getCompleteness() != null && item.getCompleteness() > 10).count();

        if (averageClarity >= 70) {
            strengths.add("Answers were generally clear and easy to follow.");
        }
        if (averageCommunication >= 70) {
            strengths.add("Communication remained professional and reasonably well structured.");
        }
        if (averageTechnical >= 70) {
            strengths.add("Responses referenced relevant technical ideas for the role.");
        }
        if (averageProblemSolving >= 68) {
            strengths.add("Problem-solving reasoning was visible in several answers.");
        }
        if (!skills.isEmpty()) {
            strengths.add("The interview showed at least partial alignment with skills such as " + String.join(", ", skills.stream().limit(3).toList()) + ".");
        }
        if (answeredCount == breakdown.size() && !breakdown.isEmpty()) {
            strengths.add("All interview questions received a response, which improves scoring reliability.");
        }
        if (strengths.isEmpty()) {
            strengths.add("The interview provided enough signal to produce grounded feedback.");
        }
        return strengths.stream().distinct().limit(5).toList();
    }

    private List<String> buildInterviewWeaknesses(List<InterviewQuestionBreakdownItem> breakdown) {
        List<String> weaknesses = new ArrayList<>();
        long unanswered = breakdown.stream().filter(item -> item.getCompleteness() != null && item.getCompleteness() <= 10).count();
        int averageTechnical = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getTechnicalAccuracy);
        int averageCompleteness = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getCompleteness);
        int averageConfidence = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getConfidence);
        int averageRoleRelevance = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getRoleRelevance);

        if (unanswered > 0) {
            weaknesses.add("Some questions were left unanswered, which reduced the overall evaluation quality.");
        }
        if (averageTechnical < 65) {
            weaknesses.add("Technical depth was inconsistent and needs more explicit tools, decisions, or tradeoffs.");
        }
        if (averageCompleteness < 65) {
            weaknesses.add("Several answers stayed high level instead of walking through action and impact.");
        }
        if (averageConfidence < 60) {
            weaknesses.add("Some responses sounded tentative rather than clearly owning the work.");
        }
        if (averageRoleRelevance < 65) {
            weaknesses.add("Role alignment was not always explicit enough for the target position.");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("The interview can still improve by making examples more measurable and role-specific.");
        }
        return weaknesses.stream().distinct().limit(5).toList();
    }

    private List<String> buildInterviewSuggestions(List<InterviewQuestionBreakdownItem> breakdown, List<String> skills) {
        List<String> suggestions = new ArrayList<>();
        long unanswered = breakdown.stream().filter(item -> item.getCompleteness() != null && item.getCompleteness() <= 10).count();
        int averageTechnical = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getTechnicalAccuracy);
        int averageCompleteness = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getCompleteness);
        int averageClarity = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getClarity);
        int averageConfidence = averageQuestionMetric(breakdown, InterviewQuestionBreakdownItem::getConfidence);

        if (unanswered > 0) {
            suggestions.add("Complete every interview question so the evaluator can judge consistency across the full interview.");
        }
        if (averageCompleteness < 65) {
            suggestions.add("Use a concise structure such as context, action, technical decision, and measurable result.");
        }
        if (averageTechnical < 65) {
            suggestions.add("Name the specific tools, architecture choices, and tradeoffs you used in each example.");
        }
        if (averageClarity < 65) {
            suggestions.add("Lead with a direct answer first, then add supporting detail in a short logical sequence.");
        }
        if (averageConfidence < 60) {
            suggestions.add("Use first-person ownership language that makes your contribution and impact explicit.");
        }
        if (suggestions.isEmpty() && !skills.isEmpty()) {
            suggestions.add("Keep tying each answer back to the role skills, especially " + String.join(", ", skills.stream().limit(3).toList()) + ".");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Keep practicing with sharper examples, technical choices, and measurable outcomes.");
        }
        return suggestions.stream().distinct().limit(5).toList();
    }

    private List<String> buildSingleAnswerSuggestions(
            InterviewQuestionBreakdownItem breakdown,
            List<String> expectedSkills
    ) {
        List<String> suggestions = new ArrayList<>();
        if (breakdown.getCompleteness() != null && breakdown.getCompleteness() < 65) {
            suggestions.add("Use a short structure: context, action, technical decision, and result.");
        }
        if (breakdown.getTechnicalAccuracy() != null && breakdown.getTechnicalAccuracy() < 65) {
            suggestions.add("Add concrete tools, design choices, or debugging steps to show deeper technical accuracy.");
        }
        if (!expectedSkills.isEmpty()) {
            suggestions.add("Name the specific role-relevant skills you used, such as " + expectedSkills.get(0) + ".");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Add one measurable outcome to make the answer stronger.");
        }
        return suggestions.stream().distinct().limit(3).toList();
    }

    private List<String> buildMissingConcepts(
            InterviewQuestionBreakdownItem breakdown,
            List<String> expectedSkills
    ) {
        List<String> missingConcepts = new ArrayList<>();
        if (breakdown.getTechnicalAccuracy() != null && breakdown.getTechnicalAccuracy() < 65) {
            missingConcepts.add("Deeper technical implementation detail");
        }
        if (breakdown.getCompleteness() != null && breakdown.getCompleteness() < 65) {
            missingConcepts.add("Clearer explanation of actions, tradeoffs, and outcomes");
        }
        if (expectedSkills != null) {
            for (String skill : expectedSkills.stream().limit(2).toList()) {
                if (skill != null && !skill.isBlank()) {
                    missingConcepts.add(skill + " relevance");
                }
            }
        }
        if (missingConcepts.isEmpty()) {
            missingConcepts.add("More measurable impact");
        }
        return missingConcepts.stream().distinct().limit(4).toList();
    }

    private String buildSingleAnswerCommunicationNarrative(InterviewQuestionBreakdownItem breakdown) {
        if (breakdown.getCommunication() != null && breakdown.getCommunication() >= 70) {
            return "Communication is clear and understandable, with a solid structure that supports the main point.";
        }
        if (breakdown.getCommunication() != null && breakdown.getCommunication() >= 55) {
            return "Communication is understandable, but the answer would benefit from a tighter structure and clearer outcome.";
        }
        return "Communication needs stronger structure, more detail, and a more direct explanation of the example.";
    }

    private String buildSingleAnswerTechnicalNarrative(
            InterviewQuestionBreakdownItem breakdown,
            List<String> expectedSkills
    ) {
        if (breakdown.getTechnicalAccuracy() != null && breakdown.getTechnicalAccuracy() >= 70) {
            return "Technical relevance is strong because the answer references concrete decisions or tools connected to the role.";
        }
        if (!expectedSkills.isEmpty()) {
            return "Technical relevance would improve by explicitly naming how skills such as "
                    + String.join(", ", expectedSkills.stream().limit(2).toList())
                    + " were applied.";
        }
        return "Technical relevance is moderate and would improve with more precise role-specific detail.";
    }

    private String buildSingleAnswerSummary(InterviewQuestionBreakdownItem breakdown, String jobRole) {
        int score = breakdown.getScore() == null ? 0 : breakdown.getScore();
        if (score >= 80) {
            return "This answer shows strong readiness for a " + jobRole + " interview question.";
        }
        if (score >= 65) {
            return "This answer shows a useful foundation for the " + jobRole + " role with room for more specificity.";
        }
        return "This answer needs deeper detail and clearer role alignment for a " + jobRole + " interview.";
    }

    private String buildInterviewFinalFeedback(
            int overallScore,
            int communicationScore,
            int technicalScore,
            String difficulty,
            List<InterviewQuestionBreakdownItem> breakdown
    ) {
        long unanswered = breakdown.stream().filter(item -> item.getCompleteness() != null && item.getCompleteness() <= 10).count();
        String level = overallScore >= 85 ? "strong" : overallScore >= 70 ? "solid" : overallScore >= 55 ? "developing" : "early-stage";
        String focusArea;
        if (technicalScore <= communicationScore - 8) {
            focusArea = "deepening technical detail and tradeoff explanation";
        } else if (communicationScore <= technicalScore - 8) {
            focusArea = "making answers more structured and direct";
        } else {
            focusArea = "adding more specific outcomes and role-aligned examples";
        }

        String unansweredNote = unanswered > 0
                ? " " + unanswered + " question" + (unanswered == 1 ? " was" : "s were") + " left unanswered, which lowered confidence in the final score."
                : "";

        return "This was a " + level + " " + difficulty.toLowerCase(Locale.ROOT)
                + " interview performance. The clearest next step is "
                + focusArea + "." + unansweredNote;
    }

    private int averageQuestionMetric(List<InterviewQuestionBreakdownItem> breakdown, ToIntFunction<InterviewQuestionBreakdownItem> extractor) {
        if (breakdown == null || breakdown.isEmpty()) {
            return 0;
        }
        return boundedScore((int) Math.round(breakdown.stream().mapToInt(extractor).average().orElse(0)));
    }

    private int blendScore(Integer modelScore, int averageScore, int fallbackScore) {
        int safeModelScore = clamp(modelScore, 0, 100, fallbackScore);
        int safeAverageScore = averageScore <= 0 ? fallbackScore : averageScore;
        return boundedScore(Math.round((safeModelScore * 0.6f) + (safeAverageScore * 0.4f)));
    }

    private int boundedScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int countWords(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }

    private int countSentences(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Math.max(1, value.split("[.!?]+").length);
    }

    private int countMatches(String normalizedAnswer, List<String> candidates) {
        if (normalizedAnswer == null || normalizedAnswer.isBlank() || candidates == null) {
            return 0;
        }
        int matches = 0;
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && normalizedAnswer.contains(candidate.toLowerCase(Locale.ROOT))) {
                matches++;
            }
        }
        return matches;
    }

    private int countTermMatches(String normalizedAnswer, List<String> terms) {
        if (normalizedAnswer == null || normalizedAnswer.isBlank()) {
            return 0;
        }
        int matches = 0;
        for (String term : terms) {
            if (normalizedAnswer.contains(term)) {
                matches++;
            }
        }
        return matches;
    }

    private String joinNaturalLanguage(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        if (values.size() == 2) {
            return values.get(0) + " and " + values.get(1);
        }
        return String.join(", ", values.subList(0, values.size() - 1)) + ", and " + values.get(values.size() - 1);
    }

    private String cacheKey(Map<String, Object> inputs) {
        try {
            return objectMapper.writeValueAsString(inputs);
        } catch (JsonProcessingException exception) {
            logger.warn("Unable to serialize AI response cache key. Falling back to a compact hash.", exception);
            return Integer.toHexString(String.valueOf(inputs).hashCode());
        }
    }

    private Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        for (int index = 0; index < values.length - 1; index += 2) {
            ordered.put(String.valueOf(values[index]), values[index + 1]);
        }
        return ordered;
    }

    private List<String> extractKeywords(String source, int limit) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(source);
        while (matcher.find() && keywords.size() < limit) {
            String candidate = matcher.group().trim();
            if (!STOP_WORDS.contains(candidate.toLowerCase(Locale.ROOT))) {
                keywords.add(candidate);
            }
        }

        return new ArrayList<>(keywords);
    }

    public static class QuestionGenerationPayload {
        private List<QuestionEntry> questions;

        public List<QuestionEntry> getQuestions() {
            return questions;
        }

        public void setQuestions(List<QuestionEntry> questions) {
            this.questions = questions;
        }

        public static class QuestionEntry {
            private Integer number;
            private String category;
            private String question;

            public Integer getNumber() {
                return number;
            }

            public void setNumber(Integer number) {
                this.number = number;
            }

            public String getCategory() {
                return category;
            }

            public void setCategory(String category) {
                this.category = category;
            }

            public String getQuestion() {
                return question;
            }

            public void setQuestion(String question) {
                this.question = question;
            }
        }
    }
}
