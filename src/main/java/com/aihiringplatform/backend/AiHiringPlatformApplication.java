package com.aihiringplatform.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiHiringPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHiringPlatformApplication.class, args);
    }

}