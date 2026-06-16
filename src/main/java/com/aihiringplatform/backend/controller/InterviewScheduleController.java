package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.InterviewScheduleRequest;
import com.aihiringplatform.backend.service.InterviewScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interviews")
public class InterviewScheduleController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InterviewScheduleController.class);

    @Autowired
    private InterviewScheduleService interviewScheduleService;

    @GetMapping("/recruiter")
    public ResponseEntity<?> getRecruiterSchedules() {
        String recruiterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(interviewScheduleService.getSchedulesForRecruiter(recruiterEmail));
    }

    @PostMapping
    public ResponseEntity<?> scheduleInterview(@RequestBody InterviewScheduleRequest request) {
        logger.info("Incoming Schedule Interview Request. Payload: applicationId={}, interviewDate={}, interviewTime={}", 
            request.getApplicationId(), request.getInterviewDate(), request.getInterviewTime());
            
        String recruiterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(interviewScheduleService.scheduleInterview(request, recruiterEmail));
    }
}
