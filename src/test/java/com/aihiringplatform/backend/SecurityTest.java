package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.AuthRequest;
import com.aihiringplatform.backend.dto.AuthResponse;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JobService jobService;

    @MockBean
    private com.aihiringplatform.backend.repository.ActiveSessionRepository activeSessionRepository;

    @MockBean
    private CaptchaService captchaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    public void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testProtectedEndpointWithValidToken() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(Role.CANDIDATE);
        user.setEmailVerified(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        doNothing().when(captchaService).validateCaptcha(any(), any());

        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        // Since GlobalResponseHandler wraps the response, the token is at $.data.token
        String token = objectMapper.readTree(loginResponse).path("data").path("token").asText();

        mockMvc.perform(get("/api/profiles/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    public void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/profiles/me")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRoleBased_CandidateCannotCreateJob() throws Exception {
        User candidate = new User();
        candidate.setId(2L);
        candidate.setEmail("candidate@example.com");
        candidate.setPassword(passwordEncoder.encode("password"));
        candidate.setRole(Role.CANDIDATE);
        candidate.setEmailVerified(true);

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidate));
        doNothing().when(captchaService).validateCaptcha(any(), any());

        AuthRequest request = new AuthRequest();
        request.setEmail("candidate@example.com");
        request.setPassword("password");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).path("data").path("token").asText();

        Job job = new Job();
        job.setTitle("Test Job");
        job.setDescription("Test Description");
        job.setCompanyName("Test Company");

        mockMvc.perform(post("/api/jobs/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRoleBased_RecruiterCanCreateJob() throws Exception {
        User recruiter = new User();
        recruiter.setId(3L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setPassword(passwordEncoder.encode("password"));
        recruiter.setRole(Role.RECRUITER);
        recruiter.setEmailVerified(true);

        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        doNothing().when(captchaService).validateCaptcha(any(), any());

        AuthRequest request = new AuthRequest();
        request.setEmail("recruiter@example.com");
        request.setPassword("password");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).path("data").path("token").asText();

        Job job = new Job();
        job.setTitle("Test Job");
        job.setDescription("Test Description");
        job.setCompanyName("Test Company");

        Job savedJob = new Job();
        savedJob.setId(1L);
        savedJob.setTitle("Test Job");

        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(jobService.createJob(any(Job.class), eq(recruiter))).thenReturn(savedJob);

        mockMvc.perform(post("/api/jobs/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }
}
