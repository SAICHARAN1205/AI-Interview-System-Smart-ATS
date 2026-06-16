package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportServiceTest {

    private final PdfReportService pdfReportService = new PdfReportService();

    @Test
    void testGenerateGenericReport() throws Exception {
        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setAtsScore(85);
        response.setSummary("Great match for the role.");
        response.setFormattingQuality("Excellent readability");
        response.setMatchedKeywords(List.of("Java", "Spring Boot"));
        response.setMissingKeywords(List.of("Docker"));
        response.setOptimizationTips(List.of("Add more details about Docker."));

        byte[] pdfBytes = pdfReportService.generateGenericReport(response);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        
        // Check PDF header
        assertEquals(0x25, pdfBytes[0]); // %
        assertEquals(0x50, pdfBytes[1]); // P
        assertEquals(0x44, pdfBytes[2]); // D
        assertEquals(0x46, pdfBytes[3]); // F
    }
}
