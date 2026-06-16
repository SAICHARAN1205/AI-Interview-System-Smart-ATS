package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.JobMatchRequest;
import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import com.aihiringplatform.backend.entity.AtsAnalysisSnapshot;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.JobMatchSnapshot;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.AtsAnalysisSnapshotRepository;
import com.aihiringplatform.backend.repository.JobMatchSnapshotRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalyticsSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsSnapshotService.class);

    private final AtsAnalysisSnapshotRepository atsAnalysisSnapshotRepository;
    private final JobMatchSnapshotRepository jobMatchSnapshotRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final org.springframework.context.ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyticsSnapshotService(
            AtsAnalysisSnapshotRepository atsAnalysisSnapshotRepository,
            JobMatchSnapshotRepository jobMatchSnapshotRepository,
            UserRepository userRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            org.springframework.context.ApplicationContext applicationContext
    ) {
        this.atsAnalysisSnapshotRepository = atsAnalysisSnapshotRepository;
        this.jobMatchSnapshotRepository = jobMatchSnapshotRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.applicationContext = applicationContext;
    }

    public void recordAtsAnalysis(String candidateEmail, ResumeAnalysisRequest request, ResumeAnalysisResponse response) {
        User candidate = userRepository.findByEmail(candidateEmail).orElse(null);
        if (candidate == null || response == null) {
            logger.warn("ATS analysis recording skipped: candidate={} responseNull={}", candidateEmail, response == null);
            return;
        }

        logger.info("ATS analysis started: recording snapshot for candidate {}", candidateEmail);

        try {
            AtsAnalysisSnapshot snapshot = new AtsAnalysisSnapshot();
            Resume latestResume = resumeRepository.findTopByUserOrderByUploadedAtDesc(candidate).orElse(null);

            List<String> matchedKeywords = sanitizeList(response.getMatchedKeywords());
            List<String> missingKeywords = sanitizeList(response.getMissingKeywords());
            int atsScore = safeInt(response.getAtsScore(), 0);

            snapshot.setCandidate(candidate);
            snapshot.setSourceFileName(latestResume == null ? "resume" : latestResume.getFileName());
            snapshot.setTargetRole(trim(request == null ? null : request.getTargetRole(), "Software Engineer"));
            snapshot.setAtsScore(atsScore);
            snapshot.setKeywordCoverageScore(keywordCoverageScore(matchedKeywords, missingKeywords, atsScore));
            snapshot.setFormattingScore(narrativeScore(response.getFormattingQuality(), atsScore));
            snapshot.setProjectScore(narrativeScore(response.getProjectQuality(), Math.max(atsScore - 5, 40)));
            snapshot.setAtsCompatibility(trim(response.getAtsCompatibility(), "Moderate ATS compatibility"));
            snapshot.setFormattingQuality(trim(response.getFormattingQuality(), "Formatting feedback available."));
            snapshot.setProjectQuality(trim(response.getProjectQuality(), "Project feedback available."));
            snapshot.setSummary(trim(response.getSummary(), "ATS analysis completed."));
            snapshot.setFallbackUsed(Boolean.TRUE.equals(response.getFallbackUsed()));
            snapshot.setMessage(trim(response.getMessage(), ""));
            snapshot.setMatchedKeywordsJson(writeList(matchedKeywords));
            snapshot.setMissingKeywordsJson(writeList(missingKeywords));
            snapshot.setStrengthsJson(writeList(sanitizeList(response.getStrengths())));
            snapshot.setWeaknessesJson(writeList(sanitizeList(response.getWeaknesses())));
            snapshot.setOptimizationTipsJson(writeList(sanitizeList(
                    response.getOptimizationTips() == null ? response.getOptimizationFeedback() : response.getOptimizationTips()
            )));
            snapshot.setCreatedAt(LocalDateTime.now());

            atsAnalysisSnapshotRepository.save(snapshot);
            logger.info("ATS analysis completed: snapshot saved for candidate {} (atsScore={})", candidateEmail, atsScore);
            
            // Schedule background score recalculation AFTER current transaction commits
            applicationContext.getBean(ApplicationService.class).scheduleScoreUpdateAfterCommit(candidate);
        } catch (Exception exception) {
            logger.error("ATS analysis FAILED for candidate {}: {}", candidateEmail, exception.getMessage(), exception);
        }
    }

    public void recordJobMatch(String candidateEmail, JobMatchRequest request, MatchResponse response) {
        User candidate = userRepository.findByEmail(candidateEmail).orElse(null);
        if (candidate == null || response == null) {
            return;
        }

        try {
            JobMatchSnapshot snapshot = new JobMatchSnapshot();
            Job job = request == null || request.getJobId() == null
                    ? null
                    : jobRepository.findById(request.getJobId()).orElse(null);

            snapshot.setCandidate(candidate);
            snapshot.setJob(job);
            snapshot.setTargetRole(trim(
                    request == null ? response.getTargetRole() : request.getTargetRole(),
                    trim(response.getTargetRole(), job == null ? "Software Engineer" : job.getTitle())
            ));
            snapshot.setMatchPercentage(safeInt(
                    response.getMatchPercentage() == null ? response.getScore() : response.getMatchPercentage(),
                    response.getScore()
            ));
            snapshot.setFallbackUsed(Boolean.TRUE.equals(response.getFallbackUsed()));
            snapshot.setRecruiterSummary(trim(response.getRecruiterSummary(), "Match score generated."));
            snapshot.setMatchedSkillsJson(writeList(sanitizeList(response.getMatchedSkills())));
            snapshot.setMissingSkillsJson(writeList(sanitizeList(response.getMissingSkills())));
            snapshot.setCreatedAt(LocalDateTime.now());

            jobMatchSnapshotRepository.save(snapshot);
        } catch (Exception exception) {
            logger.warn("Unable to record job-match analytics snapshot for {}", candidateEmail, exception);
        }
    }

    public List<String> readList(String json) {
        if (json == null || json.trim().isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(item -> item != null && !item.trim().isBlank())
                .map(String::trim)
                .distinct()
                .limit(12)
                .toList();
    }

    private int keywordCoverageScore(List<String> matchedKeywords, List<String> missingKeywords, int fallback) {
        int total = matchedKeywords.size() + missingKeywords.size();
        if (total == 0) {
            return Math.max(55, Math.min(85, fallback));
        }
        return (int) Math.round((matchedKeywords.size() * 100.0) / total);
    }

    private int narrativeScore(String text, int fallback) {
        String normalized = String.valueOf(text).toLowerCase();
        if (normalized.contains("high") || normalized.contains("strong") || normalized.contains("excellent")) {
            return 88;
        }
        if (normalized.contains("moderate") || normalized.contains("good") || normalized.contains("serviceable")) {
            return 72;
        }
        if (normalized.contains("low") || normalized.contains("weak") || normalized.contains("needs")) {
            return 52;
        }
        return Math.max(0, Math.min(100, fallback));
    }

    private String trim(String value, String fallback) {
        if (value == null || value.trim().isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
