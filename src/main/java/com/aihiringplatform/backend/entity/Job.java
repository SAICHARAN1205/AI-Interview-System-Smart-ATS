package com.aihiringplatform.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank
    private String companyName;

    private String salary;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    private String location;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private User recruiter;

    // ── New fields ──────────────────────────────────────────────

    private LocalDate applicationDeadline;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'STANDARD'")
    private AtsStrictnessLevel atsStrictnessLevel = AtsStrictnessLevel.STANDARD;

    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer interviewRounds = 1;

    private String minimumEducation;

    private Double minimumPercentage;

    private Double minimumCGPA;

    private Integer minimumExperienceYears;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(columnDefinition = "TEXT")
    private String preferredSkills;

    private Integer openingsCount;

    private String noticePeriodPreference;

    @Column(columnDefinition = "TEXT")
    private String companyDescription;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(columnDefinition = "TEXT")
    private String recruiterNotes;

    // ── Getters and setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getRecruiter() { return recruiter; }
    public void setRecruiter(User recruiter) { this.recruiter = recruiter; }

    public LocalDate getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(LocalDate applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public WorkMode getWorkMode() { return workMode; }
    public void setWorkMode(WorkMode workMode) { this.workMode = workMode; }

    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(ExperienceLevel experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getMinimumEducation() { return minimumEducation; }
    public void setMinimumEducation(String minimumEducation) { this.minimumEducation = minimumEducation; }

    public Double getMinimumPercentage() { return minimumPercentage; }
    public void setMinimumPercentage(Double minimumPercentage) { this.minimumPercentage = minimumPercentage; }

    public Double getMinimumCGPA() { return minimumCGPA; }
    public void setMinimumCGPA(Double minimumCGPA) { this.minimumCGPA = minimumCGPA; }

    public Integer getMinimumExperienceYears() { return minimumExperienceYears; }
    public void setMinimumExperienceYears(Integer minimumExperienceYears) { this.minimumExperienceYears = minimumExperienceYears; }

    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }

    public String getPreferredSkills() { return preferredSkills; }
    public void setPreferredSkills(String preferredSkills) { this.preferredSkills = preferredSkills; }

    public Integer getOpeningsCount() { return openingsCount; }
    public void setOpeningsCount(Integer openingsCount) { this.openingsCount = openingsCount; }

    public String getNoticePeriodPreference() { return noticePeriodPreference; }
    public void setNoticePeriodPreference(String noticePeriodPreference) { this.noticePeriodPreference = noticePeriodPreference; }

    public String getCompanyDescription() { return companyDescription; }
    public void setCompanyDescription(String companyDescription) { this.companyDescription = companyDescription; }

    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }

    public String getRecruiterNotes() { return recruiterNotes; }
    public void setRecruiterNotes(String recruiterNotes) { this.recruiterNotes = recruiterNotes; }

    public AtsStrictnessLevel getAtsStrictnessLevel() { return atsStrictnessLevel; }
    public void setAtsStrictnessLevel(AtsStrictnessLevel atsStrictnessLevel) { this.atsStrictnessLevel = atsStrictnessLevel; }

    public Integer getInterviewRounds() { return interviewRounds; }
    public void setInterviewRounds(Integer interviewRounds) { this.interviewRounds = interviewRounds; }
}
