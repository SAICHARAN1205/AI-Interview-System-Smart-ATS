package com.aihiringplatform.backend;

import com.aihiringplatform.backend.controller.JobController;
import com.aihiringplatform.backend.dto.AuthRequest;
import com.aihiringplatform.backend.dto.AuthResponse;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testCreateJob_Success() throws Exception {
        // Given
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = new Job();
        job.setTitle("Software Engineer");
        job.setDescription("Great job opportunity");
        job.setCompanyName("Tech Corp");
        job.setLocation("New York");

        Job savedJob = new Job();
        savedJob.setId(1L);
        savedJob.setTitle("Software Engineer");
        savedJob.setDescription("Great job opportunity");
        savedJob.setCompanyName("Tech Corp");
        savedJob.setLocation("New York");
        savedJob.setRecruiter(recruiter);
        savedJob.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(jobService.createJob(any(Job.class), eq(recruiter))).thenReturn(savedJob);

        // When & Then
        mockMvc.perform(post("/api/jobs/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Software Engineer"))
                .andExpect(jsonPath("$.data.companyName").value("Tech Corp"));
    }

    @Test
    public void testCreateJob_Unauthenticated() throws Exception {
        // Given
        Job job = new Job();
        job.setTitle("Software Engineer");
        job.setDescription("Great job opportunity");
        job.setCompanyName("Tech Corp");

        // When & Then (unauthenticated user is blocked by SecurityConfig -> 401)
        mockMvc.perform(post("/api/jobs/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAllJobs_Success() throws Exception {
        // Given
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setRole(Role.RECRUITER);

        Job job1 = new Job();
        job1.setId(1L);
        job1.setTitle("Software Engineer");
        job1.setCompanyName("Tech Corp");
        job1.setRecruiter(recruiter);

        Job job2 = new Job();
        job2.setId(2L);
        job2.setTitle("Product Manager");
        job2.setCompanyName("Biz Corp");
        job2.setRecruiter(recruiter);

        List<Job> jobs = Arrays.asList(job1, job2);
        Page<Job> jobPage = new PageImpl<>(jobs, PageRequest.of(0, 10), jobs.size());
        when(jobService.getAllJobs(any())).thenReturn(jobPage);

        // When & Then
        mockMvc.perform(get("/api/jobs/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("Software Engineer"))
                .andExpect(jsonPath("$.data.content[1].title").value("Product Manager"));
    }

    @Test
    public void testGetJobById_Success() throws Exception {
        // Given
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Software Engineer");
        job.setDescription("Great job opportunity");
        job.setCompanyName("Tech Corp");
        job.setRecruiter(recruiter);

        when(jobService.getJobById(1L)).thenReturn(job);

        // When & Then (public endpoint, no auth needed)
        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("Software Engineer"))
                .andExpect(jsonPath("$.data.companyName").value("Tech Corp"));
    }

    @Test
    public void testGetJobById_NotFound() throws Exception {
        // Given
        when(jobService.getJobById(999L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Job not found"));

        // When & Then (public endpoint, should return 500 error with message)
        mockMvc.perform(get("/api/jobs/999"))
                .andExpect(status().isBadRequest());
    }
}
