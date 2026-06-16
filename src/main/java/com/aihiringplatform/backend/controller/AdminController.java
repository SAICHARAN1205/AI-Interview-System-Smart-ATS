package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.ApiResponse;
import com.aihiringplatform.backend.entity.AIProviderConfig;
import com.aihiringplatform.backend.entity.RecruiterProfile;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.entity.UserStatus;
import com.aihiringplatform.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private String getAdminEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = adminService.getAllUsers(getAdminEmail());
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<User>> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        UserStatus status = UserStatus.valueOf(body.get("status"));
        User updated = adminService.updateUserStatus(getAdminEmail(), userId, status);
        return ResponseEntity.ok(ApiResponse.success("User status updated", updated));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(getAdminEmail(), userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/recruiters")
    public ResponseEntity<ApiResponse<List<RecruiterProfile>>> getAllRecruiters() {
        List<RecruiterProfile> profiles = adminService.getAllRecruiterProfiles(getAdminEmail());
        return ResponseEntity.ok(ApiResponse.success("Recruiters fetched successfully", profiles));
    }

    @PutMapping("/recruiters/{profileId}/verify")
    public ResponseEntity<ApiResponse<RecruiterProfile>> verifyRecruiter(@PathVariable Long profileId, @RequestBody Map<String, String> body) {
        RecruiterProfile.VerificationStatus status = RecruiterProfile.VerificationStatus.valueOf(body.get("status"));
        RecruiterProfile updated = adminService.updateRecruiterVerification(getAdminEmail(), profileId, status);
        return ResponseEntity.ok(ApiResponse.success("Recruiter verification status updated", updated));
    }

    @GetMapping("/ai/configs")
    public ResponseEntity<ApiResponse<List<AIProviderConfig>>> getAIConfigs() {
        List<AIProviderConfig> configs = adminService.getAIConfigs(getAdminEmail());
        return ResponseEntity.ok(ApiResponse.success("AI configurations fetched successfully", configs));
    }

    @PostMapping("/ai/configs")
    public ResponseEntity<ApiResponse<AIProviderConfig>> createAIConfig(@RequestBody AIProviderConfig config) {
        AIProviderConfig created = adminService.createAIConfig(getAdminEmail(), config);
        return ResponseEntity.ok(ApiResponse.success("AI configuration created", created));
    }

    @PutMapping("/ai/configs/{configId}")
    public ResponseEntity<ApiResponse<AIProviderConfig>> updateAIConfig(@PathVariable Long configId, @RequestBody AIProviderConfig updates) {
        AIProviderConfig updated = adminService.updateAIConfig(getAdminEmail(), configId, updates.isEnabled(), updates.getPriorityOrder());
        return ResponseEntity.ok(ApiResponse.success("AI configuration updated", updated));
    }

    @DeleteMapping("/ai/configs/{configId}")
    public ResponseEntity<ApiResponse<Void>> deleteAIConfig(@PathVariable Long configId) {
        adminService.deleteAIConfig(getAdminEmail(), configId);
        return ResponseEntity.ok(ApiResponse.success("AI configuration deleted", null));
    }
}
