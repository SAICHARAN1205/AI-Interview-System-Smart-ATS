package com.aihiringplatform.backend;

import com.aihiringplatform.backend.util.ResumeValidationUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeValidationUtilsTest {

    @Test
    void identifiesLikelyResumeText() {
        String resumeText = """
                Jane Candidate
                jane.candidate@example.com
                +1 555 999 1212

                Summary
                Software engineer with backend and cloud experience.

                Skills
                Java, Spring Boot, PostgreSQL, Docker

                Experience
                Built ATS workflows for recruiting teams.

                Education
                Bachelor of Engineering
                """;

        ResumeValidationUtils.ResumeValidationResult result = ResumeValidationUtils.analyze(resumeText);

        assertTrue(ResumeValidationUtils.isLikelyResume(resumeText));
        assertTrue(result.resumeConfidenceScore() >= 80);
        assertEquals("high", result.confidenceBand());
    }

    @Test
    void acceptsCompactFresherResumeWithoutExperienceAsMediumConfidence() {
        String fresherResume = """
                asha.rao@example.com
                B.Tech Computer Science, 2026
                Entry level backend developer focused on APIs and SQL.
                Java, SQL, Git
                """
                .trim();

        ResumeValidationUtils.ResumeValidationResult result = ResumeValidationUtils.analyze(fresherResume);

        assertTrue(result.likelyResume());
        assertTrue(result.resumeConfidenceScore() >= 50);
        assertEquals("medium", result.confidenceBand());
        assertEquals(ResumeValidationUtils.MEDIUM_CONFIDENCE_WARNING, result.warningMessage());
        assertTrue(result.detectedSections().contains("education"));
        assertTrue(result.detectedSections().contains("skills"));
    }

    @Test
    void rejectsSuspiciousNonResumeText() {
        String suspiciousText = """
                Tax Invoice
                Amount Due 2,450
                Bill To
                Payment Due 2026-05-27
                """;

        ResumeValidationUtils.ResumeValidationResult suspiciousResult = ResumeValidationUtils.analyze(suspiciousText);

        assertFalse(ResumeValidationUtils.isLikelyResume(suspiciousText));
        assertEquals("low", suspiciousResult.confidenceBand());
        assertEquals("suspicious_document_invoice", suspiciousResult.rejectionReason());
        assertFalse(ResumeValidationUtils.analyze("   ").likelyResume());
    }

    @Test
    void rejectsRandomNonResumePdfTextWithLowConfidence() {
        String randomText = """
                Quarterly facility maintenance checklist
                Review air conditioning vents, lighting inventory, and hallway signage.
                Submit observations to operations before the next building audit.
                """;

        ResumeValidationUtils.ResumeValidationResult result = ResumeValidationUtils.analyze(randomText);

        assertFalse(result.likelyResume());
        assertEquals("low", result.confidenceBand());
        assertEquals("low_resume_confidence", result.rejectionReason());
    }
}
