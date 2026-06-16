package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.MockInterviewSession;
import com.aihiringplatform.backend.entity.MockInterviewStatus;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockInterviewSessionRepository extends JpaRepository<MockInterviewSession, Long> {
    Optional<MockInterviewSession> findByIdAndCandidateEmail(Long id, String candidateEmail);
    List<MockInterviewSession> findAllByCandidateOrderByCompletedAtDesc(User candidate);
    List<MockInterviewSession> findAllByCandidateOrderByCompletedAtAsc(User candidate);
    List<MockInterviewSession> findTop20ByCandidateOrderByUpdatedAtDesc(User candidate);
    List<MockInterviewSession> findAllByJobOrderByCompletedAtDesc(Job job);
    long countByCandidateAndStatus(User candidate, MockInterviewStatus status);
}
