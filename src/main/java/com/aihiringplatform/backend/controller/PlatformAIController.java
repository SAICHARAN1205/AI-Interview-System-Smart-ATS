package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.JobMatchRequest;
import com.aihiringplatform.backend.dto.QuestionResponse;
import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import com.aihiringplatform.backend.util.ResumeValidationUtils;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.AnalyticsSnapshotService;
import com.aihiringplatform.backend.service.AiRateLimitService;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.ResumeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/ai")
@Validated
public class PlatformAIController {

    private final AIService aiService;
    private final AiRateLimitService aiRateLimitService;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final AnalyticsSnapshotService analyticsSnapshotService;

    public PlatformAIController(
            AIService aiService,
            AiRateLimitService aiRateLimitService,
            ResumeService resumeService,
            JobService jobService,
            AnalyticsSnapshotService analyticsSnapshotService
    ) {
        this.aiService = aiService;
        this.aiRateLimitService = aiRateLimitService;
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.analyticsSnapshotService = analyticsSnapshotService;
    }

    @PostMapping("/interview/generate")
    public ResponseEntity<QuestionResponse> generateInterviewQuestions(
            @Valid @RequestBody InterviewQuestionGenerationRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        aiRateLimitService.assertAllowed("ai-interview-generate", actorKey(authentication, httpServletRequest));
        return ResponseEntity.ok(aiService.generateInterviewQuestions(request));
    }

    @PostMapping("/interview/evaluate")
    public ResponseEntity<?> evaluateInterviewAnswer(
            @Valid @RequestBody InterviewAnswerEvaluationRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        aiRateLimitService.assertAllowed("ai-interview-evaluate", actorKey(authentication, httpServletRequest));
        return ResponseEntity.ok(aiService.evaluateAnswer(request));
    }

    @PostMapping("/ats/analyze")
    public ResponseEntity<?> analyzeResume(
            @Valid @RequestBody ResumeAnalysisRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        aiRateLimitService.assertAllowed("ai-ats-analyze", actorKey(authentication, httpServletRequest));

        ResumeAnalysisRequest normalizedRequest = new ResumeAnalysisRequest();
        normalizedRequest.setTargetRole(request.getTargetRole());
        normalizedRequest.setJobDescription(request.getJobDescription());
        ResumeService.ValidatedResumeText validatedResume = hasText(request.getResumeText())
                ? resumeService.validateResumeTextForAtsAnalysis(request.getResumeText(), "inline-resume-text")
                : resumeService.getValidatedResumeTextForCandidate(authentication.getName());
        normalizedRequest.setResumeText(validatedResume.text());

        ResumeAnalysisResponse response = aiService.analyzeResume(normalizedRequest);
        applyResumeValidationMetadata(response, validatedResume.validationResult());
        analyticsSnapshotService.recordAtsAnalysis(authentication.getName(), normalizedRequest, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/match/score")
    public ResponseEntity<?> scoreJobMatch(
            @Valid @RequestBody JobMatchRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        aiRateLimitService.assertAllowed("ai-match-score", actorKey(authentication, httpServletRequest));

        JobMatchRequest normalizedRequest = new JobMatchRequest();
        normalizedRequest.setJobId(request.getJobId());
        normalizedRequest.setCandidateProfile(request.getCandidateProfile());
        normalizedRequest.setTargetRole(request.getTargetRole());

        if (hasText(request.getJobDescription())) {
            normalizedRequest.setJobDescription(request.getJobDescription());
        } else if (request.getJobId() != null) {
            Job job = jobService.getJobById(request.getJobId());
            if (job == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Job not found.");
            }

            normalizedRequest.setJobDescription(job.getDescription());
            if (!hasText(normalizedRequest.getTargetRole())) {
                normalizedRequest.setTargetRole(job.getTitle());
            }
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Job description or jobId is required.");
        }

        if (hasText(request.getResumeText())) {
            normalizedRequest.setResumeText(request.getResumeText());
        } else if (authentication != null && authentication.getName() != null) {
            normalizedRequest.setResumeText(resumeService.getResumeTextForCandidate(authentication.getName()));
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Resume text is required.");
        }

        var response = aiService.analyzeJobMatch(normalizedRequest);
        if (authentication != null && authentication.getName() != null) {
            analyticsSnapshotService.recordJobMatch(authentication.getName(), normalizedRequest, response);
        }
        return ResponseEntity.ok(response);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String actorKey(Authentication authentication, HttpServletRequest httpServletRequest) {
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }

        return httpServletRequest == null ? "anonymous" : httpServletRequest.getRemoteAddr();
    }

    private void applyResumeValidationMetadata(
            ResumeAnalysisResponse response,
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

        if (!hasText(response.getMessage())) {
            response.setMessage(validationResult.warningMessage());
            return;
        }

        if (!response.getMessage().contains(validationResult.warningMessage())) {
            response.setMessage(validationResult.warningMessage() + " " + response.getMessage().trim());
        }
    }
}
