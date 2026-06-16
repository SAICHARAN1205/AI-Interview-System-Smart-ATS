package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.InterviewAnswerEvaluationRequest;
import com.aihiringplatform.backend.dto.InterviewQuestionGenerationRequest;
import com.aihiringplatform.backend.dto.InterviewAnswerRequest;
import com.aihiringplatform.backend.dto.InterviewSessionStartRequest;
import com.aihiringplatform.backend.dto.QuestionResponse;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.AiRateLimitService;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.MockInterviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
@Validated
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private JobService jobService;

    @Autowired
    private MockInterviewService mockInterviewService;

    @Autowired
    private AiRateLimitService aiRateLimitService;

    @GetMapping("/questions/{jobId}")
    public ResponseEntity<?> getQuestions(@PathVariable Long jobId, Authentication authentication, HttpServletRequest httpServletRequest) {
        aiRateLimitService.assertAllowed("interview-questions", actorKey(authentication, httpServletRequest));
        Job job = jobService.getJobById(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found.");
        }

        InterviewQuestionGenerationRequest request = new InterviewQuestionGenerationRequest();
        request.setJobRole(job.getTitle());
        request.setDifficulty("Intermediate");
        request.setInterviewType("Mixed");
        request.setSkills(job.getSkills() == null ? List.of() : List.of(job.getSkills().split(",")));
        request.setJobDescription(job.getDescription());

        QuestionResponse response = aiService.generateInterviewQuestions(request);
        response.setJobId(jobId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/questions/generate")
    public ResponseEntity<?> generateQuestions(
            @Valid @RequestBody InterviewQuestionGenerationRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        aiRateLimitService.assertAllowed("interview-questions", actorKey(authentication, httpServletRequest));
        return ResponseEntity.ok(aiService.generateInterviewQuestions(request));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateAnswer(
            @Valid @RequestBody InterviewAnswerEvaluationRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        aiRateLimitService.assertAllowed("interview-evaluate", actorKey(authentication, httpServletRequest));
        return ResponseEntity.ok(aiService.evaluateAnswer(request));
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(
            @Valid @RequestBody InterviewSessionStartRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        aiRateLimitService.assertAllowed("interview-session-start", actorKey(authentication, httpServletRequest));
        return ResponseEntity.ok(mockInterviewService.startSession(request, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getSession(sessionId, authentication.getName()));
    }

    @PutMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<?> saveAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody InterviewAnswerRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.saveAnswer(sessionId, request, authentication.getName()));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public ResponseEntity<?> submitSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody(required = false) InterviewAnswerRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        aiRateLimitService.assertAllowed("interview-session-submit", actorKey(authentication, httpServletRequest));
        return ResponseEntity.ok(mockInterviewService.submitSession(sessionId, request, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<?> getResult(
            @PathVariable Long sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getResult(sessionId, authentication.getName()));
    }

    
    @GetMapping("/sessions/history")
    public ResponseEntity<?> getCandidateHistory(Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getCandidateHistory(authentication.getName()));
    }

    @GetMapping("/recruiter/reviews")
    public ResponseEntity<?> getRecruiterReviews(Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getRecruiterReviewSessions(authentication.getName()));
    }

    @GetMapping("/recruiter/reviews/{sessionId}")
    public ResponseEntity<?> getRecruiterReviewResult(@PathVariable Long sessionId, Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.getRecruiterReviewResult(sessionId, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}/report.pdf")
    public ResponseEntity<byte[]> downloadInterviewReport(@PathVariable Long sessionId, Authentication authentication) {
        byte[] pdfBytes = mockInterviewService.downloadInterviewReport(sessionId, authentication.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "SmartATS_Interview_Report_" + sessionId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @PutMapping("/sessions/{sessionId}/monitoring")
    public ResponseEntity<?> reportMonitoring(
            @PathVariable Long sessionId,
            @RequestBody java.util.List<com.aihiringplatform.backend.dto.MonitoringEventDto> events,
            Authentication authentication) {
        return ResponseEntity.ok(mockInterviewService.reportMonitoringEvents(sessionId, events, authentication.getName()));
    }

    private String actorKey(Authentication authentication, HttpServletRequest httpServletRequest) {
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }

        return httpServletRequest == null ? "anonymous" : httpServletRequest.getRemoteAddr();
    }
}


