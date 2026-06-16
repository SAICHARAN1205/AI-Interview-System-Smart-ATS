package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.InterviewAnswerRequest;
import com.aihiringplatform.backend.dto.InterviewResultResponse;
import com.aihiringplatform.backend.dto.InterviewSessionResponse;
import com.aihiringplatform.backend.dto.InterviewSessionStartRequest;
import com.aihiringplatform.backend.service.MockInterviewService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MockInterviewService mockInterviewService;

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testStartSessionSuccess() throws Exception {
        InterviewSessionStartRequest request = new InterviewSessionStartRequest();
        request.setJobRole("Java Developer");
        request.setDifficulty("Intermediate");
        request.setInterviewType("Technical");
        request.setSkills(List.of("Java", "Spring Boot"));
        request.setEstimatedDurationMinutes(20);

        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setId(10L);
        response.setJobRole("Java Developer");
        response.setStatus("IN_PROGRESS");
        response.setQuestions(List.of("What is Spring Boot?"));
        response.setAnswers(Map.of());

        when(mockInterviewService.startSession(any(InterviewSessionStartRequest.class), eq("candidate@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.questions[0]").value("What is Spring Boot?"));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testSaveAnswerSuccess() throws Exception {
        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setQuestionIndex(0);
        request.setAnswer("I use Spring Boot to create production-ready REST APIs.");
        request.setCurrentQuestionIndex(0);
        request.setElapsedSeconds(45);

        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setId(10L);
        response.setStatus("IN_PROGRESS");
        response.setAnswers(Map.of(0, request.getAnswer()));

        when(mockInterviewService.saveAnswer(eq(10L), any(InterviewAnswerRequest.class), eq("candidate@example.com")))
                .thenReturn(response);

        mockMvc.perform(put("/api/interview/sessions/10/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answers['0']").value(request.getAnswer()));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testSubmitSessionReturnsResult() throws Exception {
        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setQuestionIndex(0);
        request.setAnswer("I would explain the tradeoffs and outcome.");

        InterviewResultResponse result = new InterviewResultResponse();
        result.setSessionId(10L);
        result.setOverallScore(82);
        result.setCommunicationScore(80);
        result.setTechnicalScore(84);
        result.setStrengths(List.of("Clear structure"));
        result.setWeaknesses(List.of("Add deeper metrics"));
        result.setImprovementSuggestions(List.of("Use context, action, result"));

        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setId(10L);
        response.setStatus("COMPLETED");
        response.setResult(result);

        when(mockInterviewService.submitSession(eq(10L), any(InterviewAnswerRequest.class), eq("candidate@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/interview/sessions/10/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.result.overallScore").value(82));
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testRecruiterCannotStartCandidateInterview() throws Exception {
        InterviewSessionStartRequest request = new InterviewSessionStartRequest();
        request.setJobRole("Java Developer");

        mockMvc.perform(post("/api/interview/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
