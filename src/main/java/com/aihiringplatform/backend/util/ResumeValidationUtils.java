package com.aihiringplatform.backend.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class ResumeValidationUtils {

    public static final String MEDIUM_CONFIDENCE_WARNING =
            "This resume may have limited ATS readability, but analysis can continue.";

    private static final int HIGH_CONFIDENCE_THRESHOLD = 80;
    private static final int MEDIUM_CONFIDENCE_THRESHOLD = 50;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\d)");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("\\b(?:linkedin(?:\\.com)?(?:/in)?|linkedin\\.com/in)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITHUB_PATTERN = Pattern.compile("\\b(?:github(?:\\.com)?(?:/[^\\s]+)?|github\\.com/[^\\s]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_LINE_PATTERN = Pattern.compile("^[A-Z][A-Za-z]+(?:[\\s.'-]+[A-Z][A-Za-z]+){1,3}$");
    private static final Pattern UPPERCASE_NAME_LINE_PATTERN = Pattern.compile("^[A-Z]+(?:[\\s.'-]+[A-Z]+){1,3}$");
    private static final Pattern OBJECTIVE_PATTERN = Pattern.compile("\\b(objective|career objective|career goal)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROFILE_PATTERN = Pattern.compile("\\b(profile|professional profile|about me)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("\\b(summary|professional summary|executive summary|career summary)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EDUCATION_PATTERN = Pattern.compile(
            "\\b(education|academic|b\\.?tech|b\\.?e\\b|bachelor|master|m\\.?tech|mba|bca|mca|bsc|msc|diploma|university|college|school|cgpa|gpa)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SKILLS_PATTERN = Pattern.compile(
            "\\b(skills|technical skills|core competencies|competencies|tech stack|tooling|technologies)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
            "\\b(experience|employment|work history|professional experience|work experience|career history|freelance)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INTERNSHIP_PATTERN = Pattern.compile(
            "\\b(internship|intern|industrial training|trainee|apprentice)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PROJECTS_PATTERN = Pattern.compile(
            "\\b(projects|project experience|selected projects|academic projects|capstone)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CERTIFICATIONS_PATTERN = Pattern.compile(
            "\\b(certifications|certificates|licensed|licenses|credential)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+\\d{4}\\s*(?:-|to|\\u2013)\\s*(?:present|current|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+\\d{4})\\b|\\b\\d{4}\\s*(?:-|to|\\u2013)\\s*(?:present|current|\\d{4})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SKILL_SEPARATOR_PATTERN = Pattern.compile("[,|/\\u2022\\u00B7]");
    private static final Pattern URL_PATTERN = Pattern.compile("\\b(?:https?://|www\\.)\\S+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMON_TECH_PATTERN = Pattern.compile(
            "\\b(java|python|sql|spring|react|node|aws|azure|docker|kubernetes|html|css|javascript|typescript|git|linux|rest|api|postgresql|mysql|mongodb|excel|power bi|tableau)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, List<String>> SUSPICIOUS_DOCUMENT_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, Integer> SUSPICIOUS_DOCUMENT_THRESHOLDS = new LinkedHashMap<>();

    static {
        SUSPICIOUS_DOCUMENT_KEYWORDS.put("ticket", List.of("ticket", "boarding pass", "bus", "train", "pnr", "seat no", "departure", "arrival"));
        SUSPICIOUS_DOCUMENT_THRESHOLDS.put("ticket", 3);

        SUSPICIOUS_DOCUMENT_KEYWORDS.put("invoice", List.of("invoice", "bill to", "amount due", "tax invoice", "gst", "vat", "subtotal", "payment due"));
        SUSPICIOUS_DOCUMENT_THRESHOLDS.put("invoice", 2);

        SUSPICIOUS_DOCUMENT_KEYWORDS.put("receipt", List.of("receipt", "cash memo", "total paid", "thank you for your purchase", "payment received"));
        SUSPICIOUS_DOCUMENT_THRESHOLDS.put("receipt", 2);

        SUSPICIOUS_DOCUMENT_KEYWORDS.put("screenshot", List.of("screenshot", "screen shot", "captured on", "snipping tool"));
        SUSPICIOUS_DOCUMENT_THRESHOLDS.put("screenshot", 2);
    }

    private ResumeValidationUtils() {
    }

    public static boolean isLikelyResume(String extractedText) {
        return analyze(extractedText).likelyResume();
    }

    public static ResumeValidationResult analyze(String extractedText) {
        String rawText = extractedText == null ? "" : extractedText;
        String normalizedText = rawText.trim();
        List<String> lines = Arrays.stream(rawText.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(60)
                .toList();

        boolean hasName = detectName(lines);
        boolean hasEmail = EMAIL_PATTERN.matcher(normalizedText).find();
        boolean hasPhone = PHONE_PATTERN.matcher(normalizedText).find();
        boolean hasLinkedIn = LINKEDIN_PATTERN.matcher(normalizedText).find();
        boolean hasGitHub = GITHUB_PATTERN.matcher(normalizedText).find();
        boolean hasObjective = OBJECTIVE_PATTERN.matcher(normalizedText).find();
        boolean hasProfile = PROFILE_PATTERN.matcher(normalizedText).find();
        boolean hasSummary = SUMMARY_PATTERN.matcher(normalizedText).find();
        boolean hasEducation = detectEducation(normalizedText, lines);
        boolean hasSkills = detectSkills(normalizedText, lines);
        boolean hasExperience = EXPERIENCE_PATTERN.matcher(normalizedText).find();
        boolean hasInternship = INTERNSHIP_PATTERN.matcher(normalizedText).find();
        boolean hasProjects = PROJECTS_PATTERN.matcher(normalizedText).find();
        boolean hasCertifications = CERTIFICATIONS_PATTERN.matcher(normalizedText).find();
        boolean hasDateRanges = DATE_RANGE_PATTERN.matcher(normalizedText).find();
        boolean hasBullets = detectBulletDensity(lines);
        boolean hasContactCluster = detectContactCluster(lines);

        LinkedHashMap<String, Boolean> signalMap = new LinkedHashMap<>();
        signalMap.put("name", hasName);
        signalMap.put("email", hasEmail);
        signalMap.put("phone", hasPhone);
        signalMap.put("linkedin", hasLinkedIn);
        signalMap.put("github", hasGitHub);
        signalMap.put("objective", hasObjective);
        signalMap.put("profile", hasProfile);
        signalMap.put("summary", hasSummary);
        signalMap.put("education", hasEducation);
        signalMap.put("skills", hasSkills);
        signalMap.put("experience", hasExperience);
        signalMap.put("internship", hasInternship);
        signalMap.put("projects", hasProjects);
        signalMap.put("certifications", hasCertifications);

        LinkedHashSet<String> detectedSections = new LinkedHashSet<>();
        signalMap.forEach((signal, detected) -> {
            if (Boolean.TRUE.equals(detected)) {
                detectedSections.add(signal);
            }
        });

        LinkedHashSet<String> suspiciousIndicators = detectSuspiciousIndicators(normalizedText);
        int textLength = normalizedText.length();
        int contentSignalCount = countSignals(signalMap, Set.of(
                "objective",
                "profile",
                "summary",
                "education",
                "skills",
                "experience",
                "internship",
                "projects",
                "certifications"
        ));
        int contactSignalCount = countSignals(signalMap, Set.of("email", "phone", "linkedin", "github"));

        int confidenceScore = calculateConfidenceScore(
                textLength,
                signalMap,
                contentSignalCount,
                contactSignalCount,
                hasDateRanges,
                hasBullets,
                hasContactCluster,
                suspiciousIndicators.size()
        );

        String hardRejectReason = determineHardRejectReason(
                textLength,
                suspiciousIndicators,
                contentSignalCount,
                contactSignalCount,
                detectedSections.size()
        );

        if (hardRejectReason != null) {
            confidenceScore = Math.min(confidenceScore, 25);
        }

        String confidenceBand = resolveConfidenceBand(confidenceScore, hardRejectReason);
        boolean likelyResume = hardRejectReason == null && confidenceScore >= MEDIUM_CONFIDENCE_THRESHOLD;
        String rejectionReason = likelyResume
                ? null
                : hardRejectReason != null ? hardRejectReason : "low_resume_confidence";
        String warningMessage = likelyResume && "medium".equals(confidenceBand)
                ? MEDIUM_CONFIDENCE_WARNING
                : null;

        return new ResumeValidationResult(
                likelyResume,
                confidenceScore,
                confidenceBand,
                List.copyOf(detectedSections),
                List.copyOf(suspiciousIndicators),
                rejectionReason,
                warningMessage
        );
    }

    private static int calculateConfidenceScore(
            int textLength,
            Map<String, Boolean> signalMap,
            int contentSignalCount,
            int contactSignalCount,
            boolean hasDateRanges,
            boolean hasBullets,
            boolean hasContactCluster,
            int suspiciousIndicatorCount
    ) {
        int score = 0;

        score += scoreSignal(signalMap, "name", 8);
        score += scoreSignal(signalMap, "email", 16);
        score += scoreSignal(signalMap, "phone", 14);
        score += scoreSignal(signalMap, "linkedin", 8);
        score += scoreSignal(signalMap, "github", 8);
        score += scoreSignal(signalMap, "objective", 10);
        score += scoreSignal(signalMap, "profile", 8);
        score += scoreSignal(signalMap, "summary", 10);
        score += scoreSignal(signalMap, "education", 16);
        score += scoreSignal(signalMap, "skills", 14);
        score += scoreSignal(signalMap, "experience", 12);
        score += scoreSignal(signalMap, "internship", 10);
        score += scoreSignal(signalMap, "projects", 8);
        score += scoreSignal(signalMap, "certifications", 5);

        if (hasDateRanges) {
            score += 6;
        }
        if (hasBullets) {
            score += 4;
        }
        if (hasContactCluster) {
            score += 10;
        }

        if (textLength >= 120) {
            score += 4;
        }
        if (textLength >= 250) {
            score += 6;
        }
        if (textLength >= 600) {
            score += 6;
        }

        boolean hasEducation = Boolean.TRUE.equals(signalMap.get("education"));
        boolean hasSkills = Boolean.TRUE.equals(signalMap.get("skills"));
        boolean hasExperience = Boolean.TRUE.equals(signalMap.get("experience"));
        boolean hasInternship = Boolean.TRUE.equals(signalMap.get("internship"));
        boolean hasProjects = Boolean.TRUE.equals(signalMap.get("projects"));
        boolean hasSummaryGroup = Boolean.TRUE.equals(signalMap.get("objective"))
                || Boolean.TRUE.equals(signalMap.get("profile"))
                || Boolean.TRUE.equals(signalMap.get("summary"));
        boolean hasLinkedProfiles = Boolean.TRUE.equals(signalMap.get("linkedin"))
                || Boolean.TRUE.equals(signalMap.get("github"));

        if (contactSignalCount >= 1 && contentSignalCount >= 1) {
            score += 12;
        }
        if (hasEducation && (hasSkills || hasExperience || hasInternship || hasProjects)) {
            score += 10;
        }
        if (hasLinkedProfiles && (hasSkills || hasProjects || hasEducation)) {
            score += 6;
        }
        if (hasSummaryGroup && (hasEducation || hasSkills || hasExperience || hasInternship)) {
            score += 6;
        }

        if (contactSignalCount == 0) {
            score -= 18;
        }
        if (contentSignalCount <= 1) {
            score -= 18;
        } else if (contentSignalCount == 2) {
            score -= 6;
        }
        if (textLength < 80) {
            score -= 28;
        } else if (textLength < 140) {
            score -= 12;
        }

        score -= suspiciousIndicatorCount * 30;
        return Math.max(0, Math.min(100, score));
    }

    private static String determineHardRejectReason(
            int textLength,
            Set<String> suspiciousIndicators,
            int contentSignalCount,
            int contactSignalCount,
            int detectedSignalCount
    ) {
        if (textLength == 0) {
            return "blank_pdf";
        }

        if (!suspiciousIndicators.isEmpty()) {
            String indicator = suspiciousIndicators.iterator().next();
            if ("screenshot".equals(indicator)) {
                return "image_only_or_unreadable";
            }
            return "suspicious_document_" + indicator;
        }

        if (textLength < 60 && detectedSignalCount <= 1) {
            return "image_only_or_unreadable";
        }

        if (textLength < 100 && contactSignalCount == 0 && contentSignalCount == 0) {
            return "image_only_or_unreadable";
        }

        return null;
    }

    private static String resolveConfidenceBand(int confidenceScore, String hardRejectReason) {
        if (hardRejectReason != null || confidenceScore < MEDIUM_CONFIDENCE_THRESHOLD) {
            return "low";
        }
        if (confidenceScore >= HIGH_CONFIDENCE_THRESHOLD) {
            return "high";
        }
        return "medium";
    }

    private static LinkedHashSet<String> detectSuspiciousIndicators(String normalizedText) {
        String lowerText = normalizedText.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> indicators = new LinkedHashSet<>();

        if (lowerText.isBlank()) {
            indicators.add("blank_pdf");
            return indicators;
        }

        SUSPICIOUS_DOCUMENT_KEYWORDS.forEach((type, keywords) -> {
            int hits = countKeywordHits(lowerText, keywords);
            int threshold = SUSPICIOUS_DOCUMENT_THRESHOLDS.getOrDefault(type, 2);
            if (hits >= threshold) {
                indicators.add(type);
            }
        });

        return indicators;
    }

    private static int countKeywordHits(String lowerText, List<String> keywords) {
        int hits = 0;
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase(Locale.ROOT))) {
                hits += 1;
            }
        }
        return hits;
    }

    private static int countSignals(Map<String, Boolean> signalMap, Set<String> keys) {
        return (int) keys.stream()
                .filter(key -> Boolean.TRUE.equals(signalMap.get(key)))
                .count();
    }

    private static int scoreSignal(Map<String, Boolean> signalMap, String key, int points) {
        return Boolean.TRUE.equals(signalMap.get(key)) ? points : 0;
    }

    private static boolean detectEducation(String normalizedText, List<String> lines) {
        if (EDUCATION_PATTERN.matcher(normalizedText).find()) {
            return true;
        }

        return lines.stream().limit(20).anyMatch(line -> {
            String lowerLine = line.toLowerCase(Locale.ROOT);
            return lowerLine.contains("institute")
                    || lowerLine.contains("faculty")
                    || lowerLine.contains("graduated")
                    || lowerLine.contains("class of")
                    || lowerLine.contains("cgpa")
                    || lowerLine.contains("gpa");
        });
    }

    private static boolean detectSkills(String normalizedText, List<String> lines) {
        if (SKILLS_PATTERN.matcher(normalizedText).find()) {
            return true;
        }

        if (COMMON_TECH_PATTERN.matcher(normalizedText).results().limit(4).count() >= 3) {
            return true;
        }

        return lines.stream().limit(30).anyMatch(ResumeValidationUtils::looksLikeSkillInventory);
    }

    private static boolean looksLikeSkillInventory(String line) {
        String sanitizedLine = line == null ? "" : line.trim();
        if (sanitizedLine.length() < 12 || sanitizedLine.length() > 180) {
            return false;
        }

        String[] tokens = SKILL_SEPARATOR_PATTERN.split(sanitizedLine);
        List<String> normalizedTokens = new ArrayList<>();
        for (String token : tokens) {
            String cleaned = token.replaceAll("[^A-Za-z0-9+#. ]", "").trim();
            if (!cleaned.isBlank()) {
                normalizedTokens.add(cleaned);
            }
        }

        if (normalizedTokens.size() < 4) {
            return false;
        }

        long compactTokenCount = normalizedTokens.stream()
                .filter(token -> token.length() >= 2 && token.length() <= 24)
                .count();
        return compactTokenCount >= 4;
    }

    private static boolean detectBulletDensity(List<String> lines) {
        long bulletLines = lines.stream()
                .filter(line -> line.startsWith("-")
                        || line.startsWith("*")
                        || line.startsWith("\u2022")
                        || line.matches("^\\d+[.)].*"))
                .count();
        return bulletLines >= 3;
    }

    private static boolean detectContactCluster(List<String> lines) {
        String topBlock = String.join(" ", lines.stream().limit(10).toList());
        int clusterSignals = 0;
        if (EMAIL_PATTERN.matcher(topBlock).find()) {
            clusterSignals += 1;
        }
        if (PHONE_PATTERN.matcher(topBlock).find()) {
            clusterSignals += 1;
        }
        if (LINKEDIN_PATTERN.matcher(topBlock).find()) {
            clusterSignals += 1;
        }
        if (GITHUB_PATTERN.matcher(topBlock).find()) {
            clusterSignals += 1;
        }
        return clusterSignals >= 2;
    }

    private static boolean detectName(List<String> lines) {
        for (String line : lines.stream().limit(8).toList()) {
            if (line.length() > 60
                    || EMAIL_PATTERN.matcher(line).find()
                    || PHONE_PATTERN.matcher(line).find()
                    || URL_PATTERN.matcher(line).find()) {
                continue;
            }

            String compact = line.replaceAll("\\s+", " ").trim();
            if (compact.length() < 4 || compact.matches(".*\\d.*")) {
                continue;
            }

            if (NAME_LINE_PATTERN.matcher(compact).matches() || UPPERCASE_NAME_LINE_PATTERN.matcher(compact).matches()) {
                return true;
            }
        }

        return false;
    }

    public record ResumeValidationResult(
            boolean likelyResume,
            int resumeConfidenceScore,
            String confidenceBand,
            List<String> detectedSections,
            List<String> suspiciousIndicators,
            String rejectionReason,
            String warningMessage
    ) {
        public boolean highConfidence() {
            return "high".equals(confidenceBand);
        }

        public boolean mediumConfidence() {
            return "medium".equals(confidenceBand);
        }
    }
}
