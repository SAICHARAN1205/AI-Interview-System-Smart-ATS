package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.service.AnalyticsSnapshotService;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.MatchingService;
import com.aihiringplatform.backend.dto.JobMatchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/match")
public class MatchingController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MatchingController.class);

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private JobService jobService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AnalyticsSnapshotService analyticsSnapshotService;

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getMatchScore(@PathVariable Long jobId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String email = authentication.getName();
        User candidate = userRepository.findByEmail(email).orElse(null);
        if (candidate == null) return ResponseEntity.badRequest().body("User not found");

        Resume resume = resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId()).orElse(null);

        if (resume == null || resume.getExtractedText() == null) {
            return ResponseEntity.badRequest().body("No resume found or extracted text is empty");
        }

        try {
            Job job = jobService.getJobById(jobId);
            if (job == null) return ResponseEntity.badRequest().body("Job not found");

            MatchResponse response = matchingService.calculateDetailedMatch(
                    resume.getExtractedText(),
                    job
            );
            response.setJobId(jobId);
            JobMatchRequest snapshotRequest = new JobMatchRequest();
            snapshotRequest.setJobId(jobId);
            snapshotRequest.setJobDescription(job.getDescription());
            snapshotRequest.setTargetRole(job.getTitle());
            snapshotRequest.setResumeText(resume.getExtractedText());
            analyticsSnapshotService.recordJobMatch(email, snapshotRequest, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calculating match for job {}", jobId, e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", "AI service temporarily unavailable."
            ));
        }
    }
}
