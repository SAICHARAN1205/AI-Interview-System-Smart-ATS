package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    boolean existsByUserId(Long userId);
    java.util.Optional<Resume> findTopByUserIdOrderByUploadedAtDesc(Long userId);
    java.util.Optional<Resume> findTopByUserOrderByUploadedAtDesc(User user);
    List<Resume> findAllByUserIdOrderByUploadedAtDesc(Long userId);
}
