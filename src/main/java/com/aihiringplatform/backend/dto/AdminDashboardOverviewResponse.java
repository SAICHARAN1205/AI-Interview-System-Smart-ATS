package com.aihiringplatform.backend.dto;

public class AdminDashboardOverviewResponse {
    private long totalUsers;
    private long activeRecruiters;
    private long totalJobs;
    private long totalApplications;
    private String systemHealth;
    private long aiProvidersActive;

    public AdminDashboardOverviewResponse() {}

    public AdminDashboardOverviewResponse(long totalUsers, long activeRecruiters, long totalJobs, long totalApplications, String systemHealth, long aiProvidersActive) {
        this.totalUsers = totalUsers;
        this.activeRecruiters = activeRecruiters;
        this.totalJobs = totalJobs;
        this.totalApplications = totalApplications;
        this.systemHealth = systemHealth;
        this.aiProvidersActive = aiProvidersActive;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getActiveRecruiters() { return activeRecruiters; }
    public void setActiveRecruiters(long activeRecruiters) { this.activeRecruiters = activeRecruiters; }
    
    public long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(long totalJobs) { this.totalJobs = totalJobs; }
    
    public long getTotalApplications() { return totalApplications; }
    public void setTotalApplications(long totalApplications) { this.totalApplications = totalApplications; }
    
    public String getSystemHealth() { return systemHealth; }
    public void setSystemHealth(String systemHealth) { this.systemHealth = systemHealth; }
    
    public long getAiProvidersActive() { return aiProvidersActive; }
    public void setAiProvidersActive(long aiProvidersActive) { this.aiProvidersActive = aiProvidersActive; }
}
