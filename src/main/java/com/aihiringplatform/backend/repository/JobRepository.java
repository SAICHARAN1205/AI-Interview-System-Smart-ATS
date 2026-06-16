package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.Job;
import com.aihiringplatform.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findAllByRecruiterOrderByCreatedAtDesc(User recruiter);
    long countByRecruiter(User recruiter);
}
