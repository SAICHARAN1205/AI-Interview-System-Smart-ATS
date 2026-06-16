package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.ApplicationStatus;
import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByCandidateAndJob(User candidate, Job job);
    List<Application> findAllByJob(Job job);
    List<Application> findAllByCandidate(User candidate);
    List<Application> findAllByCandidateOrderByAppliedAtDesc(User candidate);
    List<Application> findAllByJobRecruiter(User recruiter);
    List<Application> findAllByJobRecruiterOrderByAppliedAtDesc(User recruiter);
    long countByJobRecruiter(User recruiter);
    long countByJobRecruiterAndStatus(User recruiter, ApplicationStatus status);
    long countByJobRecruiterAndStatusIn(User recruiter, Collection<ApplicationStatus> statuses);
    boolean existsByCandidateIdAndJobRecruiterEmail(Long candidateId, String recruiterEmail);
    long countByCandidateAndAppliedAtAfter(User candidate, java.time.LocalDateTime time);
}
