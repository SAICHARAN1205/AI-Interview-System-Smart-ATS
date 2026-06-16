package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.ProfileDTO;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "candidate@example.com", roles = "CANDIDATE")
    void canGetCandidateProfile() throws Exception {
        ProfileDTO mockResponse = ProfileDTO.builder()
                .name("Test Candidate")
                .email("candidate@example.com")
                .role("CANDIDATE")
                .skills("Java, Spring")
                .build();

        when(profileService.getProfile("candidate@example.com")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Candidate"))
                .andExpect(jsonPath("$.data.skills").value("Java, Spring"));
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = "RECRUITER")
    void canUpdateRecruiterProfile() throws Exception {
        ProfileDTO requestDto = ProfileDTO.builder()
                .companyName("Acme Corp")
                .designation("Senior Tech Recruiter")
                .build();

        ProfileDTO responseDto = ProfileDTO.builder()
                .name("Test Recruiter")
                .email("recruiter@example.com")
                .role("RECRUITER")
                .companyName("Acme Corp")
                .designation("Senior Tech Recruiter")
                .build();

        when(profileService.updateRecruiterProfile(eq("recruiter@example.com"), any(ProfileDTO.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/profiles/recruiter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.data.designation").value("Senior Tech Recruiter"));
    }
}
