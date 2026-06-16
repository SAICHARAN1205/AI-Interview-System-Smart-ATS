package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.InterviewQuestionBreakdownItem;
import com.aihiringplatform.backend.dto.InterviewResultResponse;
import com.aihiringplatform.backend.dto.MatchResponse;
import com.aihiringplatform.backend.entity.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generateAtsReport(Application application) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.BLACK);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

        // Header
        Paragraph title = new Paragraph("SmartATS Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph("\n"));

        // Candidate Info
        document.add(new Paragraph("Candidate Information", subtitleFont));
        document.add(new Paragraph("Name: " + (application.getCandidate() != null ? application.getCandidate().getName() : "Unknown"), normalFont));
        document.add(new Paragraph("Target Role: " + (application.getJob() != null ? application.getJob().getTitle() : "Unknown"), normalFont));
        document.add(new Paragraph("Applied At: " + application.getAppliedAt().toString(), normalFont));
        document.add(new Paragraph("\n"));

        // Scores
        document.add(new Paragraph("Scores", subtitleFont));
        int matchScore = application.getMatchScore() != null ? application.getMatchScore() : 0;
        int atsScore = application.getAtsScore() != null ? application.getAtsScore() : 0;
        document.add(new Paragraph("Resume Match Score: " + matchScore + "%", boldFont));
        document.add(new Paragraph("Overall ATS Score: " + atsScore + "%", boldFont));
        document.add(new Paragraph("\n"));

        // Breakdown
        if (application.getAtsBreakdownJson() != null && !application.getAtsBreakdownJson().isBlank()) {
            try {
                MatchResponse breakdown = objectMapper.readValue(application.getAtsBreakdownJson(), MatchResponse.class);
                
                document.add(new Paragraph("Analysis Breakdown", subtitleFont));
                document.add(new Paragraph(breakdown.getRecruiterSummary() != null ? breakdown.getRecruiterSummary() : "No summary available.", normalFont));
                document.add(new Paragraph("\n"));

                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(100);
                
                PdfPCell matchedCell = new PdfPCell(new Phrase("Matched Skills", boldFont));
                matchedCell.setBackgroundColor(new Color(220, 255, 220));
                table.addCell(matchedCell);
                
                PdfPCell missingCell = new PdfPCell(new Phrase("Missing Skills", boldFont));
                missingCell.setBackgroundColor(new Color(255, 220, 220));
                table.addCell(missingCell);

                List<String> matched = breakdown.getMatchedSkills();
                List<String> missing = breakdown.getMissingSkills();
                
                String matchedStr = (matched == null || matched.isEmpty()) ? "None" : String.join("\n- ", matched);
                if (!matchedStr.equals("None")) matchedStr = "- " + matchedStr;
                
                String missingStr = (missing == null || missing.isEmpty()) ? "None" : String.join("\n- ", missing);
                if (!missingStr.equals("None")) missingStr = "- " + missingStr;

                table.addCell(new Phrase(matchedStr, normalFont));
                table.addCell(new Phrase(missingStr, normalFont));
                
                document.add(table);
                
            } catch(Exception e) {
                document.add(new Paragraph("Failed to load detailed breakdown.", normalFont));
            }
        }

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateGenericReport(com.aihiringplatform.backend.dto.ResumeAnalysisResponse response) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.BLACK);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);

        // Header
        Paragraph title = new Paragraph("SmartATS Analysis Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph("\n"));

        // Candidate Info
        document.add(new Paragraph("Analysis Overview", subtitleFont));
        document.add(new Paragraph("Analyzed On: " + java.time.LocalDateTime.now(), normalFont));
        document.add(new Paragraph("\n"));

        // Scores
        document.add(new Paragraph("Scores", subtitleFont));
        int atsScore = response.getAtsScore() != null ? response.getAtsScore() : 0;
        document.add(new Paragraph("Overall ATS Score: " + atsScore + "%", boldFont));
        document.add(new Paragraph("Readability / Formatting Quality: " + (response.getFormattingQuality() != null ? response.getFormattingQuality() : "N/A"), normalFont));
        document.add(new Paragraph("\n"));

        // Breakdown
        document.add(new Paragraph("Analysis Breakdown", subtitleFont));
        document.add(new Paragraph(response.getSummary() != null ? response.getSummary() : "No summary available.", normalFont));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        
        PdfPCell matchedCell = new PdfPCell(new Phrase("Matched Keywords", boldFont));
        matchedCell.setBackgroundColor(new Color(220, 255, 220));
        table.addCell(matchedCell);
        
        PdfPCell missingCell = new PdfPCell(new Phrase("Missing Keywords", boldFont));
        missingCell.setBackgroundColor(new Color(255, 220, 220));
        table.addCell(missingCell);

        List<String> matched = response.getMatchedKeywords();
        List<String> missing = response.getMissingKeywords();
        
        String matchedStr = (matched == null || matched.isEmpty()) ? "None" : String.join("\n- ", matched);
        if (!matchedStr.equals("None")) matchedStr = "- " + matchedStr;
        
        String missingStr = (missing == null || missing.isEmpty()) ? "None" : String.join("\n- ", missing);
        if (!missingStr.equals("None")) missingStr = "- " + missingStr;

        table.addCell(new Phrase(matchedStr, normalFont));
        table.addCell(new Phrase(missingStr, normalFont));
        
        document.add(table);
        
        // Tips
        if (response.getOptimizationTips() != null && !response.getOptimizationTips().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Optimization Tips", subtitleFont));
            for (String tip : response.getOptimizationTips()) {
                document.add(new Paragraph("- " + tip, normalFont));
            }
        }

        document.close();
        return baos.toByteArray();
    }
    public byte[] generateInterviewReport(String candidateName, String candidateEmail, String role, Integer durationSeconds, InterviewResultResponse result) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

        Paragraph title = new Paragraph("SmartATS Interview Analytics Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Candidate Overview", sectionFont));
        document.add(new Paragraph("Candidate: " + safe(candidateName, "Candidate"), normalFont));
        document.add(new Paragraph("Email: " + safe(candidateEmail, "N/A"), normalFont));
        document.add(new Paragraph("Role: " + safe(role, "Interview"), normalFont));
        document.add(new Paragraph("Duration: " + formatDuration(durationSeconds), normalFont));
        document.add(new Paragraph("Generated At: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normalFont));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Scores", sectionFont));
        PdfPTable scoreTable = new PdfPTable(4);
        scoreTable.setWidthPercentage(100);
        scoreTable.addCell(scoreHeader("Overall", boldFont));
        scoreTable.addCell(scoreHeader("Communication", boldFont));
        scoreTable.addCell(scoreHeader("Technical", boldFont));
        scoreTable.addCell(scoreHeader("Confidence", boldFont));
        scoreTable.addCell(scoreValue(value(result.getOverallScore()), normalFont));
        scoreTable.addCell(scoreValue(value(result.getCommunicationScore()), normalFont));
        scoreTable.addCell(scoreValue(value(result.getTechnicalScore()), normalFont));
        scoreTable.addCell(scoreValue(value(result.getConfidenceScore()), normalFont));
        document.add(scoreTable);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Hiring Recommendation", sectionFont));
        document.add(new Paragraph(safe(result.getFinalFeedback(), "No recommendation available."), normalFont));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Summary", sectionFont));
        document.add(new Paragraph(safe(result.getSummary(), "No summary available."), normalFont));
        document.add(new Paragraph("\n"));

        addBulletSection(document, "Strengths", result.getStrengths(), normalFont, sectionFont);
        addBulletSection(document, "Weaknesses", result.getWeaknesses(), normalFont, sectionFont);
        addBulletSection(document, "Improvement Suggestions", result.getImprovementSuggestions(), normalFont, sectionFont);

        document.add(new Paragraph("Interview Integrity", sectionFont));
        document.add(new Paragraph("Integrity Status: " + safe(result.getIntegrityStatus(), "Unknown"), normalFont));
        document.add(new Paragraph("Tab Switches: " + value(result.getTabSwitchCount()), normalFont));
        document.add(new Paragraph("Face Warnings: " + value(result.getFaceWarningCount()), normalFont));
        document.add(new Paragraph("Suspicious Events: " + value(result.getSuspiciousActivityCount()), normalFont));
        document.add(new Paragraph("\n"));

        if (result.getQuestionBreakdown() != null && !result.getQuestionBreakdown().isEmpty()) {
            document.add(new Paragraph("Question Breakdown", sectionFont));
            PdfPTable breakdownTable = new PdfPTable(new float[]{4f, 1.2f, 1.2f, 1.2f});
            breakdownTable.setWidthPercentage(100);
            breakdownTable.addCell(scoreHeader("Question", boldFont));
            breakdownTable.addCell(scoreHeader("Clarity", boldFont));
            breakdownTable.addCell(scoreHeader("Technical", boldFont));
            breakdownTable.addCell(scoreHeader("Confidence", boldFont));

            for (InterviewQuestionBreakdownItem item : result.getQuestionBreakdown()) {
                breakdownTable.addCell(new Phrase(safe(item.getQuestion(), "Question"), normalFont));
                breakdownTable.addCell(new Phrase(value(item.getClarity()), normalFont));
                breakdownTable.addCell(new Phrase(value(item.getTechnicalAccuracy()), normalFont));
                breakdownTable.addCell(new Phrase(value(item.getConfidence()), normalFont));
            }
            document.add(breakdownTable);
        }

        document.close();
        return baos.toByteArray();
    }

    private void addBulletSection(Document document, String title, List<String> items, Font normalFont, Font sectionFont) throws Exception {
        document.add(new Paragraph(title, sectionFont));
        if (items == null || items.isEmpty()) {
            document.add(new Paragraph("- None", normalFont));
        } else {
            for (String item : items) {
                document.add(new Paragraph("- " + safe(item, ""), normalFont));
            }
        }
        document.add(new Paragraph("\n"));
    }

    private PdfPCell scoreHeader(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 236, 255));
        return cell;
    }

    private PdfPCell scoreValue(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String value(Integer number) {
        return number == null ? "0" : number + "%";
    }

    private String formatDuration(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return "0m";
        }
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return minutes + "m " + seconds + "s";
    }
}


