package com.aihiringplatform.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiHiringPlatformApplication {

    @PostConstruct
    public void init() {
        // Force JVM to UTC to match Render and prevent timezone mismatches in DB timestamps (like OTP cooldowns)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AiHiringPlatformApplication.class, args);
    }

}