package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.InterviewSchedule;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {
    Optional<InterviewSchedule> findByApplication(Application application);
    List<InterviewSchedule> findAllByApplicationIn(List<Application> applications);
    List<InterviewSchedule> findAllByRecruiterOrderByScheduledAtAsc(User recruiter);
}
