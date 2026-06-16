package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.InterviewScheduleRequest;
import com.aihiringplatform.backend.dto.InterviewScheduleResponse;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.InterviewSchedule;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.InterviewScheduleRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InterviewScheduleServiceTest {

    @Mock
    private InterviewScheduleRepository interviewScheduleRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private InterviewScheduleService interviewScheduleService;

    private User recruiter;
    private Application application;
    private Job job;
    private InterviewScheduleRequest request;

    @BeforeEach
    void setUp() {
        recruiter = new User();
        recruiter.setId(1L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setRole(Role.RECRUITER);

        job = new Job();
        job.setId(10L);
        job.setRecruiter(recruiter);

        lenient().when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
            Application a = i.getArgument(0);
            a.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
            return a;
        });

        application = new Application();
        application.setId(100L);
        application.setJob(job);
        application.setStatus(ApplicationStatus.SHORTLISTED);

        request = new InterviewScheduleRequest();
        request.setApplicationId(100L);
        request.setInterviewDate("2030-06-11");
        request.setInterviewTime("17:30");
    }

    @Test
    void testScheduleInterview_Success_ShortlistedCandidate() {
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        when(interviewScheduleRepository.findByApplication(application)).thenReturn(Optional.empty());

        InterviewSchedule savedSchedule = new InterviewSchedule();
        savedSchedule.setId(1L);
        savedSchedule.setApplication(application);
        when(interviewScheduleRepository.save(any(InterviewSchedule.class))).thenReturn(savedSchedule);

        InterviewScheduleResponse response = interviewScheduleService.scheduleInterview(request, "recruiter@example.com");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getApplicationId());
        verify(applicationRepository, times(1)).save(application);
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, application.getStatus());
    }

    @Test
    void testScheduleInterview_Fails_RejectedCandidateBlocked() {
        application.setStatus(ApplicationStatus.REJECTED);
        
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            interviewScheduleService.scheduleInterview(request, "recruiter@example.com");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Only shortlisted candidates can be scheduled"));
    }

    @Test
    void testScheduleInterview_Fails_MissingPayloadValidation() {
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        
        request.setInterviewDate(null); // Missing date

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            interviewScheduleService.scheduleInterview(request, "recruiter@example.com");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Application, date, and time are required"));
    }

    @Test
    void testScheduleInterview_Fails_PastDate() {
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        
        request.setInterviewDate("2020-01-01");
        request.setInterviewTime("12:00");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            interviewScheduleService.scheduleInterview(request, "recruiter@example.com");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Interview date cannot be in the past"));
    }

    @Test
    void testScheduleInterview_Fails_PastTimeToday() {
        when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiter));
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(application));
        
        java.time.LocalDateTime pastDateTime = java.time.LocalDateTime.now().minusMinutes(5);
        request.setInterviewDate(pastDateTime.toLocalDate().toString());
        request.setInterviewTime(String.format("%02d:%02d", pastDateTime.getHour(), pastDateTime.getMinute()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            interviewScheduleService.scheduleInterview(request, "recruiter@example.com");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Interview time must be in the future") || 
                   exception.getReason().contains("Interview date cannot be in the past"));
    }
}
