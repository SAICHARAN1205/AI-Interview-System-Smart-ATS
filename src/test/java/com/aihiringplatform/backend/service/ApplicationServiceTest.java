package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.CandidateApplicationResponse;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.AtsAnalysisSnapshot;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.AtsAnalysisSnapshotRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ApplicationServiceTest {

    @InjectMocks
    private ApplicationService applicationService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private AtsAnalysisSnapshotRepository atsAnalysisSnapshotRepository;

    @Mock
    private MatchingService matchingService;

    @Mock
    private InterviewScheduleRepository interviewScheduleRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private com.aihiringplatform.backend.repository.ApplicationHistoryLogRepository applicationHistoryLogRepository;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void applyToJob_shouldSaveInitialMatchAndAtsScores() {
        User candidate = new User();
        candidate.setId(1L);
        candidate.setEmail("candidate@test.com");
        candidate.setRole(Role.CANDIDATE);

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Java Developer");
        job.setDescription("Java Developer role");

        Resume resume = new Resume();
        resume.setExtractedText("Experienced Java Developer");

        AtsAnalysisSnapshot snapshot = new AtsAnalysisSnapshot();
        snapshot.setTargetRole(job.getTitle());
        snapshot.setAtsScore(85);

        when(userRepository.findByEmail(candidate.getEmail())).thenReturn(Optional.of(candidate));
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(applicationRepository.existsByCandidateAndJob(candidate, job)).thenReturn(false);
        when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId())).thenReturn(Optional.of(resume));
        when(atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)).thenReturn(List.of(snapshot));
        when(matchingService.calculateMatchScore(anyString(), any(Job.class), anyList())).thenReturn(90);

        Application savedApp = new Application();
        savedApp.setId(1L);
        savedApp.setAtsScore(85);
        savedApp.setMatchScore(90);
        savedApp.setJob(job);
        
        when(applicationRepository.save(any(Application.class))).thenReturn(savedApp);

        CandidateApplicationResponse response = applicationService.applyToJob(job.getId(), candidate.getEmail());

        assertNotNull(response);
        assertEquals(85, response.getAtsScore());
        assertEquals(90, response.getMatchScore());

        verify(applicationRepository).save(argThat(app -> {
            return app.getAtsScore() != null && app.getAtsScore() == 85
                && app.getMatchScore() != null && app.getMatchScore() == 90;
        }));
    }

    @Test
    public void updateApplicationScoresForCandidate_shouldUpdateScores() {
        User candidate = new User();
        candidate.setId(1L);

        Application application = new Application();
        Job job = new Job();
        job.setTitle("Frontend Developer");
        job.setDescription("Frontend Developer");
        application.setJob(job);
        
        Resume resume = new Resume();
        resume.setExtractedText("React expert");

        AtsAnalysisSnapshot snapshot = new AtsAnalysisSnapshot();
        snapshot.setTargetRole(job.getTitle());
        snapshot.setAtsScore(77);

        when(applicationRepository.findAllByCandidate(candidate)).thenReturn(List.of(application));
        when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId())).thenReturn(Optional.of(resume));
        when(atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)).thenReturn(List.of(snapshot));
        when(matchingService.calculateMatchScore(anyString(), any(Job.class), anyList())).thenReturn(82);

        applicationService.updateApplicationScoresForCandidate(candidate);

        verify(applicationRepository).save(argThat(app -> {
            return app.getAtsScore() != null && app.getAtsScore() == 77 && app.getMatchScore() != null && app.getMatchScore() == 82;
        }));
    }

    @Test
    public void updateApplicationStatus_whenRejected_shouldReturnRejectionFeedback() {
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = new Job();
        job.setId(1L);
        job.setRecruiter(recruiter);

        Application application = new Application();
        application.setId(10L);
        application.setJob(job);
        application.setStatus(ApplicationStatus.ATS_REVIEW);

        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        CandidateApplicationResponse response = applicationService.updateApplicationStatus(
                application.getId(),
                recruiter.getEmail(),
                ApplicationStatus.REJECTED,
                "You are not fit for this role"
        );

        assertNotNull(response);
        assertEquals(ApplicationStatus.REJECTED.name(), response.getStatus());
        assertEquals("You are not fit for this role", response.getRejectionFeedback());
    }

    @Test
    public void getApplicationsForRecruiter_shouldReturnMatchAndAtsScores() {
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Java Dev");
        job.setCompanyName("TestCo");
        job.setRecruiter(recruiter);

        User candidate = new User();
        candidate.setId(2L);
        candidate.setName("Alice");
        candidate.setEmail("alice@test.com");

        Application application = new Application();
        application.setId(10L);
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setMatchScore(84);
        application.setAtsScore(72);

        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findAllByJobRecruiterOrderByAppliedAtDesc(recruiter)).thenReturn(List.of(application));

        var results = applicationService.getApplicationsForRecruiter(recruiter.getEmail());

        assertEquals(1, results.size());
        var dto = results.get(0);
        assertEquals(84, dto.getMatchScore());
        assertEquals(72, dto.getAtsScore());
        assertEquals("Alice", dto.getCandidateName());
        assertEquals(10L, dto.getApplicationId());
    }

    @Test
    public void getApplicationsForJob_shouldReturnMatchAndAtsScores() {
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = new Job();
        job.setId(5L);
        job.setTitle("Frontend Dev");
        job.setCompanyName("WebCo");
        job.setRecruiter(recruiter);

        User candidate = new User();
        candidate.setId(3L);
        candidate.setName("Bob");
        candidate.setEmail("bob@test.com");

        Application application = new Application();
        application.setId(20L);
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setMatchScore(91);
        application.setAtsScore(88);

        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(applicationRepository.findAllByJob(job)).thenReturn(List.of(application));

        var results = applicationService.getApplicationsForJob(job.getId(), recruiter.getEmail());

        assertEquals(1, results.size());
        var dto = results.get(0);
        assertEquals(91, dto.getMatchScore());
        assertEquals(88, dto.getAtsScore());
        assertEquals("Bob", dto.getCandidateName());
    }

    @Test
    public void updateApplicationScoresForCandidate_shouldPersistAtsAndMatchScores() {
        User candidate = new User();
        candidate.setId(5L);
        candidate.setEmail("test-candidate@test.com");

        Job job1 = new Job();
        job1.setId(10L);
        job1.setTitle("Senior Java Developer");
        job1.setDescription("Senior Java Developer with Spring Boot experience");

        Job job2 = new Job();
        job2.setId(11L);
        job2.setTitle("Frontend React Developer");
        job2.setDescription("Frontend React Developer");

        Application app1 = new Application();
        app1.setId(100L);
        app1.setJob(job1);

        Application app2 = new Application();
        app2.setId(101L);
        app2.setJob(job2);

        Resume resume = new Resume();
        resume.setExtractedText("Java developer with 5 years Spring Boot experience");

        AtsAnalysisSnapshot snapshot = new AtsAnalysisSnapshot();
        snapshot.setTargetRole(job1.getTitle()); // Match first job for test
        snapshot.setAtsScore(80);

        when(applicationRepository.findAllByCandidate(candidate)).thenReturn(List.of(app1, app2));
        when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId())).thenReturn(Optional.of(resume));
        when(atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)).thenReturn(List.of(snapshot));
        when(matchingService.calculateMatchScore(anyString(), any(Job.class), anyList()))
                .thenReturn(92)  // first call for job1
                .thenReturn(45); // second call for job2

        applicationService.updateApplicationScoresForCandidate(candidate);

        // Verify both applications were saved with correct scores
        verify(applicationRepository, times(2)).save(any(Application.class));

        verify(applicationRepository).save(argThat(app ->
                app.getId() == 100L && app.getAtsScore() != null && app.getAtsScore() == 80 && app.getMatchScore() != null && app.getMatchScore() == 92
        ));
        verify(applicationRepository).save(argThat(app ->
                app.getId() == 101L && app.getAtsScore() == null && app.getMatchScore() != null && app.getMatchScore() == 45
        ));
    }

    @Test
    public void updateApplicationScoresForCandidate_shouldPreserveExistingScoreOnAiFailure() {
        User candidate = new User();
        candidate.setId(6L);
        candidate.setEmail("preserve-score@test.com");

        Job job = new Job();
        job.setId(20L);
        job.setTitle("Data Scientist");
        job.setDescription("Data Scientist");

        Application application = new Application();
        application.setId(200L);
        application.setJob(job);
        application.setMatchScore(75); // existing score

        Resume resume = new Resume();
        resume.setExtractedText("Machine learning specialist");

        AtsAnalysisSnapshot snapshot = new AtsAnalysisSnapshot();
        snapshot.setTargetRole(job.getTitle());
        snapshot.setAtsScore(65);

        when(applicationRepository.findAllByCandidate(candidate)).thenReturn(List.of(application));
        when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId())).thenReturn(Optional.of(resume));
        when(atsAnalysisSnapshotRepository.findAllByCandidateOrderByCreatedAtDesc(candidate)).thenReturn(List.of(snapshot));
        when(matchingService.calculateMatchScore(anyString(), any(Job.class), anyList()))
                .thenThrow(new RuntimeException("AI service rate limited"));

        applicationService.updateApplicationScoresForCandidate(candidate);

        // Should save with ATS score updated but matchScore preserved (not nulled)
        verify(applicationRepository).save(argThat(app ->
                app.getAtsScore() != null && app.getAtsScore() == 65 && app.getMatchScore() != null && app.getMatchScore() == 75
        ));
    }

    @Test
    public void bothDashboards_shouldShowSameScoreFromApplication() {
        // This test verifies the single source of truth: Application.matchScore
        // Both candidate and recruiter APIs must return the same score

        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");
        recruiter.setRole(Role.RECRUITER);

        User candidate = new User();
        candidate.setId(2L);
        candidate.setName("Charlie");
        candidate.setEmail("charlie@test.com");
        candidate.setRole(Role.CANDIDATE);

        Job job = new Job();
        job.setId(5L);
        job.setTitle("Full Stack Dev");
        job.setCompanyName("TechCo");
        job.setRecruiter(recruiter);

        Application application = new Application();
        application.setId(30L);
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setMatchScore(73);
        application.setAtsScore(68);

        // Recruiter dashboard path
        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findAllByJobRecruiterOrderByAppliedAtDesc(recruiter)).thenReturn(List.of(application));

        var recruiterResults = applicationService.getApplicationsForRecruiter(recruiter.getEmail());
        assertEquals(1, recruiterResults.size());
        assertEquals(73, recruiterResults.get(0).getMatchScore());
        assertEquals(68, recruiterResults.get(0).getAtsScore());

        // Candidate dashboard path
        when(userRepository.findByEmail(candidate.getEmail())).thenReturn(Optional.of(candidate));
        when(applicationRepository.findAllByCandidateOrderByAppliedAtDesc(candidate)).thenReturn(List.of(application));
        when(interviewScheduleRepository.findAllByApplicationIn(List.of(application))).thenReturn(List.of());

        var candidateResults = applicationService.getApplicationsForCandidate(candidate.getEmail());
        assertEquals(1, candidateResults.size());
        assertEquals(73, candidateResults.get(0).getMatchScore());
        assertEquals(68, candidateResults.get(0).getAtsScore());

        // Both dashboards see the same score — single source of truth
        assertEquals(recruiterResults.get(0).getMatchScore(), candidateResults.get(0).getMatchScore());
        assertEquals(recruiterResults.get(0).getAtsScore(), candidateResults.get(0).getAtsScore());
    }

    @Test
    public void getApplicationsForCandidate_shouldReturnRejectionFeedback() {
        User candidate = new User();
        candidate.setId(1L);
        candidate.setEmail("candidate@test.com");
        candidate.setRole(Role.CANDIDATE);

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Backend Dev");
        job.setCompanyName("Corp");

        Application application = new Application();
        application.setId(50L);
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionFeedback("Not enough experience with microservices");

        when(userRepository.findByEmail(candidate.getEmail())).thenReturn(Optional.of(candidate));
        when(applicationRepository.findAllByCandidateOrderByAppliedAtDesc(candidate)).thenReturn(List.of(application));
        when(interviewScheduleRepository.findAllByApplicationIn(List.of(application))).thenReturn(List.of());

        var results = applicationService.getApplicationsForCandidate(candidate.getEmail());

        assertEquals(1, results.size());
        assertEquals("REJECTED", results.get(0).getStatus());
        assertEquals("Not enough experience with microservices", results.get(0).getRejectionFeedback());
    }

    @Test
    public void getApplicationsForCandidate_rejectedWithoutFeedback_shouldReturnNullFeedback() {
        User candidate = new User();
        candidate.setId(2L);
        candidate.setEmail("no-feedback@test.com");
        candidate.setRole(Role.CANDIDATE);

        Job job = new Job();
        job.setId(2L);
        job.setTitle("Frontend Dev");
        job.setCompanyName("WebCo");

        Application application = new Application();
        application.setId(51L);
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionFeedback(null); // recruiter didn't provide a reason

        when(userRepository.findByEmail(candidate.getEmail())).thenReturn(Optional.of(candidate));
        when(applicationRepository.findAllByCandidateOrderByAppliedAtDesc(candidate)).thenReturn(List.of(application));
        when(interviewScheduleRepository.findAllByApplicationIn(List.of(application))).thenReturn(List.of());

        var results = applicationService.getApplicationsForCandidate(candidate.getEmail());

        assertEquals(1, results.size());
        assertEquals("REJECTED", results.get(0).getStatus());
        assertNull(results.get(0).getRejectionFeedback());
    }

    @Test
    public void updateApplicationStatus_whenHired_shouldReturnHiredStatus() {
        User recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");
        recruiter.setRole(Role.RECRUITER);

        Job job = new Job();
        job.setId(1L);
        job.setRecruiter(recruiter);

        Application application = new Application();
        application.setId(11L);
        application.setJob(job);
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);

        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        CandidateApplicationResponse response = applicationService.updateApplicationStatus(
                application.getId(),
                recruiter.getEmail(),
                ApplicationStatus.SELECTED,
                null
        );

        assertNotNull(response);
        assertEquals(ApplicationStatus.SELECTED.name(), response.getStatus());
    }
}
