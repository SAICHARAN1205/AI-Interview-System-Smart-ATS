package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.AdminDashboardOverviewResponse;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.repository.AIProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final AIProviderConfigRepository aiProviderConfigRepository;

    public AdminDashboardController(UserRepository userRepository, 
                                    JobRepository jobRepository, 
                                    ApplicationRepository applicationRepository, 
                                    AIProviderConfigRepository aiProviderConfigRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.aiProviderConfigRepository = aiProviderConfigRepository;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardOverviewResponse> getOverview() {
        long totalUsers = userRepository.count();
        long activeRecruiters = userRepository.countByRole(Role.RECRUITER);
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();
        long aiProvidersActive = aiProviderConfigRepository.countByIsEnabledTrue();

        AdminDashboardOverviewResponse response = new AdminDashboardOverviewResponse(
                totalUsers,
                activeRecruiters,
                totalJobs,
                totalApplications,
                "Online",
                aiProvidersActive
        );

        return ResponseEntity.ok(response);
    }
}
