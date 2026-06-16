package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.JobMatchSnapshot;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobMatchSnapshotRepository extends JpaRepository<JobMatchSnapshot, Long> {
    List<JobMatchSnapshot> findAllByCandidateOrderByCreatedAtAsc(User candidate);
    List<JobMatchSnapshot> findAllByCandidateOrderByCreatedAtDesc(User candidate);
    long countByCandidate(User candidate);
}
