package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.AIProviderConfig;
import com.aihiringplatform.backend.entity.RecruiterProfile;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.entity.UserStatus;
import com.aihiringplatform.backend.repository.AIProviderConfigRepository;
import com.aihiringplatform.backend.repository.RecruiterProfileRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private AIProviderConfigRepository aiProviderConfigRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public void verifyAdmin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required.");
        }
    }

    public List<User> getAllUsers(String adminEmail) {
        verifyAdmin(adminEmail);
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserStatus(String adminEmail, Long userId, UserStatus status) {
        verifyAdmin(adminEmail);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setAccountStatus(status);
        User savedUser = userRepository.save(user);

        activityLogService.logSuccess(adminEmail, "ADMIN", "USER_STATUS_UPDATE", "Admin updated user " + user.getEmail() + " status to " + status.name(), null);

        return savedUser;
    }

    @Transactional
    public RecruiterProfile updateRecruiterVerification(String adminEmail, Long profileId, RecruiterProfile.VerificationStatus status) {
        verifyAdmin(adminEmail);
        RecruiterProfile profile = recruiterProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter profile not found"));
        
        profile.setVerificationStatus(status);
        
        // If approved, also set user status to active
        if (status == RecruiterProfile.VerificationStatus.APPROVED) {
            User user = profile.getUser();
            if (user != null) {
                user.setAccountStatus(UserStatus.ACTIVE);
                userRepository.save(user);
            }
        }
        
        RecruiterProfile savedProfile = recruiterProfileRepository.save(profile);
        
        activityLogService.logSuccess(adminEmail, "ADMIN", "RECRUITER_VERIFICATION", "Admin updated recruiter profile " + profile.getId() + " verification status to " + status.name(), null);

        return savedProfile;
    }

    @Transactional
    public void deleteUser(String adminEmail, Long userId) {
        verifyAdmin(adminEmail);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(user);
        activityLogService.logSuccess(adminEmail, "ADMIN", "USER_DELETE", "Admin deleted user " + user.getEmail(), null);
    }

    @Transactional(readOnly = true)
    public List<RecruiterProfile> getAllRecruiterProfiles(String adminEmail) {
        verifyAdmin(adminEmail);
        List<RecruiterProfile> profiles = recruiterProfileRepository.findAll();
        // Initialize lazy users to avoid LazyInitializationException during JSON serialization
        profiles.forEach(p -> {
            if (p.getUser() != null) {
                p.getUser().getId(); 
            }
        });
        return profiles;
    }

    // AI Provider configs
    @Transactional(readOnly = true)
    public List<AIProviderConfig> getAIConfigs(String adminEmail) {
        verifyAdmin(adminEmail);
        return aiProviderConfigRepository.findAllByOrderByPriorityOrderAsc();
    }

    @Transactional
    public AIProviderConfig createAIConfig(String adminEmail, AIProviderConfig config) {
        verifyAdmin(adminEmail);
        AIProviderConfig saved = aiProviderConfigRepository.save(config);
        activityLogService.logSuccess(adminEmail, "ADMIN", "AI_CONFIG_CREATE", "Admin created AI config for " + config.getProviderName(), null);
        return saved;
    }

    @Transactional
    public AIProviderConfig updateAIConfig(String adminEmail, Long configId, boolean isEnabled, int priorityOrder) {
        verifyAdmin(adminEmail);
        AIProviderConfig config = aiProviderConfigRepository.findById(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider config not found"));
        
        config.setEnabled(isEnabled);
        config.setPriorityOrder(priorityOrder);
        
        AIProviderConfig savedConfig = aiProviderConfigRepository.save(config);
        
        activityLogService.logSuccess(adminEmail, "ADMIN", "AI_CONFIG_UPDATE", "Admin updated AI provider config for " + config.getProviderName() + ": enabled=" + isEnabled + ", priority=" + priorityOrder, null);

        return savedConfig;
    }

    @Transactional
    public void deleteAIConfig(String adminEmail, Long configId) {
        verifyAdmin(adminEmail);
        AIProviderConfig config = aiProviderConfigRepository.findById(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider config not found"));
        aiProviderConfigRepository.delete(config);
        activityLogService.logSuccess(adminEmail, "ADMIN", "AI_CONFIG_DELETE", "Admin deleted AI config for " + config.getProviderName(), null);
    }
}
