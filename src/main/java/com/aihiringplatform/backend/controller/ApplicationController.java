package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.CandidateApplicationResponse;
import com.aihiringplatform.backend.dto.RecruiterApplicantResponse;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    /**
     * POST /api/applications/apply/{jobId}
     * Requires CANDIDATE role.
     */
    @PostMapping("/apply/{jobId}")
    public ResponseEntity<CandidateApplicationResponse> applyToJob(@PathVariable Long jobId) {
        String candidateEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        CandidateApplicationResponse response = applicationService.applyToJob(jobId, candidateEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/applications/job/{jobId}
     * Requires RECRUITER role. Returns applicants for the recruiter's job.
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<RecruiterApplicantResponse>> getApplicationsForJob(@PathVariable Long jobId) {
        String recruiterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        List<RecruiterApplicantResponse> applicants = applicationService.getApplicationsForJob(jobId, recruiterEmail);
        return ResponseEntity.ok(applicants);
    }

    @GetMapping("/recruiter")
    public ResponseEntity<List<RecruiterApplicantResponse>> getApplicationsForRecruiter() {
        String recruiterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(applicationService.getApplicationsForRecruiter(recruiterEmail));
    }

    /**
     * GET /api/applications/candidate
     * Requires CANDIDATE role. Returns the logged-in candidate's own applications.
     */
    @GetMapping("/candidate")
    public ResponseEntity<List<CandidateApplicationResponse>> getApplicationsForCandidate() {
        String candidateEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        List<CandidateApplicationResponse> applications = applicationService.getApplicationsForCandidate(candidateEmail);
        return ResponseEntity.ok(applications);
    }

    /**
     * PUT /api/applications/{applicationId}/status
     * Requires RECRUITER role. Updates application status to SHORTLISTED or REJECTED.
     */
    @PutMapping("/{applicationId}/status")
    public ResponseEntity<CandidateApplicationResponse> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody StatusUpdateRequest request) {
        String recruiterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        ApplicationStatus status = parseStatus(request.getStatus());
        CandidateApplicationResponse response = applicationService.updateApplicationStatus(
                applicationId,
                recruiterEmail,
                status,
                request.getRejectionFeedback()
        );
        return ResponseEntity.ok(response);
    }

    private ApplicationStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application status is required.");
        }

        try {
            return ApplicationStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be APPLIED, ATS_REVIEW, SHORTLISTED, INTERVIEW_SCHEDULED, INTERVIEW_COMPLETED, REJECTED, or SELECTED."
            );
        }
    }

    /**
     * POST /api/applications/{applicationId}/recalculate-score
     * Requires RECRUITER role. Recalculates match/ATS score for an applicant.
     */
    @PostMapping("/{applicationId}/recalculate-score")
    public ResponseEntity<RecruiterApplicantResponse> recalculateScore(@PathVariable Long applicationId) {
        String recruiterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        RecruiterApplicantResponse response = applicationService.recalculateScoreForApplication(applicationId, recruiterEmail);
        return ResponseEntity.ok(response);
    }

    public static class StatusUpdateRequest {
        private String status;
        private String rejectionFeedback;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRejectionFeedback() {
            return rejectionFeedback;
        }

        public void setRejectionFeedback(String rejectionFeedback) {
            this.rejectionFeedback = rejectionFeedback;
        }
    }
}
