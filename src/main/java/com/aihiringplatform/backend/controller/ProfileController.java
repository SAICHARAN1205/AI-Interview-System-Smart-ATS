package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.ProfileDTO;
import com.aihiringplatform.backend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> getProfile(Principal principal) {
        return ResponseEntity.ok(profileService.getProfile(principal.getName()));
    }

    @PutMapping("/candidate")
    public ResponseEntity<ProfileDTO> updateCandidateProfile(@RequestBody ProfileDTO dto, Principal principal) {
        return ResponseEntity.ok(profileService.updateCandidateProfile(principal.getName(), dto));
    }

    @PutMapping("/recruiter")
    public ResponseEntity<ProfileDTO> updateRecruiterProfile(@RequestBody ProfileDTO dto, Principal principal) {
        return ResponseEntity.ok(profileService.updateRecruiterProfile(principal.getName(), dto));
    }
}
