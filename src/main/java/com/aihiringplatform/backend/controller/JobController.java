package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aihiringplatform.backend.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Job>> createJob(@RequestBody Job job) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Recruiter account not found."));

        logger.info("Creating job for recruiter: {} with role: {}", userEmail, recruiter.getRole());

        Job createdJob = jobService.createJob(job, recruiter);
        return ResponseEntity.ok(ApiResponse.success("Job created", createdJob));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<Job>>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching jobs page: {} size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Job> jobs = jobService.getAllJobs(pageable);
        // Strip internal recruiterNotes from public responses
        jobs.getContent().forEach(job -> job.setRecruiterNotes(null));
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id);
        // Strip internal recruiterNotes from public responses
        job.setRecruiterNotes(null);
        return ResponseEntity.ok(ApiResponse.success("Job fetched", job));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> updateJob(@PathVariable Long id, @RequestBody Job updates) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Recruiter account not found."));
        Job updatedJob = jobService.updateJob(id, updates, recruiter);
        return ResponseEntity.ok(ApiResponse.success("Job updated", updatedJob));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Recruiter account not found."));
        jobService.deleteJob(id, recruiter);
        return ResponseEntity.ok(ApiResponse.success("Job deleted", null));
    }
}
