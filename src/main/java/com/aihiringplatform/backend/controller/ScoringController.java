package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.ScoreResponse;
import com.aihiringplatform.backend.entity.Application;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.service.MatchingService;
import com.aihiringplatform.backend.service.ScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.aihiringplatform.backend.service.PdfReportService;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/score")
public class ScoringController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScoringController.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private ScoringService scoringService;

    @GetMapping("/{applicationId}")
    public ResponseEntity<?> getScore(@PathVariable Long applicationId) {
        try {
            Application application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return ResponseEntity.badRequest().body("Application not found");

            int matchScore = 0;
            if (application.getMatchScore() != null) {
                // Return cached score
                matchScore = application.getMatchScore();
                logger.info("ATS score loaded from DB for application {}", applicationId);
                // If atsBreakdownJson is missing but matchScore is present, we still need to calculate final score
            } else {
                Resume candidateResume = resumeRepository.findTopByUserIdOrderByUploadedAtDesc(application.getCandidate().getId())
                        .orElse(null);

                if (candidateResume != null && candidateResume.getExtractedText() != null) {
                    com.aihiringplatform.backend.dto.MatchResponse matchResp = matchingService.calculateDetailedMatch(
                            candidateResume.getExtractedText(), 
                            application.getJob()
                    );
                    matchScore = matchResp.getScore();
                    application.setMatchScore(matchScore);
                    logger.info("ATS score calculated for application {}: {}", applicationId, matchScore);
                    
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        application.setAtsBreakdownJson(mapper.writeValueAsString(matchResp));
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Fixed assumed core performance
            int interviewPerformance = 85; 

            double finalScore = scoringService.calculateScore(matchScore, interviewPerformance);
            application.setAtsScore((int) finalScore);
            
            applicationRepository.save(application);

            ScoreResponse response = new ScoreResponse();
            response.setApplicationId(applicationId);
            response.setScore(finalScore);
            response.setRemark(finalScore >= 70 ? "SELECTED" : "REJECTED");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error calculating score: " + e.getMessage());
        }
    }
    @Autowired
    private PdfReportService pdfReportService;

    @GetMapping("/report/{applicationId}")
    public ResponseEntity<byte[]> getAtsReport(@PathVariable Long applicationId) {
        try {
            Application application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return ResponseEntity.badRequest().body(null);

            byte[] pdfBytes = pdfReportService.generateAtsReport(application);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            String filename = "SmartATS_Report_" + (application.getCandidate() != null ? application.getCandidate().getName().replaceAll(" ", "_") : "Candidate") + ".pdf";
            headers.setContentDispositionFormData("filename", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
    @PostMapping("/report/generate")
    public ResponseEntity<byte[]> generateAtsReportGeneric(@RequestBody com.aihiringplatform.backend.dto.ResumeAnalysisResponse payload) {
        try {
            byte[] pdfBytes = pdfReportService.generateGenericReport(payload);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            String filename = "SmartATS_Report_Candidate.pdf";
            headers.setContentDispositionFormData("filename", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
