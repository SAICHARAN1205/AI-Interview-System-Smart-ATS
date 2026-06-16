package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.ExperienceLevel;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.JobType;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.entity.WorkMode;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private InterviewScheduleRepository interviewScheduleRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private JobService jobService;

    private User recruiter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@test.com");
        recruiter.setRole(Role.RECRUITER);
    }

    private Job buildBaseJob() {
        Job job = new Job();
        job.setTitle("Software Engineer");
        job.setDescription("Build scalable services");
        job.setCompanyName("TechCo");
        job.setSalary("10-15 LPA");
        job.setLocation("Bangalore");
        return job;
    }

    // ── Fresher validation ──────────────────────────────────────

    @Test
    public void createJob_fresher_withPercentage_shouldSucceed() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.FRESHER);
        job.setMinimumPercentage(60.0);
        job.setJobType(JobType.FULL_TIME);
        job.setWorkMode(WorkMode.ONSITE);

        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Job result = jobService.createJob(job, recruiter);
        assertNotNull(result.getId());
        assertEquals(ExperienceLevel.FRESHER, result.getExperienceLevel());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    public void createJob_fresher_withCGPA_shouldSucceed() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.FRESHER);
        job.setMinimumCGPA(7.5);

        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        Job result = jobService.createJob(job, recruiter);
        assertNotNull(result.getId());
    }

    @Test
    public void createJob_fresher_withoutPercentageOrCGPA_shouldFail() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.FRESHER);
        // Neither percentage nor CGPA set

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, recruiter));
        assertTrue(ex.getReason().contains("Fresher jobs require either minimum percentage or minimum CGPA"));
    }

    // ── Experienced validation ──────────────────────────────────

    @Test
    public void createJob_experienced_withYears_shouldSucceed() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.EXPERIENCED);
        job.setMinimumExperienceYears(3);

        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        Job result = jobService.createJob(job, recruiter);
        assertNotNull(result.getId());
        assertEquals(3, result.getMinimumExperienceYears());
    }

    @Test
    public void createJob_experienced_withoutYears_shouldFail() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.EXPERIENCED);
        // minimumExperienceYears not set

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, recruiter));
        assertTrue(ex.getReason().contains("Experienced jobs require minimum experience years"));
    }

    // ── Deadline validation ─────────────────────────────────────

    @Test
    public void createJob_pastDeadline_shouldFail() {
        Job job = buildBaseJob();
        job.setApplicationDeadline(LocalDate.of(2020, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, recruiter));
        assertTrue(ex.getReason().contains("Application deadline cannot be expired"));
    }

    @Test
    public void createJob_futureDeadline_shouldSucceed() {
        Job job = buildBaseJob();
        job.setApplicationDeadline(LocalDate.now().plusDays(30));

        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0);
            saved.setId(4L);
            return saved;
        });

        Job result = jobService.createJob(job, recruiter);
        assertNotNull(result.getId());
    }

    // ── Openings validation ─────────────────────────────────────

    @Test
    public void createJob_negativeOpenings_shouldFail() {
        Job job = buildBaseJob();
        job.setOpeningsCount(0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, recruiter));
        assertTrue(ex.getReason().contains("Number of openings must be at least 1"));
    }

    // ── Range validation ────────────────────────────────────────

    @Test
    public void createJob_invalidPercentage_shouldFail() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.FRESHER);
        job.setMinimumPercentage(150.0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, recruiter));
        assertTrue(ex.getReason().contains("Minimum percentage must be between 0 and 100"));
    }

    @Test
    public void createJob_invalidCGPA_shouldFail() {
        Job job = buildBaseJob();
        job.setExperienceLevel(ExperienceLevel.FRESHER);
        job.setMinimumCGPA(12.0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, recruiter));
        assertTrue(ex.getReason().contains("Minimum CGPA must be between 0 and 10"));
    }

    // ── Role check ──────────────────────────────────────────────

    @Test
    public void createJob_nonRecruiter_shouldFail() {
        User candidate = new User();
        candidate.setRole(Role.CANDIDATE);

        Job job = buildBaseJob();

        assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(job, candidate));
    }

    // ── Full valid job with all fields ───────────────────────────

    @Test
    public void createJob_fullPayload_shouldSucceed() {
        Job job = buildBaseJob();
        job.setJobType(JobType.FULL_TIME);
        job.setWorkMode(WorkMode.HYBRID);
        job.setExperienceLevel(ExperienceLevel.EXPERIENCED);
        job.setMinimumExperienceYears(5);
        job.setMinimumEducation("BTech");
        job.setRequiredSkills("Java, Spring Boot");
        job.setPreferredSkills("Docker, Kubernetes");
        job.setOpeningsCount(3);
        job.setApplicationDeadline(LocalDate.now().plusDays(30));
        job.setNoticePeriodPreference("30 days");
        job.setCompanyDescription("A leading tech firm");
        job.setBenefits("Health insurance, WFH");
        job.setRecruiterNotes("Internal: Fast track this role");

        when(jobRepository.save(any(Job.class))).thenAnswer(i -> {
            Job saved = i.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Job result = jobService.createJob(job, recruiter);
        assertEquals(10L, result.getId());
        assertEquals(JobType.FULL_TIME, result.getJobType());
        assertEquals(WorkMode.HYBRID, result.getWorkMode());
        assertEquals(5, result.getMinimumExperienceYears());
        assertEquals("BTech", result.getMinimumEducation());
        assertEquals(3, result.getOpeningsCount());
        assertEquals("Internal: Fast track this role", result.getRecruiterNotes());
    }
}
