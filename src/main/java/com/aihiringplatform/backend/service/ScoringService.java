package com.aihiringplatform.backend.service;

import org.springframework.stereotype.Service;

@Service
public class ScoringService {
    public double calculateScore(int matchScore, int interviewPerformance) {
        return (matchScore * 0.6) + (interviewPerformance * 0.4);
    }
}
