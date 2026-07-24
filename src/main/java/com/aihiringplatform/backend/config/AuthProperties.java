package com.aihiringplatform.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private static final Logger logger = LoggerFactory.getLogger(AuthProperties.class);

    private boolean emailVerificationEnabled = false;

    @PostConstruct
    public void logEmailVerificationMode() {
        logger.info("Email verification: {}", emailVerificationEnabled ? "ENABLED" : "DISABLED");
    }

    public boolean isEmailVerificationEnabled() {
        return emailVerificationEnabled;
    }

    public void setEmailVerificationEnabled(boolean emailVerificationEnabled) {
        this.emailVerificationEnabled = emailVerificationEnabled;
    }
}
