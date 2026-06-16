package com.aihiringplatform.backend.repository;

import com.aihiringplatform.backend.entity.AIProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIProviderConfigRepository extends JpaRepository<AIProviderConfig, Long> {
    Optional<AIProviderConfig> findByProviderName(String providerName);
    List<AIProviderConfig> findAllByOrderByPriorityOrderAsc();
    long countByIsEnabledTrue();
}
