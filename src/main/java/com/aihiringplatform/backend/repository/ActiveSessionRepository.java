package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {
    Optional<ActiveSession> findByRefreshToken(String refreshToken);
    List<ActiveSession> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM ActiveSession a WHERE a.user.id = :userId")
    void deleteByUserId(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime time);
}
