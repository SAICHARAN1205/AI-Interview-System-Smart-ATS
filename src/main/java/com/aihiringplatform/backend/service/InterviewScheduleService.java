package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.InterviewScheduleRequest;
import com.aihiringplatform.backend.dto.InterviewScheduleResponse;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.InterviewSchedule;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class InterviewScheduleService {

    @Autowired
    private InterviewScheduleRepository interviewScheduleRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Transactional
    public InterviewScheduleResponse scheduleInterview(InterviewScheduleRequest request, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can schedule interviews.");
        }

        if (request.getApplicationId() == null || request.getInterviewDate() == null || request.getInterviewTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application, date, and time are required.");
        }

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found."));

        if (application.getJob() == null || application.getJob().getRecruiter() == null
                || !application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only schedule interviews for your jobs.");
        }

        if (application.getStatus() != ApplicationStatus.SHORTLISTED && application.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only shortlisted candidates can be scheduled for interviews.");
        }

        LocalDateTime scheduledAt;
        try {
            scheduledAt = LocalDateTime.of(
                    LocalDate.parse(request.getInterviewDate()),
                    LocalTime.parse(request.getInterviewTime())
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date/time format.");
        }

        if (scheduledAt.toLocalDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview date cannot be in the past.");
        }

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview time must be in the future.");
        }

        InterviewSchedule schedule = interviewScheduleRepository.findByApplication(application)
                .orElseGet(InterviewSchedule::new);

        if (schedule.getCreatedAt() == null) {
            schedule.setCreatedAt(LocalDateTime.now());
        }

        schedule.setApplication(application);
        schedule.setRecruiter(recruiter);
        schedule.setScheduledAt(scheduledAt);
        schedule.setUpdatedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        applicationRepository.save(application);

        InterviewSchedule saved = interviewScheduleRepository.save(schedule);

        activityLogService.logSuccess(recruiterEmail, "RECRUITER", "INTERVIEW_SCHEDULED", "Recruiter scheduled an interview for application " + application.getId() + " at " + scheduledAt, null);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewScheduleResponse> getSchedulesForRecruiter(String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view scheduled interviews.");
        }

        return interviewScheduleRepository.findAllByRecruiterOrderByScheduledAtAsc(recruiter)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InterviewScheduleResponse toResponse(InterviewSchedule schedule) {
        InterviewScheduleResponse response = new InterviewScheduleResponse();
        response.setId(schedule.getId());
        response.setScheduledAt(schedule.getScheduledAt());

        Application application = schedule.getApplication();
        if (application != null) {
            response.setApplicationId(application.getId());
            if (application.getCandidate() != null) {
                response.setCandidateId(application.getCandidate().getId());
                response.setCandidateName(application.getCandidate().getName());
                response.setCandidateEmail(application.getCandidate().getEmail());
            }
            if (application.getJob() != null) {
                response.setJobId(application.getJob().getId());
                response.setJobTitle(application.getJob().getTitle());
            }
        }

        return response;
    }
}
