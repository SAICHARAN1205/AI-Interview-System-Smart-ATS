package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ExperienceLevel;
import com.aihiringplatform.backend.entity.InterviewSchedule;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import com.aihiringplatform.backend.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public Job createJob(Job job, User recruiter) {
        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can create jobs.");
        }

        validateJob(job);

        job.setRecruiter(recruiter);
        job.setCreatedAt(LocalDateTime.now());

        Job saved = jobRepository.save(job);
        logger.info("Job created: id={} title='{}' jobType={} workMode={} experienceLevel={} by recruiter={}",
                saved.getId(), saved.getTitle(), saved.getJobType(), saved.getWorkMode(),
                saved.getExperienceLevel(), recruiter.getEmail());

        activityLogService.logSuccess(recruiter.getEmail(), "RECRUITER", "JOB_CREATION", "Recruiter created a new job posting: " + saved.getTitle() + " (ID: " + saved.getId() + ")", null);

        return saved;
    }

    /**
     * Validates job fields with conditional rules based on experience level.
     * Throws ResponseStatusException with a concatenated error message for all violations.
     */
    public void validateJob(Job job) {
        List<String> errors = new ArrayList<>();

        // Deadline must be in the future
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDate.now())) {
            errors.add("Application deadline cannot be expired.");
        }

        // Openings must be >= 1
        if (job.getOpeningsCount() != null && job.getOpeningsCount() < 1) {
            errors.add("Number of openings must be at least 1.");
        }

        // Percentage range
        if (job.getMinimumPercentage() != null && (job.getMinimumPercentage() < 0 || job.getMinimumPercentage() > 100)) {
            errors.add("Minimum percentage must be between 0 and 100.");
        }

        // CGPA range
        if (job.getMinimumCGPA() != null && (job.getMinimumCGPA() < 0 || job.getMinimumCGPA() > 10)) {
            errors.add("Minimum CGPA must be between 0 and 10.");
        }

        // Experience years must be non-negative
        if (job.getMinimumExperienceYears() != null && job.getMinimumExperienceYears() < 0) {
            errors.add("Minimum experience years cannot be negative.");
        }

        // Conditional validation: FRESHER requires percentage or CGPA
        if (job.getExperienceLevel() == ExperienceLevel.FRESHER) {
            if (job.getMinimumPercentage() == null && job.getMinimumCGPA() == null) {
                errors.add("Fresher jobs require either minimum percentage or minimum CGPA.");
            }
        }

        // Conditional validation: EXPERIENCED requires experience years
        if (job.getExperienceLevel() == ExperienceLevel.EXPERIENCED) {
            if (job.getMinimumExperienceYears() == null) {
                errors.add("Experienced jobs require minimum experience years.");
            }
        }

        if (!errors.isEmpty()) {
            String message = String.join(" ", errors);
            logger.warn("Job validation failed: {}", message);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    public Page<Job> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
    }

    public Job updateJob(Long jobId, Job updates, User requester) {
        if (requester.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can update jobs.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));

        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(requester.getId())) {
            if (job.getRecruiter() != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit jobs you own.");
            }
        }

        if (updates.getTitle() != null && !updates.getTitle().isBlank()) {
            job.setTitle(updates.getTitle());
        }
        if (updates.getDescription() != null && !updates.getDescription().isBlank()) {
            job.setDescription(updates.getDescription());
        }
        if (updates.getCompanyName() != null && !updates.getCompanyName().isBlank()) {
            job.setCompanyName(updates.getCompanyName());
        }
        if (updates.getSalary() != null) {
            job.setSalary(updates.getSalary());
        }
        if (updates.getSkills() != null) {
            job.setSkills(updates.getSkills());
        }
        if (updates.getRequirements() != null) {
            job.setRequirements(updates.getRequirements());
        }
        if (updates.getLocation() != null) {
            job.setLocation(updates.getLocation());
        }

        // New fields
        if (updates.getApplicationDeadline() != null) {
            job.setApplicationDeadline(updates.getApplicationDeadline());
        }
        if (updates.getJobType() != null) {
            job.setJobType(updates.getJobType());
        }
        if (updates.getWorkMode() != null) {
            job.setWorkMode(updates.getWorkMode());
        }
        if (updates.getExperienceLevel() != null) {
            job.setExperienceLevel(updates.getExperienceLevel());
        }
        if (updates.getMinimumEducation() != null) {
            job.setMinimumEducation(updates.getMinimumEducation());
        }
        if (updates.getMinimumPercentage() != null) {
            job.setMinimumPercentage(updates.getMinimumPercentage());
        }
        if (updates.getMinimumCGPA() != null) {
            job.setMinimumCGPA(updates.getMinimumCGPA());
        }
        if (updates.getMinimumExperienceYears() != null) {
            job.setMinimumExperienceYears(updates.getMinimumExperienceYears());
        }
        if (updates.getRequiredSkills() != null) {
            job.setRequiredSkills(updates.getRequiredSkills());
        }
        if (updates.getPreferredSkills() != null) {
            job.setPreferredSkills(updates.getPreferredSkills());
        }
        if (updates.getOpeningsCount() != null) {
            job.setOpeningsCount(updates.getOpeningsCount());
        }
        if (updates.getNoticePeriodPreference() != null) {
            job.setNoticePeriodPreference(updates.getNoticePeriodPreference());
        }
        if (updates.getCompanyDescription() != null) {
            job.setCompanyDescription(updates.getCompanyDescription());
        }
        if (updates.getBenefits() != null) {
            job.setBenefits(updates.getBenefits());
        }
        if (updates.getRecruiterNotes() != null) {
            job.setRecruiterNotes(updates.getRecruiterNotes());
        }
        if (updates.getAtsStrictnessLevel() != null) {
            job.setAtsStrictnessLevel(updates.getAtsStrictnessLevel());
        }
        if (updates.getInterviewRounds() != null) {
            job.setInterviewRounds(updates.getInterviewRounds());
        }

        validateJob(job);

        Job updatedJob = jobRepository.save(job);
        activityLogService.logSuccess(requester.getEmail(), "RECRUITER", "JOB_UPDATE", "Recruiter updated job posting: " + updatedJob.getTitle() + " (ID: " + updatedJob.getId() + ")", null);
        
        return updatedJob;
    }

    public void deleteJob(Long jobId, User requester) {
        if (requester.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can delete jobs.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));

        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(requester.getId())) {
            if (job.getRecruiter() != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete jobs you own.");
            }
        }

        List<Application> applications = applicationRepository.findAllByJob(job);
        if (!applications.isEmpty()) {
            List<InterviewSchedule> schedules = interviewScheduleRepository.findAllByApplicationIn(applications);
            if (!schedules.isEmpty()) {
                interviewScheduleRepository.deleteAll(schedules);
            }
            applicationRepository.deleteAll(applications);
        }

        jobRepository.delete(job);
        activityLogService.logSuccess(requester.getEmail(), "RECRUITER", "JOB_DELETION", "Recruiter deleted job posting: " + job.getTitle() + " (ID: " + job.getId() + ")", null);
    }
}
