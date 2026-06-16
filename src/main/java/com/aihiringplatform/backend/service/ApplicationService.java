package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.CandidateApplicationResponse;
import com.aihiringplatform.backend.dto.RecruiterApplicantResponse;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.InterviewSchedule;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.repository.AtsAnalysisSnapshotRepository;
import com.aihiringplatform.backend.entity.AtsAnalysisSnapshot;
import com.aihiringplatform.backend.repository.ApplicationHistoryLogRepository;
import com.aihiringplatform.backend.entity.ApplicationHistoryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AtsAnalysisSnapshotRepository atsAnalysisSnapshotRepository;

    @Autowired
    private ApplicationHistoryLogRepository applicationHistoryLogRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Transactional
    public CandidateApplicationResponse applyToJob(Long jobId, String candidateEmail) {
        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate account was not found."));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only candidates can apply for jobs.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));

        if (applicationRepository.existsByCandidateAndJob(candidate, job)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job.");
        }

        long recentApps = applicationRepository.countByCandidateAndAppliedAtAfter(candidate, LocalDateTime.now().minusMinutes(5));
        if (recentApps > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You are applying too fast. Please wait a few minutes between applications.");
        }

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.APPLIED);

        // Pre-calculate initial scores if resume exists
        Resume resume = resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId()).orElse(null);
        AtsAnalysisSnapshot roleAts = atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)
                .stream()
                .filter(s -> s.getTargetRole() != null && s.getTargetRole().equalsIgnoreCase(job.getTitle()))
                .findFirst()
                .orElse(null);
        if (roleAts != null) {
            application.setAtsScore(roleAts.getAtsScore());
        }
        if (resume != null && resume.getExtractedText() != null && job.getDescription() != null) {
            try {
                int matchScore = matchingService.calculateMatchScore(
                        resume.getExtractedText(), job, new ArrayList<>()
                );
                application.setMatchScore(matchScore);
                logger.info("Pre-calculated match score for new application (job={}): {}", jobId, matchScore);
            } catch (Exception e) {
                logger.warn("Failed to pre-calculate match score for new application (job={}): {}", jobId, e.getMessage());
            }
        }

        Application saved = applicationRepository.save(application);
        
        activityLogService.logSuccess(candidateEmail, "CANDIDATE", "JOB_APPLICATION", "Candidate applied to job: " + job.getTitle() + " (ID: " + jobId + ")", null);
        
        return toCandidateApplicationResponse(saved, null);
    }

    public List<RecruiterApplicantResponse> getApplicationsForJob(Long jobId, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view applicants.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));

        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review applicants for your jobs.");
        }

        return applicationRepository.findAllByJob(job).stream()
                .map(app -> toRecruiterApplicantResponse(app))
                .toList();
    }

    public List<RecruiterApplicantResponse> getApplicationsForRecruiter(String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view applicants.");
        }

        List<RecruiterApplicantResponse> results = applicationRepository.findAllByJobRecruiterOrderByAppliedAtDesc(recruiter)
                .stream()
                .map(this::toRecruiterApplicantResponse)
                .toList();
        logger.info("ATS scores loaded: {} applications for recruiter {} (scores present: {})",
                results.size(), recruiterEmail,
                results.stream().filter(r -> r.getMatchScore() != null).count());
        return results;
    }

    public List<CandidateApplicationResponse> getApplicationsForCandidate(String candidateEmail) {
        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate account was not found."));

        if (candidate.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only candidates can view their applications.");
        }

        List<Application> applications = applicationRepository.findAllByCandidateOrderByAppliedAtDesc(candidate);
        if (applications.isEmpty()) {
            logger.info("ATS scores loaded: 0 applications for candidate {}", candidateEmail);
            return List.of();
        }

        Map<Long, InterviewSchedule> schedulesByApplicationId = interviewScheduleRepository.findAllByApplicationIn(applications)
                .stream()
                .filter(schedule -> schedule.getApplication() != null && schedule.getApplication().getId() != null)
                .collect(Collectors.toMap(
                        schedule -> schedule.getApplication().getId(),
                        Function.identity()
                ));

        List<CandidateApplicationResponse> results = applications.stream()
                .map(application -> toCandidateApplicationResponse(
                        application,
                        schedulesByApplicationId.get(application.getId())
                ))
                .toList();
        logger.info("ATS scores loaded: {} applications for candidate {} (scores present: {})",
                results.size(), candidateEmail,
                results.stream().filter(r -> r.getMatchScore() != null).count());
        return results;
    }

    public CandidateApplicationResponse updateApplicationStatus(
            Long applicationId,
            String recruiterEmail,
            ApplicationStatus requestedStatus,
            String rejectionFeedback) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can update application status.");
        }

        if (requestedStatus == null || requestedStatus == ApplicationStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));

        Job job = application.getJob();
        if (job == null || job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update applicants for your jobs.");
        }

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(requestedStatus);
        application.setRejectionFeedback(
                requestedStatus == ApplicationStatus.REJECTED
                        ? normalizeOptionalText(rejectionFeedback)
                        : null
        );
        Application saved = applicationRepository.save(application);

        ApplicationHistoryLog historyLog = new ApplicationHistoryLog();
        historyLog.setApplication(saved);
        historyLog.setChangedBy(recruiter);
        historyLog.setOldStatus(oldStatus);
        historyLog.setNewStatus(requestedStatus);
        historyLog.setComments(rejectionFeedback);
        applicationHistoryLogRepository.save(historyLog);

        activityLogService.logSuccess(recruiterEmail, "RECRUITER", "APPLICATION_STATUS_UPDATE", "Recruiter changed application " + applicationId + " status to " + requestedStatus.name(), null);

        return toCandidateApplicationResponse(saved, null);
    }

    private CandidateApplicationResponse toCandidateApplicationResponse(Application application, InterviewSchedule schedule) {
        CandidateApplicationResponse response = new CandidateApplicationResponse();
        response.setApplicationId(application.getId());
        response.setAppliedAt(application.getAppliedAt());
        response.setStatus(application.getStatus() == null ? null : application.getStatus().name());

        Job job = application.getJob();
        if (job != null) {
            response.setJobId(job.getId());
            response.setJobTitle(job.getTitle());
            response.setCompanyName(job.getCompanyName());
            response.setLocation(job.getLocation());
            response.setSalary(job.getSalary());
            response.setSkills(job.getSkills());
            response.setDescription(job.getDescription());
            response.setRequirements(job.getRequirements());
        }

        response.setRejectionFeedback(application.getRejectionFeedback());

        if (schedule != null) {
            response.setInterviewScheduledAt(schedule.getScheduledAt());
        }

        response.setMatchScore(application.getMatchScore());
        response.setAtsScore(application.getAtsScore());

        return response;
    }

    private RecruiterApplicantResponse toRecruiterApplicantResponse(Application application) {
        RecruiterApplicantResponse response = new RecruiterApplicantResponse();
        response.setApplicationId(application.getId());
        response.setAppliedAt(application.getAppliedAt());
        response.setStatus(application.getStatus() == null ? null : application.getStatus().name());
        response.setRejectionFeedback(application.getRejectionFeedback());

        User candidate = application.getCandidate();
        if (candidate != null) {
            response.setCandidateId(candidate.getId());
            response.setCandidateName(candidate.getName());
            response.setCandidateEmail(candidate.getEmail());
        }

        Job job = application.getJob();
        if (job != null) {
            response.setJobId(job.getId());
            response.setJobTitle(job.getTitle());
            response.setCompanyName(job.getCompanyName());
        }

        response.setMatchScore(application.getMatchScore());
        response.setAtsScore(application.getAtsScore());

        return response;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    /**
     * Recalculates matchScore and atsScore for a single application.
     * Can be triggered by a recruiter when scores are missing.
     */
    @Transactional
    public RecruiterApplicantResponse recalculateScoreForApplication(Long applicationId, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can trigger score recalculation.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));

        Job job = application.getJob();
        if (job == null || job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only recalculate scores for your own job applicants.");
        }

        User candidate = application.getCandidate();
        if (candidate == null) {
            return toRecruiterApplicantResponse(application);
        }

        // Update ATS score from role-specific snapshot
        AtsAnalysisSnapshot roleAts = atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)
                .stream()
                .filter(s -> s.getTargetRole() != null && s.getTargetRole().equalsIgnoreCase(job.getTitle()))
                .findFirst()
                .orElse(null);
        if (roleAts != null) {
            application.setAtsScore(roleAts.getAtsScore());
        }

        // Recalculate match score from resume
        Resume resume = resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId()).orElse(null);
        if (resume != null && resume.getExtractedText() != null && job.getDescription() != null) {
            try {
                int matchScore = matchingService.calculateMatchScore(
                        resume.getExtractedText(), job, new ArrayList<>()
                );
                application.setMatchScore(matchScore);
                logger.info("Recalculated match score for application {}: {}", applicationId, matchScore);
            } catch (Exception e) {
                logger.warn("Failed to recalculate match score for application {}: {}", applicationId, e.getMessage());
                // Keep existing score on failure
            }
        }

        Application saved = applicationRepository.save(application);
        return toRecruiterApplicantResponse(saved);
    }

    /**
     * Schedules the async score update to run AFTER the current transaction commits.
     * This prevents the race condition where the @Async method reads stale data
     * because the calling transaction hasn't committed yet.
     *
     * If no transaction is active, runs the update immediately.
     */
    public void scheduleScoreUpdateAfterCommit(User candidate) {
        if (candidate == null) return;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            logger.info("Scheduling post-commit score update for candidate {}", candidate.getEmail());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logger.info("Transaction committed — triggering async score update for candidate {}", candidate.getEmail());
                    updateApplicationScoresForCandidate(candidate);
                }
            });
        } else {
            // No active transaction — run immediately
            logger.info("No active transaction — triggering immediate async score update for candidate {}", candidate.getEmail());
            updateApplicationScoresForCandidate(candidate);
        }
    }

    @Async
    @Transactional
    public void updateApplicationScoresForCandidate(User candidate) {
        if (candidate == null) return;
        try {
            List<Application> applications = applicationRepository.findAllByCandidate(candidate);
            if (applications.isEmpty()) {
                logger.info("No applications found for candidate {} — skipping score update", candidate.getEmail());
                return;
            }

            Resume resume = resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId()).orElse(null);
            List<AtsAnalysisSnapshot> allSnapshots = atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate);

            logger.info("ATS score update started for candidate {} across {} applications",
                    candidate.getEmail(), applications.size());

            int successCount = 0;
            int failureCount = 0;

            for (Application app : applications) {
                Integer roleAtsScore = null;
                if (app.getJob() != null) {
                    AtsAnalysisSnapshot roleAts = allSnapshots.stream()
                            .filter(s -> s.getTargetRole() != null && s.getTargetRole().equalsIgnoreCase(app.getJob().getTitle()))
                            .findFirst()
                            .orElse(null);
                    roleAtsScore = roleAts != null ? roleAts.getAtsScore() : null;
                }
                app.setAtsScore(roleAtsScore);
                
                if (resume != null && resume.getExtractedText() != null && app.getJob() != null) {
                    try {
                        int matchScore = matchingService.calculateMatchScore(
                                resume.getExtractedText(), app.getJob(), new ArrayList<>()
                        );
                        app.setMatchScore(matchScore);
                        logger.info("ATS match score saved: application={} job={} matchScore={} atsScore={}",
                                app.getId(), app.getJob().getId(), matchScore, roleAtsScore);
                        successCount++;
                    } catch (Exception e) {
                        logger.warn("ATS match score FAILED for application {} (job={}): {}",
                                app.getId(), app.getJob().getId(), e.getMessage());
                        failureCount++;
                        // Keep existing matchScore rather than nulling it on AI failure
                    }
                } else if (resume == null || resume.getExtractedText() == null) {
                    app.setMatchScore(null);
                    logger.debug("No resume text available for application {} — matchScore set to null", app.getId());
                }
                applicationRepository.save(app);
            }
            logger.info("ATS score update completed for candidate {}: {} succeeded, {} failed out of {} applications",
                    candidate.getEmail(), successCount, failureCount, applications.size());
        } catch (Exception e) {
            logger.error("ATS score update FAILED entirely for candidate {}: {}", candidate.getEmail(), e.getMessage(), e);
        }
    }
}
