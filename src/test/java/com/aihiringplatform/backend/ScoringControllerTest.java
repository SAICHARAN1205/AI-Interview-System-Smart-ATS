package com.aihiringplatform.backend;

import com.aihiringplatform.backend.controller.ScoringController;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.service.MatchingService;
import com.aihiringplatform.backend.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationRepository applicationRepository;

    @MockBean
    private ResumeRepository resumeRepository;

    @MockBean
    private MatchingService matchingService;

    @MockBean
    private ScoringService scoringService;

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testScoreSuccess() throws Exception {
        User candidate = new User();
        candidate.setId(1L);

        Job job = new Job();
        job.setDescription("java developer");

        Application app = new Application();
        app.setId(1L);
        app.setCandidate(candidate);
        app.setJob(job);

        Resume resume = new Resume();
        resume.setUser(candidate);
        resume.setExtractedText("java spring boot");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(resumeRepository.findAll()).thenReturn(List.of(resume));
        when(matchingService.calculateDetailedMatch(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Job.class))).thenReturn(new com.aihiringplatform.backend.dto.MatchResponse() {{ setScore(80); }});
        when(scoringService.calculateScore(anyInt(), anyInt())).thenReturn(82.5);

        mockMvc.perform(get("/api/score/1"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    public void testInvalidRoleCandidate() throws Exception {
        mockMvc.perform(get("/api/score/1"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testInvalidApplicationId() throws Exception {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/score/99"))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    public void testNoRecalculationIfScoreExists() throws Exception {
        User candidate = new User();
        candidate.setId(2L);

        Application app = new Application();
        app.setId(2L);
        app.setCandidate(candidate);
        app.setMatchScore(90); // Already has a score
        app.setAtsScore(85);

        when(applicationRepository.findById(2L)).thenReturn(Optional.of(app));
        when(scoringService.calculateScore(90, 85)).thenReturn(90.0);

        mockMvc.perform(get("/api/score/2"))
               .andExpect(status().isOk());

        // Verify that calculateDetailedMatch was NOT called because score already exists
        org.mockito.Mockito.verify(matchingService, org.mockito.Mockito.never())
                .calculateDetailedMatch(anyString(), org.mockito.ArgumentMatchers.any(Job.class));
    }
}
