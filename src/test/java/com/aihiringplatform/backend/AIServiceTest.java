package com.aihiringplatform.backend;

import com.aihiringplatform.backend.config.AiStabilityProperties;
import com.aihiringplatform.backend.config.GeminiProperties;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationResponse;
import com.aihiringplatform.backend.dto.InterviewEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewQuestionBreakdownItem;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.InterviewTranscriptItem;
import com.aihiringplatform.backend.dto.JobMatchRequest;
import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.AiInputSanitizer;
import com.aihiringplatform.backend.service.AiResponseCacheService;
import com.aihiringplatform.backend.service.PromptBuilderService;
import com.aihiringplatform.backend.service.ai.AIGatewayResult;
import com.aihiringplatform.backend.service.ai.AIGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AIServiceTest {

    private static final String BUSY_FALLBACK_MESSAGE = "AI service temporarily unavailable. Using fallback evaluation.";

    @Mock
    private AIGatewayService gatewayService;

    @Mock
    private com.aihiringplatform.backend.service.MatchingService matchingService;

    private AIService aiService;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setMaxInputChars(6000);
        properties.setMaxResumeChars(24000);
        properties.setMaxJobDescriptionChars(12000);
        properties.setMaxSkills(12);
        AiStabilityProperties stabilityProperties = new AiStabilityProperties();
        stabilityProperties.setCacheTtlMillis(120000L);
        stabilityProperties.setCacheMaxEntries(50);
        aiService = new AIService(
                gatewayService,
                properties,
                new AiInputSanitizer(),
                new PromptBuilderService(),
                new AiResponseCacheService(stabilityProperties, new ObjectMapper()),
                new ObjectMapper(),
                matchingService
        );
    }

    @Test
    void testQuestionGenerationFallsBackWhenGeminiFails() {
        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(null, "SmartATS local fallback", true));

        InterviewQuestionGenerationRequest request = new InterviewQuestionGenerationRequest();
        request.setJobRole("Backend Engineer");
        request.setDifficulty("Advanced");
        request.setInterviewType("Mixed");
        request.setSkills(List.of("Java", "Spring Boot"));

        List<String> questions = aiService.generateQuestions(request);

        assertFalse(questions.isEmpty());
        assertTrue(questions.stream().anyMatch(question -> question.toLowerCase().contains("java")
                || question.toLowerCase().contains("spring")
                || question.toLowerCase().contains("backend engineer")));
    }

    @Test
    void testQuestionGenerationUsesOpenRouterResponse() {
        AIService.QuestionGenerationPayload payload = new AIService.QuestionGenerationPayload();
        payload.setQuestions(List.of(
                questionEntry(1, "Technical", "How have you used Spring Boot in production systems?"),
                questionEntry(2, "HR", "How do you communicate blockers to stakeholders?"),
                questionEntry(3, "Role-specific", "How do you balance delivery speed and maintainability?"),
                questionEntry(4, "Technical", "How do you debug a failing backend service in production?"),
                questionEntry(5, "HR", "Tell me about a time you handled difficult feedback."),
                questionEntry(6, "Role-specific", "Why are you interested in this backend engineer role?")
        ));

        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(payload, "OpenRouter", false));

        InterviewQuestionGenerationRequest request = new InterviewQuestionGenerationRequest();
        request.setJobRole("Backend Engineer");
        request.setDifficulty("Intermediate");
        request.setInterviewType("Mixed");
        request.setSkills(List.of("Java", "Spring Boot"));

        var response = aiService.generateInterviewQuestions(request);

        assertFalse(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertEquals("How have you used Spring Boot in production systems?", response.getQuestions().get(0));
        assertTrue(response.getStructuredQuestions().size() >= 5);
    }

    @Test
    void testAnswerEvaluationFallsBackWhenGeminiFails() {
        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(null, "SmartATS local fallback", true));

        InterviewAnswerEvaluationRequest request = new InterviewAnswerEvaluationRequest();
        request.setQuestion("Explain dependency injection.");
        request.setCandidateAnswer("I use constructor injection in Spring Boot services because it improves testability.");
        request.setJobRole("Java Developer");
        request.setExpectedSkills(List.of("Java", "Spring Boot"));

        var response = aiService.evaluateAnswer(request);

        assertNotNull(response);
        assertNotNull(response.getScore());
        assertFalse(response.getStrengths().isEmpty());
        assertFalse(response.getImprovementSuggestions().isEmpty());
        assertTrue(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertEquals(BUSY_FALLBACK_MESSAGE, response.getMessage());
    }

    @Test
    void testAnswerEvaluationUsesOpenRouterResponse() {
        InterviewAnswerEvaluationResponse modelResponse = new InterviewAnswerEvaluationResponse();
        modelResponse.setScore(91);
        modelResponse.setCommunicationScore(86);
        modelResponse.setTechnicalScore(93);
        modelResponse.setStrengths(List.of("Strong role alignment"));
        modelResponse.setWeaknesses(List.of("Could mention metrics"));
        modelResponse.setImprovementSuggestions(List.of("Add measurable outcomes"));
        modelResponse.setCommunicationEvaluation("Clear and structured.");
        modelResponse.setTechnicalRelevance("Strong technical relevance.");
        modelResponse.setSummary("Strong answer.");

        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(modelResponse, "OpenRouter", false));

        InterviewAnswerEvaluationRequest request = new InterviewAnswerEvaluationRequest();
        request.setQuestion("Explain dependency injection.");
        request.setCandidateAnswer("I use constructor injection in Spring Boot services because it improves testability.");
        request.setJobRole("Java Developer");
        request.setExpectedSkills(List.of("Java", "Spring Boot"));

        var response = aiService.evaluateAnswer(request);

        assertFalse(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertTrue(response.getScore() >= 70);
        assertEquals("Strong answer.", response.getSummary());
    }

    @Test
    void testInterviewEvaluationFallsBackWhenGeminiFails() {
        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(null, "SmartATS local fallback", true));

        InterviewEvaluationRequest request = new InterviewEvaluationRequest();
        request.setJobRole("Java Developer");
        request.setDifficulty("Intermediate");
        request.setSkills(List.of("Java", "Spring Boot", "SQL"));
        request.setTranscript(List.of(
                transcriptItem(1, "How do you debug a production issue?", "I start with logs, isolate the failing service, reproduce the issue, and patch the root cause."),
                transcriptItem(2, "Explain dependency injection.", "I prefer constructor injection in Spring Boot because it improves testability."),
                transcriptItem(3, "Tell me about a blocker.", "I share the blocker early, explain the impact, and propose next steps."),
                transcriptItem(4, "How do you handle tradeoffs?", "I compare performance, maintainability, and delivery risk before deciding."),
                transcriptItem(5, "Why this role?", "I like backend work and I have used Java and Spring Boot on production APIs.")
        ));

        var response = aiService.evaluateInterview(request);

        assertNotNull(response);
        assertNotNull(response.getOverallScore());
        assertEquals(5, response.getQuestionBreakdown().size());
        assertFalse(response.getStrengths().isEmpty());
        assertFalse(response.getImprovementSuggestions().isEmpty());
        assertTrue(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertEquals(BUSY_FALLBACK_MESSAGE, response.getMessage());
    }

    @Test
    void testResumeAnalysisFallsBackWhenGeminiFails() {
        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(null, "SmartATS local fallback", true));

        ResumeAnalysisRequest request = new ResumeAnalysisRequest();
        request.setTargetRole("Java Developer");
        request.setResumeText("Java Spring Boot REST API SQL Experience Education Skills");
        request.setJobDescription("Need Java, Spring Boot, SQL, Docker, and communication.");

        MatchResponse matchResponse = new MatchResponse();
        matchResponse.setScore(45);
        when(matchingService.calculateDetailedMatch(any(), any(), any())).thenReturn(matchResponse);

        var response = aiService.analyzeResume(request);

        assertNotNull(response);
        assertNotNull(response.getAtsScore());
        assertFalse(response.getOptimizationFeedback().isEmpty());
        assertNotNull(response.getMissingKeywords());
        assertTrue(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertEquals(BUSY_FALLBACK_MESSAGE, response.getMessage());
    }

    @Test
    void testResumeAnalysisUsesOpenRouterResponse() {
        ResumeAnalysisResponse modelResponse = new ResumeAnalysisResponse();
        modelResponse.setAtsScore(88);
        modelResponse.setAtsCompatibility("Strong ATS compatibility.");
        modelResponse.setFormattingQuality("Clean formatting.");
        modelResponse.setProjectQuality("Projects are relevant.");
        modelResponse.setStrengths(List.of("Good keyword coverage"));
        modelResponse.setWeaknesses(List.of("Could quantify project impact"));
        modelResponse.setOptimizationTips(List.of("Add measurable outcomes"));
        modelResponse.setMissingKeywords(List.of("Kubernetes"));
        modelResponse.setMatchedKeywords(List.of("Java", "Spring Boot"));
        modelResponse.setSummary("Strong ATS fit.");

        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(modelResponse, "OpenRouter", false));

        ResumeAnalysisRequest request = new ResumeAnalysisRequest();
        request.setTargetRole("Java Developer");
        request.setResumeText("Java Spring Boot REST API SQL Experience Education Skills");
        request.setJobDescription("Need Java, Spring Boot, SQL, Docker, and communication.");

        MatchResponse matchResponse = new MatchResponse();
        matchResponse.setScore(88);
        when(matchingService.calculateDetailedMatch(any(), any(), any())).thenReturn(matchResponse);

        var response = aiService.analyzeResume(request);

        assertEquals(88, response.getAtsScore());
        assertFalse(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertEquals("Strong ATS fit.", response.getSummary());
    }

    @Test
    void testJobMatchFallsBackWhenGeminiFails() {
        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(null, "SmartATS local fallback", true));

        JobMatchRequest request = new JobMatchRequest();
        request.setTargetRole("Java Developer");
        request.setResumeText("Java Spring Boot REST APIs SQL Docker");
        request.setJobDescription("Looking for Java, Spring Boot, SQL, Kubernetes, and communication.");

        var response = aiService.analyzeJobMatch(request);

        assertNotNull(response);
        assertNotNull(response.getMatchPercentage());
        assertFalse(response.getMatchedSkills().isEmpty());
        assertNotNull(response.getRecruiterSummary());
        assertTrue(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertEquals(BUSY_FALLBACK_MESSAGE, response.getMessage());
    }

    @Test
    void testJobMatchUsesOpenRouterResponse() {
        MatchResponse modelResponse = new MatchResponse();
        modelResponse.setMatchPercentage(84);
        modelResponse.setScore(84);
        modelResponse.setMatchedSkills(List.of("Java", "Spring Boot"));
        modelResponse.setMissingSkills(List.of("Kubernetes"));
        modelResponse.setRecruiterSummary("Good backend alignment.");

        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(modelResponse, "OpenRouter", false));

        JobMatchRequest request = new JobMatchRequest();
        request.setTargetRole("Java Developer");
        request.setResumeText("Java Spring Boot REST APIs SQL Docker");
        request.setJobDescription("Looking for Java, Spring Boot, SQL, Kubernetes, and communication.");

        var response = aiService.analyzeJobMatch(request);

        assertFalse(Boolean.TRUE.equals(response.getFallbackUsed()));
        assertTrue(response.getMatchPercentage() >= 60);
        assertEquals("Good backend alignment.", response.getRecruiterSummary());
    }

    @Test
    void testResumeAnalysisCachesRecentResult() {
        ResumeAnalysisResponse modelResponse = new ResumeAnalysisResponse();
        modelResponse.setAtsScore(84);
        modelResponse.setAtsCompatibility("Strong ATS compatibility.");
        modelResponse.setFormattingQuality("Formatting is ATS-friendly.");
        modelResponse.setProjectQuality("Projects are relevant.");
        modelResponse.setStrengths(List.of("Role-relevant keywords are present"));
        modelResponse.setWeaknesses(List.of("Could quantify outcomes more"));
        modelResponse.setOptimizationTips(List.of("Add measurable impact"));
        modelResponse.setMissingKeywords(List.of("Kubernetes"));
        modelResponse.setMatchedKeywords(List.of("Java", "Spring Boot"));
        modelResponse.setSummary("Strong ATS alignment.");

        when(gatewayService.generateStructuredResponse(any(), any(), any()))
                .thenReturn(new AIGatewayResult<>(modelResponse, "OpenRouter", false));

        ResumeAnalysisRequest request = new ResumeAnalysisRequest();
        request.setTargetRole("Java Developer");
        request.setResumeText("Java Spring Boot REST API SQL Experience Education Skills");
        request.setJobDescription("Need Java, Spring Boot, SQL, Docker, and communication.");

        MatchResponse matchResponse = new MatchResponse();
        matchResponse.setScore(84);
        when(matchingService.calculateDetailedMatch(any(), any(), any())).thenReturn(matchResponse);

        ResumeAnalysisResponse first = aiService.analyzeResume(request);
        ResumeAnalysisResponse second = aiService.analyzeResume(request);

        assertTrue(first.getAtsScore() >= 80);
        assertEquals(first.getAtsScore(), second.getAtsScore());
        assertEquals(first.getSummary(), second.getSummary());
        verify(gatewayService, times(1)).generateStructuredResponse(any(), any(), any());
    }

    private AIService.QuestionGenerationPayload.QuestionEntry questionEntry(int number, String category, String question) {
        AIService.QuestionGenerationPayload.QuestionEntry entry = new AIService.QuestionGenerationPayload.QuestionEntry();
        entry.setNumber(number);
        entry.setCategory(category);
        entry.setQuestion(question);
        return entry;
    }

    private InterviewTranscriptItem transcriptItem(int number, String question, String answer) {
        InterviewTranscriptItem item = new InterviewTranscriptItem();
        item.setQuestionNumber(number);
        item.setQuestion(question);
        item.setAnswer(answer);
        return item;
    }
}
