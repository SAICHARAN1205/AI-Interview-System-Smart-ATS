package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.InterviewAnswerRequest;
import com.aihiringplatform.backend.dto.InterviewEvaluationResponse;
import com.aihiringplatform.backend.dto.InterviewQuestionBreakdownItem;
import com.aihiringplatform.backend.entity.MockInterviewSession;
import com.aihiringplatform.backend.entity.MockInterviewStatus;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.MockInterviewSessionRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.MockInterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
class MockInterviewServiceTest {

    @Mock
    private MockInterviewSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AIService aiService;

    @Mock
    private com.aihiringplatform.backend.service.ActivityLogService activityLogService;

    private MockInterviewService service;

    @BeforeEach
    void setUp() {
        service = new MockInterviewService();
        ReflectionTestUtils.setField(service, "sessionRepository", sessionRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "jobRepository", jobRepository);
        ReflectionTestUtils.setField(service, "applicationRepository", applicationRepository);
        ReflectionTestUtils.setField(service, "aiService", aiService);
        ReflectionTestUtils.setField(service, "activityLogService", activityLogService);
    }

    @Test
    void submitSessionUsesSingleInterviewEvaluationForFiveQuestions() {
        MockInterviewSession session = buildSession();
        when(sessionRepository.findByIdAndCandidateEmail(10L, "candidate@example.com"))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(MockInterviewSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.evaluateInterview(any())).thenReturn(buildEvaluationResponse());

        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setElapsedSeconds(420);

        var response = service.submitSession(10L, request, "candidate@example.com");

        ArgumentCaptor<com.aihiringplatform.backend.dto.InterviewEvaluationRequest> captor =
                ArgumentCaptor.forClass(com.aihiringplatform.backend.dto.InterviewEvaluationRequest.class);
        verify(aiService, times(1)).evaluateInterview(captor.capture());
        verify(aiService, never()).evaluateAnswer(any());

        var evaluationRequest = captor.getValue();
        assertEquals(5, evaluationRequest.getTranscript().size());
        assertEquals("Backend Engineer", evaluationRequest.getJobRole());

        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getResult());
        assertEquals(84, response.getResult().getOverallScore());
        assertEquals(5, response.getResult().getQuestionBreakdown().size());
        assertTrue(Boolean.FALSE.equals(response.getResult().getFallbackUsed()));
    }

    @Test
    void repeatedInterviewSubmissionsStillUseOneEvaluationPerSession() {
        MockInterviewSession firstSession = buildSession();
        MockInterviewSession secondSession = buildSession();
        secondSession.setId(11L);

        when(sessionRepository.save(any(MockInterviewSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(aiService.evaluateInterview(any())).thenReturn(buildEvaluationResponse());

        when(sessionRepository.findByIdAndCandidateEmail(10L, "candidate@example.com"))
                .thenReturn(Optional.of(firstSession));
        service.submitSession(10L, new InterviewAnswerRequest(), "candidate@example.com");

        reset(sessionRepository);
        when(sessionRepository.save(any(MockInterviewSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findByIdAndCandidateEmail(11L, "candidate@example.com"))
                .thenReturn(Optional.of(secondSession));
        service.submitSession(11L, new InterviewAnswerRequest(), "candidate@example.com");

        verify(aiService, times(2)).evaluateInterview(any());
        verify(aiService, never()).evaluateAnswer(any());
    }

    private MockInterviewSession buildSession() {
        User candidate = new User();
        candidate.setId(7L);
        candidate.setEmail("candidate@example.com");
        candidate.setRole(Role.CANDIDATE);

        MockInterviewSession session = new MockInterviewSession();
        session.setId(10L);
        session.setCandidate(candidate);
        session.setJobRole("Backend Engineer");
        session.setDifficulty("Intermediate");
        session.setSkills("Java, Spring Boot, SQL");
        session.setStatus(MockInterviewStatus.IN_PROGRESS);
        session.setQuestionsJson(asJson(List.of(
                "How do you debug a production issue?",
                "Explain dependency injection.",
                "How do you communicate blockers?",
                "What tradeoffs do you consider?",
                "Why are you interested in this role?"
        )));

        Map<Integer, String> answers = new LinkedHashMap<>();
        answers.put(0, "I start with logs, isolate the service, reproduce the issue, and confirm the fix.");
        answers.put(1, "I prefer constructor injection because it improves testability and makes dependencies explicit.");
        answers.put(2, "I raise blockers early, explain impact, and align on next steps.");
        answers.put(3, "I compare performance, maintainability, and delivery risk.");
        answers.put(4, "I enjoy backend systems and have shipped Java APIs in production.");
        session.setAnswersJson(asJson(answers));
        session.setStartedAt(LocalDateTime.now().minusMinutes(20));
        session.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        return session;
    }

    private InterviewEvaluationResponse buildEvaluationResponse() {
        InterviewEvaluationResponse response = new InterviewEvaluationResponse();
        response.setOverallScore(84);
        response.setCommunicationScore(80);
        response.setTechnicalScore(86);
        response.setStrengths(List.of("Strong technical alignment", "Clear communication"));
        response.setWeaknesses(List.of("Some answers need more metrics"));
        response.setImprovementSuggestions(List.of("Add measurable outcomes", "Explain tradeoffs more explicitly"));
        response.setFinalFeedback("Strong interview performance with a few opportunities to add measurable impact.");
        response.setFallbackUsed(false);
        response.setQuestionBreakdown(List.of(
                question(1, "How do you debug a production issue?", 86),
                question(2, "Explain dependency injection.", 88),
                question(3, "How do you communicate blockers?", 80),
                question(4, "What tradeoffs do you consider?", 82),
                question(5, "Why are you interested in this role?", 84)
        ));
        return response;
    }

    private InterviewQuestionBreakdownItem question(int number, String question, int score) {
        InterviewQuestionBreakdownItem item = new InterviewQuestionBreakdownItem();
        item.setQuestionNumber(number);
        item.setQuestion(question);
        item.setScore(score);
        item.setTechnicalAccuracy(score);
        item.setCommunication(Math.max(0, score - 4));
        item.setConfidence(Math.max(0, score - 2));
        item.setClarity(Math.max(0, score - 3));
        item.setCompleteness(score);
        item.setRoleRelevance(score);
        item.setProblemSolving(Math.max(0, score - 1));
        item.setFeedback("Professional answer with clear role alignment.");
        item.setStrengths(List.of("Specific example"));
        item.setWeaknesses(List.of("Could add more metrics"));
        return item;
    }

    private String asJson(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
