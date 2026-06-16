package com.aihiringplatform.backend;

import com.aihiringplatform.backend.controller.AIController;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationResponse;
import com.aihiringplatform.backend.dto.QuestionResponse;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @MockBean
    private AIService aiService;

    @MockBean
    private com.aihiringplatform.backend.service.AiRateLimitService aiRateLimitService;

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testSuccessValidJob() throws Exception {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Java Developer");
        job.setSkills("Java, Spring Boot");
        job.setDescription("java spring boot developer");

        when(jobService.getJobById(1L)).thenReturn(job);
        QuestionResponse response = new QuestionResponse();
        response.setQuestions(Arrays.asList("What is Spring Boot?", "Explain Dependency Injection."));
        when(aiService.generateInterviewQuestions(any(InterviewQuestionGenerationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/interview/questions/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.jobId").value(1))
               .andExpect(jsonPath("$.data.questions").isArray())
               .andExpect(jsonPath("$.data.questions[0]").value("What is Spring Boot?"));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testEvaluateAnswerSuccess() throws Exception {
        InterviewAnswerEvaluationRequest request = new InterviewAnswerEvaluationRequest();
        request.setQuestion("Explain dependency injection.");
        request.setCandidateAnswer("Dependency injection improves testability and loose coupling.");
        request.setJobRole("Java Developer");
        request.setExpectedSkills(List.of("Java", "Spring Boot"));

        InterviewAnswerEvaluationResponse response = new InterviewAnswerEvaluationResponse();
        response.setScore(86);
        response.setCommunicationScore(82);
        response.setTechnicalScore(88);
        response.setStrengths(List.of("Clear explanation"));
        response.setWeaknesses(List.of("Could add an example"));
        response.setImprovementSuggestions(List.of("Mention constructor injection"));
        response.setCommunicationEvaluation("Clear and concise.");
        response.setTechnicalRelevance("Directly relevant to Spring Boot architecture.");
        response.setSummary("Strong answer.");

        when(aiService.evaluateAnswer(any(InterviewAnswerEvaluationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/interview/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(86))
                .andExpect(jsonPath("$.data.technicalRelevance").value("Directly relevant to Spring Boot architecture."));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testEvaluateAnswerValidationFailure() throws Exception {
        InterviewAnswerEvaluationRequest request = new InterviewAnswerEvaluationRequest();
        request.setQuestion("");
        request.setCandidateAnswer("");
        request.setJobRole("");

        mockMvc.perform(post("/api/interview/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testInvalidRoleRecruiter() throws Exception {
         mockMvc.perform(get("/api/interview/questions/1"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testInvalidJobId() throws Exception {
        when(jobService.getJobById(99L)).thenReturn(null);
        
         mockMvc.perform(get("/api/interview/questions/99"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.message").value("Job not found."));
    }
}
