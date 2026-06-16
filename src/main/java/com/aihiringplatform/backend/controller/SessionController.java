package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.entity.ActiveSession;
import com.aihiringplatform.backend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping
    public ResponseEntity<?> getActiveSessions(Authentication authentication) {
        String email = authentication.getName();
        List<ActiveSession> sessions = sessionService.getUserSessions(email);
        
        // Strip out the actual token to avoid sending it to the frontend
        List<Map<String, Object>> safeSessions = sessions.stream().map(s -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", s.getId());
            map.put("deviceInfo", s.getDeviceInfo() != null ? s.getDeviceInfo() : "Unknown Device");
            map.put("ipAddress", s.getIpAddress() != null ? s.getIpAddress() : "Unknown IP");
            map.put("lastActivity", s.getLastActivity());
            map.put("expiresAt", s.getExpiresAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(safeSessions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeSession(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        sessionService.revokeSession(email, id);
        return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> revokeAllSessions(Authentication authentication) {
        String email = authentication.getName();
        sessionService.revokeAllSessions(email);
        return ResponseEntity.ok(Map.of("message", "All devices logged out successfully"));
    }
}
