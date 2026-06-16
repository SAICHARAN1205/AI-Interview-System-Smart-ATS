package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.ActiveSession;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ActiveSessionRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SessionService {

    @Autowired
    private ActiveSessionRepository activeSessionRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SecurityLogService securityLogService;

    public List<ActiveSession> getUserSessions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return activeSessionRepository.findByUserId(user.getId());
    }

    @Transactional
    public void revokeSession(String email, Long sessionId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ActiveSession session = activeSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUser().getId().equals(user.getId())) {
            securityLogService.logAction(email, "SUSPICIOUS_ACTIVITY", "Attempted to revoke session belonging to another user");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        activeSessionRepository.delete(session);
        securityLogService.logAction(email, "SESSION_REVOKED", "Session " + sessionId + " manually revoked");
    }

    @Transactional
    public void revokeAllSessions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        activeSessionRepository.deleteByUserId(user.getId());
        securityLogService.logAction(email, "ALL_SESSIONS_REVOKED", "All active sessions forcefully revoked");
    }
}
