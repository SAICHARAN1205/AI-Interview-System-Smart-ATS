package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.dto.ResumeFileResponse;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.AnalyticsSnapshotService;
import com.aihiringplatform.backend.service.AiRateLimitService;
import com.aihiringplatform.backend.service.ResumeService;
import com.aihiringplatform.backend.util.ResumeValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/resumes")
@Validated
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private AIService aiService;

    @Autowired
    private AiRateLimitService aiRateLimitService;

    @Autowired
    private AnalyticsSnapshotService analyticsSnapshotService;

    @PostMapping("/upload")
    public ResponseEntity<ResumeFileResponse> uploadResume(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required.");
        }
        return ResponseEntity.ok(resumeService.uploadResume(file, authentication.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<ResumeFileResponse> getCurrentResume(Authentication authentication) {
        return ResponseEntity.ok(resumeService.getCurrentUserResume(authentication.getName()));
    }

    @GetMapping("/me/download")
    public ResponseEntity<byte[]> downloadCurrentResume(Authentication authentication) {
        ResumeService.DownloadableResume resume = resumeService.downloadCurrentUserResume(authentication.getName());
        return fileResponse(resume, "attachment");
    }

    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<ResumeFileResponse> getCandidateResume(@PathVariable Long candidateId, Authentication authentication) {
        return ResponseEntity.ok(resumeService.getCandidateResumeForRecruiter(candidateId, authentication.getName()));
    }

    @GetMapping("/status/{candidateId}")
    public ResponseEntity<?> getResumeStatus(@PathVariable Long candidateId, Authentication authentication) {
        return ResponseEntity.ok(resumeService.hasResumeForCandidate(candidateId, authentication.getName()));
    }

    @GetMapping("/download/{candidateId}")
    public ResponseEntity<byte[]> downloadResume(@PathVariable Long candidateId, Authentication authentication) {
        ResumeService.DownloadableResume resume = resumeService.downloadResumeForCandidate(candidateId, authentication.getName());
        return fileResponse(resume, "inline");
    }

    @GetMapping("/candidates/{candidateId}/download")
    public ResponseEntity<byte[]> downloadCandidateResume(@PathVariable Long candidateId, Authentication authentication) {
        ResumeService.DownloadableResume resume = resumeService.downloadResumeForCandidate(candidateId, authentication.getName());
        return fileResponse(resume, "attachment");
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(
            @Valid @RequestBody ResumeAnalysisRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        aiRateLimitService.assertAllowed("resume-analysis", authentication.getName() + "::" + httpServletRequest.getRemoteAddr());

        ResumeAnalysisRequest analysisRequest = new ResumeAnalysisRequest();
        analysisRequest.setTargetRole(request.getTargetRole());
        analysisRequest.setJobDescription(request.getJobDescription());
        ResumeService.ValidatedResumeText validatedResume =
                request.getResumeText() == null || request.getResumeText().trim().isBlank()
                        ? resumeService.getValidatedResumeTextForCandidate(authentication.getName())
                        : resumeService.validateResumeTextForAtsAnalysis(request.getResumeText(), "inline-resume-text");
        analysisRequest.setResumeText(validatedResume.text());

        Object response = aiService.analyzeResume(analysisRequest);
        if (response instanceof com.aihiringplatform.backend.dto.ResumeAnalysisResponse resumeAnalysisResponse) {
            applyResumeValidationMetadata(resumeAnalysisResponse, validatedResume.validationResult());
            analyticsSnapshotService.recordAtsAnalysis(authentication.getName(), analysisRequest, resumeAnalysisResponse);
        }
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<byte[]> fileResponse(ResumeService.DownloadableResume resume, String dispositionType) {
        String mimeType = resume.mimeType() == null || resume.mimeType().isBlank()
                ? "application/octet-stream"
                : resume.mimeType();
        String fileName = resume.fileName() == null || resume.fileName().isBlank()
                ? "resume"
                : resume.fileName();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + fileName + "\"")
                .body(resume.content());
    }

    private void applyResumeValidationMetadata(
            com.aihiringplatform.backend.dto.ResumeAnalysisResponse response,
            ResumeValidationUtils.ResumeValidationResult validationResult
    ) {
        if (response == null || validationResult == null) {
            return;
        }

        response.setResumeConfidenceScore(validationResult.resumeConfidenceScore());
        response.setResumeValidationStatus(validationResult.confidenceBand());
        response.setDetectedResumeSignals(validationResult.detectedSections());

        if (validationResult.warningMessage() == null || validationResult.warningMessage().isBlank()) {
            return;
        }

        if (response.getMessage() == null || response.getMessage().trim().isBlank()) {
            response.setMessage(validationResult.warningMessage());
            return;
        }

        if (!response.getMessage().contains(validationResult.warningMessage())) {
            response.setMessage(validationResult.warningMessage() + " " + response.getMessage().trim());
        }
    }
}
