package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.AnalyticsInsight;
import com.aihiringplatform.backend.dto.AnalyticsInterviewHistoryItem;
import com.aihiringplatform.backend.dto.AnalyticsJobPerformance;
import com.aihiringplatform.backend.dto.AnalyticsMetric;
import com.aihiringplatform.backend.dto.AnalyticsPoint;
import com.aihiringplatform.backend.dto.AnalyticsPoint;
import com.aihiringplatform.backend.dto.AnalyticsSlice;
import com.aihiringplatform.backend.dto.AnalyticsTopCandidate;
import com.aihiringplatform.backend.dto.AtsAnalysisHistoryItem;
import com.aihiringplatform.backend.dto.CandidateAnalyticsResponse;
import com.aihiringplatform.backend.dto.RecruiterAnalyticsResponse;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.AtsAnalysisSnapshot;
import com.aihiringplatform.backend.entity.ExperienceLevel;
import com.aihiringplatform.backend.entity.InterviewSchedule;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.JobMatchSnapshot;
import com.aihiringplatform.backend.entity.MockInterviewSession;
import com.aihiringplatform.backend.entity.MockInterviewStatus;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.entity.WorkMode;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.AtsAnalysisSnapshotRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import com.aihiringplatform.backend.repository.JobMatchSnapshotRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.MockInterviewSessionRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final ResumeRepository resumeRepository;
    private final MockInterviewSessionRepository mockInterviewSessionRepository;
    private final AtsAnalysisSnapshotRepository atsAnalysisSnapshotRepository;
    private final JobMatchSnapshotRepository jobMatchSnapshotRepository;
    private final AnalyticsSnapshotService analyticsSnapshotService;

    public AnalyticsService(
            UserRepository userRepository,
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            InterviewScheduleRepository interviewScheduleRepository,
            ResumeRepository resumeRepository,
            MockInterviewSessionRepository mockInterviewSessionRepository,
            AtsAnalysisSnapshotRepository atsAnalysisSnapshotRepository,
            JobMatchSnapshotRepository jobMatchSnapshotRepository,
            AnalyticsSnapshotService analyticsSnapshotService
    ) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.interviewScheduleRepository = interviewScheduleRepository;
        this.resumeRepository = resumeRepository;
        this.mockInterviewSessionRepository = mockInterviewSessionRepository;
        this.atsAnalysisSnapshotRepository = atsAnalysisSnapshotRepository;
        this.jobMatchSnapshotRepository = jobMatchSnapshotRepository;
        this.analyticsSnapshotService = analyticsSnapshotService;
    }

    @Transactional(readOnly = true)
    public RecruiterAnalyticsResponse getRecruiterAnalytics(
            String recruiterEmail, 
            boolean adminPreviewVisible,
            String startDate,
            String endDate,
            String jobRole,
            String workMode,
            String experienceLevel
    ) {
        User recruiter = getUser(recruiterEmail, Role.RECRUITER);
        List<Job> allJobs = jobRepository.findAllByRecruiterOrderByCreatedAtDesc(recruiter);
        List<Application> allApplications = applicationRepository.findAllByJobRecruiterOrderByAppliedAtDesc(recruiter);
        List<InterviewSchedule> allSchedules = interviewScheduleRepository.findAllByRecruiterOrderByScheduledAtAsc(recruiter);

        // Apply Filters
        LocalDate start = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate) : null;

        List<Job> jobs = allJobs.stream()
                .filter(job -> start == null || !job.getCreatedAt().toLocalDate().isBefore(start))
                .filter(job -> end == null || !job.getCreatedAt().toLocalDate().isAfter(end))
                .filter(job -> jobRole == null || jobRole.isBlank() || job.getTitle().toLowerCase().contains(jobRole.toLowerCase()) || job.getId().toString().equals(jobRole))
                .filter(job -> workMode == null || workMode.isBlank() || (job.getWorkMode() != null && job.getWorkMode().name().equalsIgnoreCase(workMode)))
                .filter(job -> experienceLevel == null || experienceLevel.isBlank() || (job.getExperienceLevel() != null && job.getExperienceLevel().name().equalsIgnoreCase(experienceLevel)))
                .toList();

        Set<Long> filteredJobIds = jobs.stream().map(Job::getId).collect(Collectors.toSet());

        List<Application> applications = allApplications.stream()
                .filter(app -> app.getJob() != null && filteredJobIds.contains(app.getJob().getId()))
                .filter(app -> start == null || !app.getAppliedAt().toLocalDate().isBefore(start))
                .filter(app -> end == null || !app.getAppliedAt().toLocalDate().isAfter(end))
                .toList();

        List<InterviewSchedule> schedules = allSchedules.stream()
                .filter(schedule -> schedule.getApplication() != null && schedule.getApplication().getJob() != null && filteredJobIds.contains(schedule.getApplication().getJob().getId()))
                .filter(schedule -> start == null || !schedule.getScheduledAt().toLocalDate().isBefore(start))
                .filter(schedule -> end == null || !schedule.getScheduledAt().toLocalDate().isAfter(end))
                .toList();

        Map<Long, List<Application>> applicationsByJobId = applications.stream()
                .filter(application -> application.getJob() != null && application.getJob().getId() != null)
                .collect(Collectors.groupingBy(application -> application.getJob().getId()));

        Map<Long, Long> interviewsByJobId = schedules.stream()
                .filter(schedule -> schedule.getApplication() != null
                        && schedule.getApplication().getJob() != null
                        && schedule.getApplication().getJob().getId() != null)
                .collect(Collectors.groupingBy(
                        schedule -> schedule.getApplication().getJob().getId(),
                        Collectors.counting()
                ));

        int totalApplicants = applications.size();
        int shortlistedCount = countApplications(applications, Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.INTERVIEW_COMPLETED));
        int rejectedCount = countApplications(applications, Set.of(ApplicationStatus.REJECTED));
        int appliedCount = countApplications(applications, Set.of(ApplicationStatus.APPLIED, ApplicationStatus.ATS_REVIEW));
        int interviewCount = schedules.size();
        int hiringSuccessRate = totalApplicants == 0 ? 0 : (int) Math.round((interviewCount * 100.0) / totalApplicants);
        
        // Active and Expired Jobs
        LocalDate today = LocalDate.now();
        int activeJobsCount = (int) jobs.stream().filter(j -> j.getApplicationDeadline() == null || !j.getApplicationDeadline().isBefore(today)).count();
        int expiredJobsCount = jobs.size() - activeJobsCount;
        
        // Average ATS Score
        int averageAtsScore = average(applications.stream().map(Application::getAtsScore).filter(Objects::nonNull).toList());
        RecruiterAnalyticsResponse response = new RecruiterAnalyticsResponse();
        response.setOverview(List.of(
                metric("Total Jobs", String.valueOf(jobs.size()), "Recruiter-owned postings", jobs.isEmpty() ? "Create your first opening" : jobs.size() + " total role(s)", "neutral"),
                metric("Active Jobs", String.valueOf(activeJobsCount), "Roles open for application", activeJobsCount + " active", "primary"),
                metric("Total Applicants", String.valueOf(totalApplicants), "Candidates across your roles", totalApplicants == 0 ? "Waiting for first applicants" : totalApplicants + " total candidate(s)", "neutral"),
                metric("Average ATS Score", scoreLabel(averageAtsScore), "Overall quality of applicants", "Target >70 for best quality", averageAtsScore >= 70 ? "success" : "warning"),
                metric("Shortlisted", String.valueOf(shortlistedCount), "Candidates moved forward", percentLabel(shortlistedCount, totalApplicants), "success"),
                metric("Interviews Scheduled", String.valueOf(interviewCount), "Booked recruiter interviews", percentLabel(interviewCount, totalApplicants), "primary")
        ));
        response.setApplicationsOverTime(buildRecruiterApplicationSeries(applications));
        response.setApplicantGrowth(buildCumulativeSeries(response.getApplicationsOverTime()));
        response.setShortlistBreakdown(buildRateSlices("Shortlisted", shortlistedCount, "Not shortlisted yet", totalApplicants - shortlistedCount, "success", "neutral"));
        response.setRejectionBreakdown(buildRateSlices("Rejected", rejectedCount, "Still active", totalApplicants - rejectedCount, "danger", "primary"));
        
        // Enterprise Charts
        response.setStatusDistribution(List.of(
                slice("Applied", appliedCount, Math.round((appliedCount * 100.0f) / totalApplicants), "neutral"),
                slice("Shortlisted", shortlistedCount, Math.round((shortlistedCount * 100.0f) / totalApplicants), "primary"),
                slice("Interview Scheduled", interviewCount, Math.round((interviewCount * 100.0f) / totalApplicants), "success"),
                slice("Rejected", rejectedCount, Math.round((rejectedCount * 100.0f) / totalApplicants), "danger")
        ));
        
        response.setAtsScoreDistribution(buildAtsScoreDistribution(applications));
        response.setApplicationsPerJob(buildApplicationsPerJob(jobs, applicationsByJobId));
        response.setHiringFunnel(buildHiringFunnel(appliedCount, shortlistedCount, interviewCount, countApplications(applications, Set.of(ApplicationStatus.SELECTED))));
        
        response.setTopCandidates(applications.stream()
                .filter(app -> app.getAtsScore() != null)
                .sorted(Comparator.comparing(Application::getAtsScore).reversed())
                .limit(10)
                .map(app -> new AnalyticsTopCandidate(
                        app.getCandidate().getName(),
                        app.getCandidate().getEmail(),
                        app.getJob() != null ? app.getJob().getTitle() : "Unknown",
                        app.getStatus() != null ? app.getStatus().name() : "APPLIED",
                        app.getAtsScore(),
                        app.getMatchScore(),
                        app.getAppliedAt() != null ? app.getAppliedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A"
                ))
                .toList());

        List<AnalyticsJobPerformance> jobPerformance = jobs.stream()
                .map(job -> buildJobPerformance(job, applicationsByJobId.getOrDefault(job.getId(), List.of()), interviewsByJobId.getOrDefault(job.getId(), 0L)))
                .toList();

        response.setTopPerformingJobs(jobPerformance.stream()
                .sorted(Comparator.comparing(AnalyticsJobPerformance::getQualityScore, Comparator.nullsLast(Integer::compareTo)).reversed()
                        .thenComparing(AnalyticsJobPerformance::getApplicants, Comparator.nullsLast(Integer::compareTo)).reversed())
                .limit(5)
                .toList());
        response.setHardestJobsToFill(jobPerformance.stream()
                .sorted(Comparator.comparing(AnalyticsJobPerformance::getSuccessRate, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AnalyticsJobPerformance::getApplicants, Comparator.nullsLast(Integer::compareTo)))
                .limit(5)
                .toList());
        response.setMostSuccessfulPostings(jobPerformance.stream()
                .sorted(Comparator.comparing(AnalyticsJobPerformance::getSuccessRate, Comparator.nullsLast(Integer::compareTo)).reversed()
                        .thenComparing(AnalyticsJobPerformance::getApplicants, Comparator.nullsLast(Integer::compareTo)).reversed())
                .limit(5)
                .toList());
        response.setCandidateQualityDistribution(buildCandidateQualityDistribution(applications, schedules));
        response.setAiInsights(buildRecruiterInsights(jobs, applications, jobPerformance));
        response.setAdminPreviewVisible(adminPreviewVisible);
        response.setAdminPreviewMetrics(adminPreviewVisible ? buildAdminPreviewMetrics() : List.of());
        response.setEmptyStateMessage(jobs.isEmpty()
                ? "Create a role to unlock recruiter analytics."
                : "Recruiter analytics are ready.");
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public CandidateAnalyticsResponse getCandidateAnalytics(String candidateEmail) {
        User candidate = getUser(candidateEmail, Role.CANDIDATE);
        List<Application> applications = applicationRepository.findAllByCandidateOrderByAppliedAtDesc(candidate);
        List<AtsAnalysisSnapshot> atsSnapshots = atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtAsc(candidate);
        List<JobMatchSnapshot> matchSnapshots = jobMatchSnapshotRepository.findAllByCandidateOrderByCreatedAtAsc(candidate);
        List<MockInterviewSession> allSessions = mockInterviewSessionRepository.findAllByCandidateOrderByCompletedAtAsc(candidate);
        List<MockInterviewSession> completedSessions = allSessions.stream()
                .filter(session -> session.getStatus() == MockInterviewStatus.COMPLETED && session.getCompletedAt() != null)
                .toList();

        CandidateAnalyticsResponse response = new CandidateAnalyticsResponse();

        int activeApplications = countApplications(applications, Set.of(
                ApplicationStatus.APPLIED,
                ApplicationStatus.ATS_REVIEW,
                ApplicationStatus.SHORTLISTED,
                ApplicationStatus.INTERVIEW_SCHEDULED,
                ApplicationStatus.INTERVIEW_COMPLETED
        ));
        int averageAts = average(atsSnapshots.stream().map(AtsAnalysisSnapshot::getAtsScore).filter(Objects::nonNull).toList());
        int averageInterview = average(completedSessions.stream().map(MockInterviewSession::getOverallScore).filter(Objects::nonNull).toList());
        int bestMatch = matchSnapshots.stream()
                .map(JobMatchSnapshot::getMatchPercentage)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        int atsDelta = computeDelta(
                atsSnapshots.stream().map(AtsAnalysisSnapshot::getAtsScore).filter(Objects::nonNull).toList()
        );

        response.setOverview(List.of(
                metric("Applications", String.valueOf(applications.size()), "Roles you have applied to", activeApplications + " active pipeline item(s)", "neutral"),
                metric("Active Pipeline", String.valueOf(activeApplications), "Applications still in motion", activeApplications == 0 ? "Apply to more roles" : "Keep momentum going", activeApplications > 0 ? "success" : "warning"),
                metric("Average ATS Score", scoreLabel(averageAts), "Resume optimization trend", deltaLabel(atsDelta), atsDelta >= 0 ? "success" : "warning"),
                metric("Average Interview Score", scoreLabel(averageInterview), "Completed AI interview sessions", completedSessions.isEmpty() ? "No completed sessions yet" : completedSessions.size() + " completed session(s)", averageInterview >= 70 ? "success" : "warning"),
                metric("Best Match Score", scoreLabel(bestMatch), "Strongest recent job fit", bestMatch == 0 ? "Run match scoring to build this trend" : "Top alignment across tracked roles", bestMatch >= 75 ? "success" : "primary")
        ));
        response.setAtsScoreHistory(buildAtsScoreHistory(atsSnapshots));
        response.setInterviewScoreHistory(buildInterviewScoreHistory(completedSessions));
        response.setApplicationStatuses(buildApplicationStatusSlices(applications));
        response.setSkillMatchTrends(buildSkillMatchTrends(matchSnapshots));
        response.setCommunicationTrends(buildInterviewMetricHistory(completedSessions, "communication"));
        response.setTechnicalTrends(buildInterviewMetricHistory(completedSessions, "technical"));
        response.setRadarMetrics(buildRadarMetrics(averageAts, averageInterview, completedSessions, matchSnapshots));
        response.setAtsBreakdown(buildAtsBreakdown(atsSnapshots));
        response.setAtsHeatmap(buildAtsHeatmap(atsSnapshots));
        response.setAtsHistory(atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)
                .stream()
                .limit(10)
                .map(this::toAtsHistoryItem)
                .toList());
        response.setRecentInterviews(completedSessions.stream()
                .sorted(Comparator.comparing(MockInterviewSession::getCompletedAt).reversed())
                .limit(6)
                .map(this::toInterviewHistoryItem)
                .toList());
        response.setImprovementInsights(buildCandidateInsights(applications, atsSnapshots, matchSnapshots, completedSessions));
        response.setEmptyStateMessage(applications.isEmpty() && atsSnapshots.isEmpty() && completedSessions.isEmpty()
                ? "Upload your resume, run ATS analysis, and complete an interview to unlock full candidate insights."
                : "Candidate analytics are ready.");
        response.setGeneratedAt(LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public String exportRecruiterAnalyticsCsv(
            String recruiterEmail,
            String startDate,
            String endDate,
            String jobRole,
            String workMode,
            String experienceLevel
    ) {
        RecruiterAnalyticsResponse analytics = getRecruiterAnalytics(
                recruiterEmail, 
                false,
                startDate,
                endDate,
                jobRole,
                workMode,
                experienceLevel
        );
        StringBuilder csv = new StringBuilder();
        csv.append("section,label,value,note\n");

        for (AnalyticsMetric metric : analytics.getOverview()) {
            csv.append(csvRow("overview", metric.getLabel(), metric.getValue(), metric.getNote()));
        }

        for (AnalyticsPoint point : analytics.getApplicationsOverTime()) {
            csv.append(csvRow("applications_over_time", point.getLabel(), String.valueOf(point.getValue()), point.getMeta()));
        }

        for (AnalyticsJobPerformance item : analytics.getTopPerformingJobs()) {
            csv.append(csvRow("top_jobs", item.getJobTitle(), String.valueOf(item.getApplicants()), "successRate=" + item.getSuccessRate()));
        }

        for (AnalyticsInsight insight : analytics.getAiInsights()) {
            csv.append(csvRow("insights", insight.getTitle(), insight.getTone(), insight.getDescription()));
        }

        return csv.toString();
    }

    private User getUser(String email, Role expectedRole) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));

        if (expectedRole != null && user.getRole() != expectedRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to these analytics.");
        }

        return user;
    }

    private List<AnalyticsPoint> buildRecruiterApplicationSeries(List<Application> applications) {
        LocalDate startWeek = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(7);
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();

        for (int index = 0; index < 8; index += 1) {
            counts.put(startWeek.plusWeeks(index), 0);
        }

        for (Application application : applications) {
            if (application.getAppliedAt() == null) {
                continue;
            }

            LocalDate bucket = application.getAppliedAt().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (counts.containsKey(bucket)) {
                counts.put(bucket, counts.get(bucket) + 1);
            }
        }

        return counts.entrySet().stream()
                .map(entry -> point(entry.getKey().format(SHORT_DATE), entry.getValue(), null, "Applications"))
                .toList();
    }

    private List<AnalyticsPoint> buildCumulativeSeries(List<AnalyticsPoint> source) {
        List<AnalyticsPoint> points = new ArrayList<>();
        int runningTotal = 0;
        for (AnalyticsPoint item : source) {
            runningTotal += safe(item.getValue());
            points.add(point(item.getLabel(), runningTotal, item.getValue(), "Cumulative applicants"));
        }
        return points;
    }

    private AnalyticsJobPerformance buildJobPerformance(Job job, List<Application> applications, long interviews) {
        int shortlisted = countApplications(applications, Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.INTERVIEW_COMPLETED));
        int rejected = countApplications(applications, Set.of(ApplicationStatus.REJECTED));
        int successRate = applications.isEmpty() ? 0 : (int) Math.round((interviews * 100.0) / applications.size());
        int qualityScore = successRate + Math.min(35, applications.size() * 5) + Math.min(20, shortlisted * 4) - Math.min(20, rejected * 2);

        AnalyticsJobPerformance item = new AnalyticsJobPerformance();
        item.setJobId(job.getId());
        item.setJobTitle(job.getTitle());
        item.setApplicants(applications.size());
        item.setShortlisted(shortlisted);
        item.setRejected(rejected);
        item.setInterviews((int) interviews);
        item.setSuccessRate(successRate);
        item.setQualityScore(Math.max(0, Math.min(100, qualityScore)));
        return item;
    }

    private List<AnalyticsSlice> buildCandidateQualityDistribution(List<Application> applications, List<InterviewSchedule> schedules) {
        Map<Long, LocalDateTime> scheduledApplications = schedules.stream()
                .filter(schedule -> schedule.getApplication() != null)
                .collect(Collectors.toMap(
                        schedule -> schedule.getApplication().getId(),
                        InterviewSchedule::getScheduledAt,
                        (left, right) -> right
                ));

        int high = 0;
        int medium = 0;
        int low = 0;

        for (Application application : applications) {
            if (application.getStatus() == ApplicationStatus.REJECTED) {
                low += 1;
            } else if (application.getStatus() == ApplicationStatus.SHORTLISTED
                    || application.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED
                    || application.getStatus() == ApplicationStatus.INTERVIEW_COMPLETED
                    || scheduledApplications.containsKey(application.getId())) {
                high += 1;
            } else {
                medium += 1;
            }
        }

        int total = applications.size();
        return List.of(
                slice("High Fit", high, percentage(high, total), "success"),
                slice("Developing", medium, percentage(medium, total), "warning"),
                slice("Low Fit", low, percentage(low, total), "danger")
        );
    }

    private List<AnalyticsInsight> buildRecruiterInsights(List<Job> jobs, List<Application> applications, List<AnalyticsJobPerformance> jobPerformance) {
        List<AnalyticsInsight> insights = new ArrayList<>();

        AnalyticsJobPerformance strongest = jobPerformance.stream()
                .max(Comparator.comparing(AnalyticsJobPerformance::getQualityScore, Comparator.nullsLast(Integer::compareTo)))
                .orElse(null);
        if (strongest != null) {
            insights.add(insight(
                    "Strongest Candidate Pipeline",
                    strongest.getJobTitle() + " is leading with " + strongest.getApplicants() + " applicants and a " + strongest.getSuccessRate() + "% interview conversion rate.",
                    strongest.getSuccessRate() >= 35 ? "success" : "primary"
            ));
        }

        AnalyticsJobPerformance weakest = jobPerformance.stream()
                .filter(item -> safe(item.getApplicants()) > 0)
                .min(Comparator.comparing(AnalyticsJobPerformance::getSuccessRate, Comparator.nullsLast(Integer::compareTo)))
                .orElse(null);
        if (weakest != null) {
            insights.add(insight(
                    "Weak Job Posting",
                    weakest.getJobTitle() + " is underperforming with " + weakest.getApplicants() + " applicants and only a " + weakest.getSuccessRate() + "% conversion rate. Tightening the description and required skills could help.",
                    "warning"
            ));
        }

        Set<Long> candidateIds = applications.stream()
                .map(application -> application.getCandidate() == null ? null : application.getCandidate().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long uploadedResumeCount = candidateIds.stream().filter(resumeRepository::existsByUserId).count();
        int coverage = candidateIds.isEmpty() ? 0 : (int) Math.round((uploadedResumeCount * 100.0) / candidateIds.size());
        insights.add(insight(
                "ATS Trend",
                coverage + "% of current applicants have uploaded resumes, which is the strongest signal for running matching and ATS workflows.",
                coverage >= 70 ? "success" : "warning"
        ));

        if (insights.isEmpty() && jobs.isEmpty()) {
            insights.add(insight(
                    "Pipeline Ready",
                    "Analytics insights will deepen as soon as recruiter jobs and applicants start flowing into SmartATS.",
                    "neutral"
            ));
        }

        return insights;
    }

    private List<AnalyticsMetric> buildAdminPreviewMetrics() {
        long users = userRepository.count();
        long completedInterviews = mockInterviewSessionRepository.count();
        long atsAnalyses = atsAnalysisSnapshotRepository.count();
        long resumes = resumeRepository.count();

        return List.of(
                metric("Total Users", String.valueOf(users), "Platform-wide accounts", "Hidden until admin role exists", "neutral"),
                metric("Total Interviews", String.valueOf(completedInterviews), "Interview sessions created", "Hidden until admin role exists", "primary"),
                metric("ATS Analyses", String.valueOf(atsAnalyses), "Saved ATS snapshot count", "Hidden until admin role exists", "success"),
                metric("Resume Uploads", String.valueOf(resumes), "Stored candidate resumes", "Hidden until admin role exists", "warning")
        );
    }

    private List<AnalyticsPoint> buildAtsScoreHistory(List<AtsAnalysisSnapshot> snapshots) {
        return snapshots.stream()
                .limit(10)
                .map(snapshot -> point(
                        formatDate(snapshot.getCreatedAt()),
                        safe(snapshot.getAtsScore()),
                        snapshot.getKeywordCoverageScore(),
                        trim(snapshot.getTargetRole(), "ATS analysis")
                ))
                .toList();
    }

    private List<AnalyticsPoint> buildInterviewScoreHistory(List<MockInterviewSession> sessions) {
        return sessions.stream()
                .limit(10)
                .map(session -> point(
                        formatDate(session.getCompletedAt()),
                        safe(session.getOverallScore()),
                        session.getTechnicalScore(),
                        trim(session.getJobRole(), "Interview")
                ))
                .toList();
    }

    private List<AnalyticsSlice> buildApplicationStatusSlices(List<Application> applications) {
        Map<ApplicationStatus, Integer> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0);
        }

        for (Application application : applications) {
            ApplicationStatus status = application.getStatus() == null ? ApplicationStatus.APPLIED : application.getStatus();
            counts.put(status, counts.get(status) + 1);
        }

        int total = applications.size();
        return List.of(
                slice("Applied", counts.get(ApplicationStatus.APPLIED), percentage(counts.get(ApplicationStatus.APPLIED), total), "neutral"),
                slice("ATS Review", counts.get(ApplicationStatus.ATS_REVIEW), percentage(counts.get(ApplicationStatus.ATS_REVIEW), total), "warning"),
                slice("Shortlisted", counts.get(ApplicationStatus.SHORTLISTED), percentage(counts.get(ApplicationStatus.SHORTLISTED), total), "success"),
                slice("Interview", counts.get(ApplicationStatus.INTERVIEW_SCHEDULED) + counts.get(ApplicationStatus.INTERVIEW_COMPLETED), percentage(counts.get(ApplicationStatus.INTERVIEW_SCHEDULED) + counts.get(ApplicationStatus.INTERVIEW_COMPLETED), total), "primary"),
                slice("Rejected", counts.get(ApplicationStatus.REJECTED), percentage(counts.get(ApplicationStatus.REJECTED), total), "danger")
        );
    }

    private List<AnalyticsPoint> buildSkillMatchTrends(List<JobMatchSnapshot> matchSnapshots) {
        return matchSnapshots.stream()
                .limit(8)
                .map(snapshot -> point(
                        trim(snapshot.getTargetRole(), snapshot.getJob() == null ? "Tracked role" : snapshot.getJob().getTitle()),
                        safe(snapshot.getMatchPercentage()),
                        null,
                        formatDate(snapshot.getCreatedAt())
                ))
                .toList();
    }

    private List<AnalyticsPoint> buildInterviewMetricHistory(List<MockInterviewSession> sessions, String metric) {
        return sessions.stream()
                .limit(10)
                .map(session -> {
                    int value = "communication".equals(metric)
                            ? safe(session.getCommunicationScore())
                            : safe(session.getTechnicalScore());
                    return point(formatDate(session.getCompletedAt()), value, session.getOverallScore(), trim(session.getJobRole(), "Interview"));
                })
                .toList();
    }

    private List<AnalyticsPoint> buildRadarMetrics(int averageAts, int averageInterview, List<MockInterviewSession> sessions, List<JobMatchSnapshot> matchSnapshots) {
        int avgCommunication = average(sessions.stream().map(MockInterviewSession::getCommunicationScore).filter(Objects::nonNull).toList());
        int avgTechnical = average(sessions.stream().map(MockInterviewSession::getTechnicalScore).filter(Objects::nonNull).toList());
        int avgMatch = average(matchSnapshots.stream().map(JobMatchSnapshot::getMatchPercentage).filter(Objects::nonNull).toList());

        return List.of(
                point("ATS Readiness", averageAts, null, "resume"),
                point("Interview Overall", averageInterview, null, "interview"),
                point("Communication", avgCommunication, null, "interview"),
                point("Technical", avgTechnical, null, "interview"),
                point("Role Match", avgMatch, null, "jobs")
        );
    }

    private List<AnalyticsPoint> buildAtsBreakdown(List<AtsAnalysisSnapshot> snapshots) {
        AtsAnalysisSnapshot latest = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        if (latest == null) {
            return List.of();
        }

        return List.of(
                point("ATS Score", safe(latest.getAtsScore()), null, "Overall compatibility"),
                point("Keyword Coverage", safe(latest.getKeywordCoverageScore()), null, "Tracked keywords"),
                point("Formatting", safe(latest.getFormattingScore()), null, "Parser readability"),
                point("Projects", safe(latest.getProjectScore()), null, "Project signal")
        );
    }

    private List<AnalyticsPoint> buildAtsHeatmap(List<AtsAnalysisSnapshot> snapshots) {
        AtsAnalysisSnapshot latest = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        if (latest == null) {
            return List.of();
        }

        return List.of(
                point("Resume / Keywords", safe(latest.getKeywordCoverageScore()), null, "Keywords"),
                point("Resume / Formatting", safe(latest.getFormattingScore()), null, "Formatting"),
                point("Resume / Projects", safe(latest.getProjectScore()), null, "Projects"),
                point("Resume / ATS", safe(latest.getAtsScore()), null, "Overall")
        );
    }

    private AtsAnalysisHistoryItem toAtsHistoryItem(AtsAnalysisSnapshot snapshot) {
        AtsAnalysisHistoryItem item = new AtsAnalysisHistoryItem();
        item.setId(snapshot.getId());
        item.setSourceFileName(trim(snapshot.getSourceFileName(), "resume"));
        item.setTargetRole(trim(snapshot.getTargetRole(), "Software Engineer"));
        item.setAtsScore(snapshot.getAtsScore());
        item.setKeywordCoverageScore(snapshot.getKeywordCoverageScore());
        item.setFormattingScore(snapshot.getFormattingScore());
        item.setProjectScore(snapshot.getProjectScore());
        item.setAtsCompatibility(trim(snapshot.getAtsCompatibility(), "Moderate ATS compatibility"));
        item.setFormattingQuality(trim(snapshot.getFormattingQuality(), "Formatting feedback available."));
        item.setProjectQuality(trim(snapshot.getProjectQuality(), "Project feedback available."));
        item.setSummary(trim(snapshot.getSummary(), "ATS analysis completed."));
        item.setFallbackUsed(Boolean.TRUE.equals(snapshot.getFallbackUsed()));
        item.setMessage(trim(snapshot.getMessage(), ""));
        item.setMatchedKeywords(analyticsSnapshotService.readList(snapshot.getMatchedKeywordsJson()));
        item.setMissingKeywords(analyticsSnapshotService.readList(snapshot.getMissingKeywordsJson()));
        item.setStrengths(analyticsSnapshotService.readList(snapshot.getStrengthsJson()));
        item.setWeaknesses(analyticsSnapshotService.readList(snapshot.getWeaknessesJson()));
        item.setOptimizationTips(analyticsSnapshotService.readList(snapshot.getOptimizationTipsJson()));
        item.setCreatedAt(snapshot.getCreatedAt());
        return item;
    }

    private AnalyticsInterviewHistoryItem toInterviewHistoryItem(MockInterviewSession session) {
        AnalyticsInterviewHistoryItem item = new AnalyticsInterviewHistoryItem();
        item.setSessionId(session.getId());
        item.setJobRole(trim(session.getJobRole(), "Interview session"));
        item.setOverallScore(session.getOverallScore());
        item.setCommunicationScore(session.getCommunicationScore());
        item.setTechnicalScore(session.getTechnicalScore());
        item.setSummary(trim(session.getResultSummary(), "Interview completed."));
        item.setCompletedAt(session.getCompletedAt());
        return item;
    }

    private List<AnalyticsInsight> buildCandidateInsights(
            List<Application> applications,
            List<AtsAnalysisSnapshot> atsSnapshots,
            List<JobMatchSnapshot> matchSnapshots,
            List<MockInterviewSession> completedSessions
    ) {
        List<AnalyticsInsight> insights = new ArrayList<>();

        if (!atsSnapshots.isEmpty()) {
            AtsAnalysisSnapshot first = atsSnapshots.get(0);
            AtsAnalysisSnapshot latest = atsSnapshots.get(atsSnapshots.size() - 1);
            int atsDelta = safe(latest.getAtsScore()) - safe(first.getAtsScore());
            insights.add(insight(
                    "Resume Improvement",
                    atsDelta >= 0
                            ? "Your ATS score has improved by " + atsDelta + " point(s) across tracked resume analyses."
                            : "Your latest ATS score is " + Math.abs(atsDelta) + " point(s) below your earliest tracked run, which suggests recent edits need another pass.",
                    atsDelta >= 0 ? "success" : "warning"
            ));
        }

        if (!matchSnapshots.isEmpty()) {
            JobMatchSnapshot strongest = matchSnapshots.stream()
                    .max(Comparator.comparing(JobMatchSnapshot::getMatchPercentage, Comparator.nullsLast(Integer::compareTo)))
                    .orElse(null);
            if (strongest != null) {
                insights.add(insight(
                        "Best Job Fit",
                        trim(strongest.getTargetRole(), "Tracked role") + " is your strongest tracked match at " + safe(strongest.getMatchPercentage()) + "%.",
                        safe(strongest.getMatchPercentage()) >= 75 ? "success" : "primary"
                ));
            }
        }

        if (!completedSessions.isEmpty()) {
            int communication = average(completedSessions.stream().map(MockInterviewSession::getCommunicationScore).filter(Objects::nonNull).toList());
            int technical = average(completedSessions.stream().map(MockInterviewSession::getTechnicalScore).filter(Objects::nonNull).toList());
            insights.add(insight(
                    "Interview Pattern",
                    communication >= technical
                            ? "Communication is currently stronger than technical depth. Keep adding more role-specific technical detail to answers."
                            : "Technical answers are landing well. Focus on structure and storytelling to raise communication scores.",
                    communication >= technical ? "primary" : "warning"
            ));
        }

        if (!applications.isEmpty()) {
            int rejected = countApplications(applications, Set.of(ApplicationStatus.REJECTED));
            insights.add(insight(
                    "Application Momentum",
                    rejected >= Math.max(2, applications.size() / 2)
                            ? "Application outcomes suggest your resume and targeting strategy still need tightening before broad applying."
                            : "Your application pipeline still has active opportunities. Keep tailoring by role and tracking interview feedback.",
                    rejected >= Math.max(2, applications.size() / 2) ? "warning" : "success"
            ));
        }

        if (insights.isEmpty()) {
            insights.add(insight(
                    "Insights Waiting",
                    "Run ATS analysis, match scoring, and at least one interview session to unlock richer candidate intelligence.",
                    "neutral"
            ));
        }

        return insights;
    }

    private List<AnalyticsSlice> buildRateSlices(String primaryLabel, int primaryValue, String secondaryLabel, int secondaryValue, String primaryTone, String secondaryTone) {
        int total = primaryValue + secondaryValue;
        if (total == 0) return List.of();
        return List.of(
                slice(primaryLabel, primaryValue, Math.round((primaryValue * 100.0f) / total), primaryTone),
                slice(secondaryLabel, secondaryValue, Math.round((secondaryValue * 100.0f) / total), secondaryTone)
        );
    }

    private List<AnalyticsPoint> buildAtsScoreDistribution(List<Application> applications) {
        int[] buckets = new int[5]; // 0-20, 21-40, 41-60, 61-80, 81-100
        for (Application app : applications) {
            Integer score = app.getAtsScore();
            if (score != null) {
                if (score <= 20) buckets[0]++;
                else if (score <= 40) buckets[1]++;
                else if (score <= 60) buckets[2]++;
                else if (score <= 80) buckets[3]++;
                else buckets[4]++;
            }
        }
        return List.of(
                point("0-20", buckets[0], null, "Needs Improvement"),
                point("21-40", buckets[1], null, "Below Average"),
                point("41-60", buckets[2], null, "Average"),
                point("61-80", buckets[3], null, "Good Fit"),
                point("81-100", buckets[4], null, "Excellent Match")
        );
    }

    private List<AnalyticsPoint> buildApplicationsPerJob(List<Job> jobs, Map<Long, List<Application>> applicationsByJobId) {
        return jobs.stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .limit(10) // Show last 10 jobs to prevent chart overflow
                .map(job -> point(
                        job.getTitle().length() > 20 ? job.getTitle().substring(0, 17) + "..." : job.getTitle(),
                        applicationsByJobId.getOrDefault(job.getId(), List.of()).size(),
                        null,
                        job.getCompanyName()
                ))
                .toList();
    }

    private List<AnalyticsPoint> buildHiringFunnel(int appliedCount, int shortlistedCount, int interviewCount, int hiredCount) {
        return List.of(
                point("Applied", appliedCount, null, "Total candidates"),
                point("Shortlisted", shortlistedCount, null, "Passed screening"),
                point("Interviewed", interviewCount, null, "Scheduled for interview"),
                point("Hired", hiredCount, null, "Successfully hired")
        );
    }

    private int countApplications(Collection<Application> applications, Set<ApplicationStatus> statuses) {
        return (int) applications.stream()
                .filter(application -> statuses.contains(application.getStatus()))
                .count();
    }

    private AnalyticsMetric metric(String label, String value, String note, String delta, String tone) {
        AnalyticsMetric metric = new AnalyticsMetric();
        metric.setLabel(label);
        metric.setValue(value);
        metric.setNote(note);
        metric.setDelta(delta);
        metric.setTone(tone);
        return metric;
    }

    private AnalyticsPoint point(String label, Integer value, Integer secondaryValue, String meta) {
        AnalyticsPoint point = new AnalyticsPoint();
        point.setLabel(label);
        point.setValue(value);
        point.setSecondaryValue(secondaryValue);
        point.setMeta(meta);
        return point;
    }

    private AnalyticsSlice slice(String label, Integer value, Integer percentage, String tone) {
        AnalyticsSlice slice = new AnalyticsSlice();
        slice.setLabel(label);
        slice.setValue(value);
        slice.setPercentage(percentage);
        slice.setTone(tone);
        return slice;
    }

    private AnalyticsInsight insight(String title, String description, String tone) {
        AnalyticsInsight insight = new AnalyticsInsight();
        insight.setTitle(title);
        insight.setDescription(description);
        insight.setTone(tone);
        return insight;
    }

    private int average(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }

        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private int computeDelta(List<Integer> values) {
        if (values == null || values.size() < 2) {
            return 0;
        }
        return safe(values.get(values.size() - 1)) - safe(values.get(0));
    }

    private String csvRow(String section, String label, String value, String note) {
        return escapeCsv(section) + "," + escapeCsv(label) + "," + escapeCsv(value) + "," + escapeCsv(note) + "\n";
    }

    private String escapeCsv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private int percentage(int value, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((value * 100.0) / total);
    }

    private String percentLabel(int value, int total) {
        return percentage(value, total) + "% of applicants";
    }

    private String scoreLabel(int score) {
        return score <= 0 ? "--" : score + "%";
    }

    private String deltaLabel(int delta) {
        if (delta > 0) {
            return "+" + delta + " vs first tracked run";
        }
        if (delta < 0) {
            return delta + " vs first tracked run";
        }
        return "No tracked change yet";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "Pending" : value.toLocalDate().format(SHORT_DATE);
    }

    private String trim(String value, String fallback) {
        if (value == null || value.trim().isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
