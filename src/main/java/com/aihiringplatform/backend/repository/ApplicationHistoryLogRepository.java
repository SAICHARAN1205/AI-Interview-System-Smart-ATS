package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.ApplicationHistoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationHistoryLogRepository extends JpaRepository<ApplicationHistoryLog, Long> {
    List<ApplicationHistoryLog> findByApplicationIdOrderByChangedAtDesc(Long applicationId);
}
