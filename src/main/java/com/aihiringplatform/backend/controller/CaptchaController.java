package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.CaptchaResponse;
import com.aihiringplatform.backend.service.CaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public CaptchaResponse getCaptcha() {
        return captchaService.generateCaptcha();
    }
}
