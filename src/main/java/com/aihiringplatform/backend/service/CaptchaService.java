package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.CaptchaResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Service
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);

    @Autowired
    private ActivityLogService activityLogService;

    @Value("${captcha.secret:smartats_captcha_secret_key_change_in_production_2026}")
    private String captchaSecret;

    private static final long EXPIRATION_MS = 1000 * 60 * 5; // 5 minutes

    private final Random random = new Random();

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(captchaSecret.getBytes());
    }

    public CaptchaResponse generateCaptcha() {
        int a = random.nextInt(10) + 1; // 1 to 10
        int b = random.nextInt(10) + 1; // 1 to 10
        
        int answer;
        String question;
        
        if (random.nextBoolean()) {
            answer = a + b;
            question = a + " + " + b + " = ?";
        } else {
            if (a < b) {
                int temp = a;
                a = b;
                b = temp;
            }
            answer = a - b;
            question = a + " - " + b + " = ?";
        }

        String captchaId = UUID.randomUUID().toString().substring(0, 8);

        String token = Jwts.builder()
                .setId(captchaId)
                .setSubject("captcha")
                .claim("answer", String.valueOf(answer))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        logger.info("CAPTCHA generated: {}", captchaId);
        return new CaptchaResponse(token, question);
    }

    public void validateCaptcha(String token, String providedAnswer) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA token is missing.");
        }
        if (providedAnswer == null || providedAnswer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA answer is missing.");
        }

        String captchaId = "unknown";
        try {
            // Attempt to read ID without validating signature first just for logging, if possible.
            // But since we want to validate, let's just parse it.
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            captchaId = claims.getId() != null ? claims.getId() : "unknown";
            logger.info("CAPTCHA validation requested: {}", captchaId);

            String expectedAnswer = claims.get("answer", String.class);
            if (!providedAnswer.trim().equals(expectedAnswer)) {
                logger.warn("CAPTCHA validation failed: {} (incorrect answer provided)", captchaId);
                activityLogService.logFailure("Anonymous", "UNKNOWN", "CAPTCHA_VERIFICATION", "Incorrect CAPTCHA answer provided", null);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect CAPTCHA answer.");
            }
            logger.info("CAPTCHA success: {}", captchaId);
        } catch (ResponseStatusException e) {
            throw e; // rethrow our own exception
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            captchaId = e.getClaims() != null ? e.getClaims().getId() : "unknown";
            logger.warn("CAPTCHA expired: {}", captchaId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA expired. Please refresh.");
        } catch (Exception e) {
            logger.warn("CAPTCHA failed due to invalid token", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to validate CAPTCHA.");
        }
    }
}
