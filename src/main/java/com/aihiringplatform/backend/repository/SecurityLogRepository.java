package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {
    List<SecurityLog> findByEmailAndActionAndTimestampAfter(String email, String action, LocalDateTime timestamp);
    long countByEmailAndActionAndTimestampAfter(String email, String action, LocalDateTime timestamp);
}
