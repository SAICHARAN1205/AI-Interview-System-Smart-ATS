package com.aihiringplatform.backend;

import com.aihiringplatform.backend.controller.ApplicationController;
import com.aihiringplatform.backend.dto.CandidateApplicationResponse;
import com.aihiringplatform.backend.dto.RecruiterApplicantResponse;
import com.aihiringplatform.backend.entity.*;
import com.aihiringplatform.backend.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testApplyToJob_Success() throws Exception {
        CandidateApplicationResponse response = new CandidateApplicationResponse();
        response.setApplicationId(1L);
        response.setJobId(1L);
        response.setJobTitle("Software Engineer");
        response.setStatus("APPLIED");
        response.setAppliedAt(LocalDateTime.now());

        when(applicationService.applyToJob(1L, "candidate@example.com")).thenReturn(response);

        // GlobalResponseHandler wraps response body in ApiResponse{data:...}
        mockMvc.perform(post("/api/applications/apply/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(1L))
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    public void testApplyToJob_Unauthenticated() throws Exception {
        mockMvc.perform(post("/api/applications/apply/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testGetApplicationsForJob_Success() throws Exception {
        RecruiterApplicantResponse applicant = new RecruiterApplicantResponse();
        applicant.setApplicationId(1L);
        applicant.setCandidateId(1L);
        applicant.setCandidateName("John Doe");
        applicant.setCandidateEmail("candidate@example.com");
        applicant.setJobId(1L);
        applicant.setJobTitle("Software Engineer");
        applicant.setStatus("APPLIED");
        applicant.setAppliedAt(LocalDateTime.now());

        when(applicationService.getApplicationsForJob(1L, "recruiter@example.com"))
                .thenReturn(List.of(applicant));

        // ApplicationController returns List directly -> GlobalResponseHandler wraps in ApiResponse{data:[...]}
        mockMvc.perform(get("/api/applications/job/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("APPLIED"))
                .andExpect(jsonPath("$.data[0].candidateEmail").value("candidate@example.com"));
    }

    @Test
    public void testGetApplicationsForJob_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/applications/job/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testGetApplicationsForCandidate_Success() throws Exception {
        CandidateApplicationResponse application = new CandidateApplicationResponse();
        application.setApplicationId(1L);
        application.setJobId(10L);
        application.setJobTitle("Software Engineer");
        application.setCompanyName("AI Hiring Platform");
        application.setStatus("SHORTLISTED");
        application.setAppliedAt(LocalDateTime.now());

        when(applicationService.getApplicationsForCandidate("candidate@example.com"))
                .thenReturn(List.of(application));

        // GlobalResponseHandler wraps List in ApiResponse{data:[...]}
        mockMvc.perform(get("/api/applications/candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].applicationId").value(1L))
                .andExpect(jsonPath("$.data[0].jobId").value(10L))
                .andExpect(jsonPath("$.data[0].status").value("SHORTLISTED"));
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testUpdateApplicationStatus_Success() throws Exception {
        CandidateApplicationResponse response = new CandidateApplicationResponse();
        response.setApplicationId(1L);
        response.setStatus("SHORTLISTED");
        response.setAppliedAt(LocalDateTime.now());

        ApplicationController.StatusUpdateRequest request = new ApplicationController.StatusUpdateRequest();
        request.setStatus("shortlisted");

        when(applicationService.updateApplicationStatus(1L, "recruiter@example.com", ApplicationStatus.SHORTLISTED, null))
                .thenReturn(response);

        // GlobalResponseHandler wraps response in ApiResponse{data:...}
        mockMvc.perform(put("/api/applications/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(1L))
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));
    }

    @Test
    public void testUpdateApplicationStatus_Unauthenticated() throws Exception {
        ApplicationController.StatusUpdateRequest request = new ApplicationController.StatusUpdateRequest();
        request.setStatus("shortlisted");

        mockMvc.perform(put("/api/applications/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testUpdateApplicationStatus_InvalidStatus() throws Exception {
        ApplicationController.StatusUpdateRequest request = new ApplicationController.StatusUpdateRequest();
        request.setStatus("invalid_status");

        mockMvc.perform(put("/api/applications/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
