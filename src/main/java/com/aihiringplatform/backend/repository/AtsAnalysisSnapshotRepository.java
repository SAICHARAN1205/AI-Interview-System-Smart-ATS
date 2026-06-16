package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.AtsAnalysisSnapshot;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtsAnalysisSnapshotRepository extends JpaRepository<AtsAnalysisSnapshot, Long> {
    List<AtsAnalysisSnapshot> findAllByCandidateOrderByCreatedAtAsc(User candidate);
    List<AtsAnalysisSnapshot> findAllByCandidateOrderByCreatedAtDesc(User candidate);
    long countByCandidate(User candidate);
}
