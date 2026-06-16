package com.aihiringplatform.backend;

import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ResumeRepository resumeRepository;

    @MockBean
    private JobService jobService;

    @MockBean
    private MatchingService matchingService;

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testMatchSuccess() throws Exception {
        User candidate = new User();
        candidate.setId(1L);
        candidate.setEmail("candidate@example.com");

        Resume resume = new Resume();
        resume.setUser(candidate);
        resume.setExtractedText("java spring test");

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        job.setDescription("java spring");

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidate));
        when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(1L)).thenReturn(Optional.of(resume));
        when(jobService.getJobById(1L)).thenReturn(job);

        com.aihiringplatform.backend.dto.MatchResponse response = new com.aihiringplatform.backend.dto.MatchResponse();
        response.setMatchPercentage(82);
        response.setScore(82);
        response.setMatchedSkills(java.util.List.of("java", "spring"));
        when(matchingService.calculateDetailedMatch(org.mockito.ArgumentMatchers.eq("java spring test"), org.mockito.ArgumentMatchers.any(Job.class))).thenReturn(response);

        mockMvc.perform(get("/api/match/1"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testMatch_NoResume() throws Exception {
        User candidate = new User();
        candidate.setId(1L);
        candidate.setEmail("candidate@example.com");

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidate));
        when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/match/1"))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testInvalidRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/match/1"))
               .andExpect(status().isForbidden());
    }
}
