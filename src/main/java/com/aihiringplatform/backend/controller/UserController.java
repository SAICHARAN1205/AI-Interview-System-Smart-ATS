package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.PasswordChangeRequest;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.service.CaptchaService;
import com.aihiringplatform.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        captchaService.validateCaptcha(user.getCaptchaToken(), user.getCaptchaAnswer());
        return ResponseEntity.ok(userService.registerUser(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getCurrentUserSummary(principal.getName()));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request, Principal principal) {
        userService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

}
