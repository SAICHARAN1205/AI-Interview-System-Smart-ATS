package com.aihiringplatform.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_provider_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String providerName;

    @Column(nullable = false)
    private boolean isEnabled = true;

    @Column(nullable = false)
    private int priorityOrder = 0;

    @Column(name = "total_tokens_used", nullable = false)
    private long totalTokensUsed = 0;

    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public int getPriorityOrder() { return priorityOrder; }
    public void setPriorityOrder(int priorityOrder) { this.priorityOrder = priorityOrder; }
    public long getTotalTokensUsed() { return totalTokensUsed; }
    public void setTotalTokensUsed(long totalTokensUsed) { this.totalTokensUsed = totalTokensUsed; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
}
