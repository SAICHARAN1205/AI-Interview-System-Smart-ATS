package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.CandidateAnalyticsResponse;
import com.aihiringplatform.backend.dto.RecruiterAnalyticsResponse;
import com.aihiringplatform.backend.service.AnalyticsService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/recruiter")
    public ResponseEntity<RecruiterAnalyticsResponse> getRecruiterAnalytics(
            Authentication authentication,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String jobRole,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String experienceLevel
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePrivate())
                .body(analyticsService.getRecruiterAnalytics(
                        authentication.getName(), 
                        isAdmin(authentication),
                        startDate,
                        endDate,
                        jobRole,
                        workMode,
                        experienceLevel
                ));
    }

    @GetMapping("/candidate")
    public ResponseEntity<CandidateAnalyticsResponse> getCandidateAnalytics(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePrivate())
                .body(analyticsService.getCandidateAnalytics(authentication.getName()));
    }

    @GetMapping("/recruiter/export.csv")
    public ResponseEntity<byte[]> exportRecruiterAnalytics(
            Authentication authentication,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String jobRole,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String experienceLevel
    ) {
        String csv = analyticsService.exportRecruiterAnalyticsCsv(
                authentication.getName(),
                startDate,
                endDate,
                jobRole,
                workMode,
                experienceLevel
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smartats-recruiter-analytics.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority()));
    }
}
