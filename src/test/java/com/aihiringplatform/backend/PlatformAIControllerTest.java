package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationResponse;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.InterviewQuestionItem;
import com.aihiringplatform.backend.dto.JobMatchRequest;
import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.dto.QuestionResponse;
import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PlatformAIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIService aiService;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private JobService jobService;

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    void testGenerateInterviewQuestions() throws Exception {
        InterviewQuestionGenerationRequest request = new InterviewQuestionGenerationRequest();
        request.setJobRole("Java Developer");
        request.setDifficulty("Intermediate");
        request.setInterviewType("Mixed");
        request.setSkills(List.of("Java", "Spring Boot"));

        InterviewQuestionItem item = new InterviewQuestionItem();
        item.setNumber(1);
        item.setCategory("Technical");
        item.setQuestion("Explain constructor injection in Spring Boot.");

        QuestionResponse response = new QuestionResponse();
        response.setJobRole("Java Developer");
        response.setQuestions(List.of(item.getQuestion()));
        response.setStructuredQuestions(List.of(item));
        response.setFallbackUsed(false);

        when(aiService.generateInterviewQuestions(any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/interview/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.structuredQuestions[0].category").value("Technical"))
                .andExpect(jsonPath("$.data.questions[0]").value("Explain constructor injection in Spring Boot."));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    void testEvaluateInterviewAnswer() throws Exception {
        InterviewAnswerEvaluationRequest request = new InterviewAnswerEvaluationRequest();
        request.setQuestion("Explain dependency injection.");
        request.setCandidateAnswer("It improves testability and loose coupling.");
        request.setJobRole("Java Developer");
        request.setExpectedSkills(List.of("Java", "Spring Boot"));

        InterviewAnswerEvaluationResponse response = new InterviewAnswerEvaluationResponse();
        response.setScore(86);
        response.setCommunicationScore(82);
        response.setTechnicalScore(88);
        response.setStrengths(List.of("Clear answer"));
        response.setWeaknesses(List.of("Needs an example"));
        response.setImprovementSuggestions(List.of("Mention constructor injection"));
        response.setCommunicationEvaluation("Clear and concise.");
        response.setTechnicalRelevance("Relevant to the role.");
        response.setSummary("Strong answer.");

        when(aiService.evaluateAnswer(any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/interview/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(86))
                .andExpect(jsonPath("$.data.technicalRelevance").value("Relevant to the role."));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    void testAnalyzeResume() throws Exception {
        ResumeAnalysisRequest request = new ResumeAnalysisRequest();
        request.setTargetRole("Java Developer");
        request.setJobDescription("Need Java and Spring Boot.");

        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setAtsScore(84);
        response.setOptimizationFeedback(List.of("Add stronger project metrics."));
        response.setOptimizationTips(List.of("Add stronger project metrics."));
        response.setMissingKeywords(List.of("Docker"));
        response.setMatchedKeywords(List.of("Java"));

        when(resumeService.getResumeTextForCandidate(eq("candidate@example.com"))).thenReturn("Java Spring Boot projects");
        when(resumeService.getValidatedResumeTextForCandidate(eq("candidate@example.com"))).thenReturn(
            new ResumeService.ValidatedResumeText("Java Spring Boot projects", new com.aihiringplatform.backend.util.ResumeValidationUtils.ResumeValidationResult(true, 95, "high", java.util.List.of(), java.util.List.of(), null, null))
        );
        when(aiService.analyzeResume(any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/ats/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.atsScore").value(84))
                .andExpect(jsonPath("$.data.missingKeywords[0]").value("Docker"));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    void testScoreJobMatchByJobId() throws Exception {
        JobMatchRequest request = new JobMatchRequest();
        request.setJobId(7L);

        Job job = new Job();
        job.setId(7L);
        job.setTitle("Backend Engineer");
        job.setDescription("Need Java, Spring Boot, SQL");

        MatchResponse response = new MatchResponse();
        response.setMatchPercentage(79);
        response.setScore(79);
        response.setMatchedSkills(List.of("Java", "Spring Boot"));
        response.setMissingSkills(List.of("SQL"));
        response.setRecruiterSummary("Strong alignment with a small SQL gap.");

        when(jobService.getJobById(7L)).thenReturn(job);
        when(resumeService.getResumeTextForCandidate(eq("candidate@example.com"))).thenReturn("Java Spring Boot REST APIs");
        when(aiService.analyzeJobMatch(any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/match/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchPercentage").value(79))
                .andExpect(jsonPath("$.data.missingSkills[0]").value("SQL"));
    }
}
