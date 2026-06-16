package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.entity.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int calculateMatchScore(String resumeText, Job job, List<String> matchedSkills) {
        MatchResponse response = calculateDetailedMatch(resumeText, job);
        if (matchedSkills != null) {
            matchedSkills.clear();
            if (response.getMatchedSkills() != null) {
                matchedSkills.addAll(response.getMatchedSkills());
            }
        }
        return response.getMatchPercentage() == null ? response.getScore() : response.getMatchPercentage();
    }

    public MatchResponse calculateDetailedMatch(String resumeText, String targetRole, List<String> requiredSkills) {
        Job tempJob = new Job();
        tempJob.setTitle(targetRole);
        if (requiredSkills != null && !requiredSkills.isEmpty()) {
            tempJob.setRequiredSkills(String.join(", ", requiredSkills));
        }
        return calculateDetailedMatch(resumeText, tempJob);
    }

    public MatchResponse calculateDetailedMatch(String resumeText, Job job) {
        if (resumeText == null) resumeText = "";
        String lowerResume = resumeText.toLowerCase(Locale.ROOT);
        
        List<String> required = parseSkills(job.getRequiredSkills() != null && !job.getRequiredSkills().isBlank() ? job.getRequiredSkills() : job.getSkills());
        List<String> preferred = parseSkills(job.getPreferredSkills());
        
        // 1. Role-Specific Intelligence Bank
        if (required.isEmpty()) {
            required = getRoleIntelligenceSkills(job.getTitle());
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        
        // Technical Skills Parsing
        double reqScore = 0;
        double reqMatchPercentage = 0;
        if (!required.isEmpty()) {
            int reqMatches = 0;
            for (String req : required) {
                if (hasSkill(lowerResume, req)) {
                    matched.add(req);
                    reqMatches++;
                } else {
                    missing.add(req);
                }
            }
            reqMatchPercentage = (double) reqMatches / required.size();
            reqScore = reqMatchPercentage * 40.0; // Max 40
        }

        double prefScore = 0;
        if (!preferred.isEmpty()) {
            int prefMatches = 0;
            for (String pref : preferred) {
                if (hasSkill(lowerResume, pref)) {
                    if (!matched.contains(pref)) matched.add(pref);
                    prefMatches++;
                }
            }
            prefScore = ((double) prefMatches / preferred.size()) * 10.0; // Max 10
        } else if (!required.isEmpty()) {
            reqScore = reqMatchPercentage * 50.0; // Scale to 50 if no preferred
        }

        double totalSkillScore = reqScore + prefScore;
        double overallSkillPercentage = (!required.isEmpty() || !preferred.isEmpty()) ? (totalSkillScore / 50.0) : 0;

        // 2. Project Relevance & Quantified Achievements
        double projectScore = 0;
        boolean hasProjects = lowerResume.contains("project") || lowerResume.contains("github.com") || lowerResume.contains("portfolio");
        if (hasProjects) projectScore += 10.0;

        boolean hasQuantifiedImpact = Pattern.compile("\\b\\d{1,3}%\\b|\\b\\d+[kKmM]?\\b").matcher(lowerResume).find();
        if (hasQuantifiedImpact) projectScore += 15.0; // Total 25

        // 3. Experience Alignment
        double expScore = 0;
        boolean foundExp = false;
        if (job.getMinimumExperienceYears() != null && job.getMinimumExperienceYears() > 0) {
            for (int i = job.getMinimumExperienceYears(); i <= job.getMinimumExperienceYears() + 7; i++) {
                if (lowerResume.contains(i + "+ years") || lowerResume.contains(i + " years") || lowerResume.contains(i + " yrs")) {
                    foundExp = true;
                    break;
                }
            }
            if (foundExp) expScore = 15.0;
        } else {
            if (lowerResume.contains("experience") || lowerResume.contains("work history")) expScore = 15.0;
        }

        // 4. Structural Readability & Formatting
        double formatScore = 0;
        if (lowerResume.contains("education") || lowerResume.contains("degree")) formatScore += 5;
        if (lowerResume.contains("experience") || lowerResume.contains("skills")) formatScore += 5;

        // Raw Score Assembly
        int rawScore = (int) Math.round(totalSkillScore + projectScore + expScore + formatScore);

        // 5. Strict Normalization Logic
        int finalScore = rawScore;
        
        // Strictness Configuration
        boolean isStrict = job.getAtsStrictnessLevel() != null && job.getAtsStrictnessLevel().name().equals("STRICT");
        
        // Penalty: Missing core keywords
        if (overallSkillPercentage < 0.50) {
            finalScore = (int) (finalScore * (isStrict ? 0.70 : 0.85)); // 15-30% penalty
        }
        
        // Penalty: No quantified impact
        if (!hasQuantifiedImpact) {
            finalScore = (int) (finalScore * (isStrict ? 0.80 : 0.90)); // 10-20% penalty
        }
        
        // Hard Caps
        if (overallSkillPercentage < 0.30) finalScore = Math.min(finalScore, 59); // Weak
        if (!hasProjects && isStrict) finalScore = Math.min(finalScore, 69); // Max average
        
        // Normalization ranges (90+ exceptional, 80-89 strong, 70-79 decent, 60-69 average, <60 weak)
        if (finalScore > 100) finalScore = 100;

        MatchResponse response = new MatchResponse();
        response.setJobId(job.getId());
        response.setScore(finalScore);
        response.setMatchPercentage(finalScore);
        response.setMatchedSkills(matched);
        response.setMissingSkills(missing);
        response.setTargetRole(job.getTitle());
        
        StringBuilder summary = new StringBuilder();
        if (finalScore >= 90) summary.append("Exceptional match. ");
        else if (finalScore >= 80) summary.append("Strong match. ");
        else if (finalScore >= 70) summary.append("Decent match. ");
        else if (finalScore >= 60) summary.append("Average match. ");
        else summary.append("Weak match. ");

        if (missing.size() > 2) {
            summary.append("Candidate is missing critical skills: ").append(String.join(", ", missing.subList(0, 2))).append(". ");
        }
        if (!hasQuantifiedImpact) summary.append("Resume lacks quantified metrics or achievements. ");
        if (!hasProjects) summary.append("No technical projects detected. ");
        
        response.setRecruiterSummary(summary.toString());
        return response;
    }
    
    private List<String> getRoleIntelligenceSkills(String jobTitle) {
        if (jobTitle == null) return new ArrayList<>();
        String lowerTitle = jobTitle.toLowerCase(Locale.ROOT);
        
        if (lowerTitle.contains("software engineer") || lowerTitle.contains("backend") || lowerTitle.contains("java")) {
            return Arrays.asList("Java", "Spring Boot", "APIs", "DSA", "React", "Microservices", "SQL");
        } else if (lowerTitle.contains("data analyst")) {
            return Arrays.asList("SQL", "Power BI", "Excel", "Python", "Statistics", "Tableau", "Data Visualization");
        } else if (lowerTitle.contains("cybersecurity") || lowerTitle.contains("security")) {
            return Arrays.asList("SIEM", "Networking", "Linux", "Threat Detection", "Firewalls", "Incident Response");
        } else if (lowerTitle.contains("frontend") || lowerTitle.contains("ui")) {
            return Arrays.asList("JavaScript", "React", "HTML", "CSS", "TypeScript", "Redux");
        }
        
        return Arrays.asList("Communication", "Problem Solving", "Teamwork"); // Generic Fallback
    }

    private List<String> parseSkills(String skillsStr) {
        if (skillsStr == null || skillsStr.isBlank()) return new ArrayList<>();
        return Arrays.stream(skillsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean hasSkill(String lowerResume, String skill) {
        String lowerSkill = skill.toLowerCase(Locale.ROOT);
        String regex = "\\b" + Pattern.quote(lowerSkill) + "\\b";
        Matcher matcher = Pattern.compile(regex).matcher(lowerResume);
        if (matcher.find()) return true;

        if (lowerSkill.contains(" ")) {
            for (String part : lowerSkill.split(" ")) {
                if (part.length() > 3) {
                    if (Pattern.compile("\\b" + Pattern.quote(part) + "\\b").matcher(lowerResume).find()) return true;
                }
            }
        }
        return false;
    }
}
