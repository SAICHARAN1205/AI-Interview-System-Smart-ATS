package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.OtpToken;
import com.aihiringplatform.backend.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndTypeOrderByExpiryTimeDesc(String email, OtpType type);
    void deleteByExpiryTimeBefore(java.time.LocalDateTime time);
}
