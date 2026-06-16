package com.aihiringplatform.backend;

import com.aihiringplatform.backend.controller.InterviewScheduleController;
import com.aihiringplatform.backend.dto.InterviewScheduleRequest;
import com.aihiringplatform.backend.dto.InterviewScheduleResponse;
import com.aihiringplatform.backend.service.InterviewScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InterviewScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterviewScheduleService interviewScheduleService;

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testScheduleInterviewSuccess() throws Exception {
        InterviewScheduleRequest request = new InterviewScheduleRequest();
        request.setApplicationId(10L);
        request.setInterviewDate("2026-07-01");
        request.setInterviewTime("14:30");

        InterviewScheduleResponse response = new InterviewScheduleResponse();
        response.setId(100L);
        response.setApplicationId(10L);
        response.setScheduledAt(LocalDateTime.of(2026, 7, 1, 14, 30));

        when(interviewScheduleService.scheduleInterview(any(InterviewScheduleRequest.class), eq("recruiter@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.applicationId").value(10));
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testScheduleInterviewMissingDateTime() throws Exception {
        InterviewScheduleRequest request = new InterviewScheduleRequest();
        request.setApplicationId(10L);
        // Missing date and time
        
        when(interviewScheduleService.scheduleInterview(any(InterviewScheduleRequest.class), eq("recruiter@example.com")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application, date, and time are required."));

        mockMvc.perform(post("/api/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Application, date, and time are required."));
    }
}
