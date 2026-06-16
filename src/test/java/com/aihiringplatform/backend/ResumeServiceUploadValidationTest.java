package com.aihiringplatform.backend;

import com.aihiringplatform.backend.config.ResumeStorageProperties;
import com.aihiringplatform.backend.controller.PlatformAIController;
import com.aihiringplatform.backend.dto.ResumeAnalysisRequest;
import com.aihiringplatform.backend.dto.ResumeAnalysisResponse;
import com.aihiringplatform.backend.dto.ResumeFileResponse;
import com.aihiringplatform.backend.service.NoopResumeOcrService;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.service.AIService;
import com.aihiringplatform.backend.service.AiRateLimitService;
import com.aihiringplatform.backend.service.AnalyticsSnapshotService;
import com.aihiringplatform.backend.service.JobService;
import com.aihiringplatform.backend.service.ResumeService;
import com.aihiringplatform.backend.service.ApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class ResumeServiceUploadValidationTest {

    private static final String VALID_RESUME_TEXT = """
            ATS Candidate
            ats.candidate@example.com
            +1 (555) 123-4567

            Summary
            Backend engineer with Spring Boot, REST API, and SQL delivery experience.

            Skills
            Java, Spring Boot, REST APIs, SQL, Docker

            Experience
            Built internal hiring workflows and improved recruiter operations.

            Education
            Bachelor of Technology in Computer Science

            Projects
            Smart ATS platform with analytics and interview automation.
            """;

    private static final String MEDIUM_CONFIDENCE_RESUME_TEXT = """
            asha.rao@example.com
            B.Tech Computer Science, 2026
            Entry level backend developer focused on APIs and SQL.
            Java, SQL, Git
            """;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private com.aihiringplatform.backend.service.CloudStorageService cloudStorageService;

    @Mock
    private com.aihiringplatform.backend.service.ActivityLogService activityLogService;

    private ResumeService resumeService;
    private ResumeStorageProperties resumeStorageProperties;
    private User candidate;
    private AtomicReference<Resume> storedResume;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService();
        resumeStorageProperties = new ResumeStorageProperties();
        resumeStorageProperties.setStoragePath(tempDir.toString());
        resumeStorageProperties.setMaxSizeBytes(10 * 1024 * 1024L);

        setField(resumeService, "resumeRepository", resumeRepository);
        setField(resumeService, "userRepository", userRepository);
        setField(resumeService, "applicationRepository", applicationRepository);
        setField(resumeService, "applicationService", applicationService);
        setField(resumeService, "resumeStorageProperties", resumeStorageProperties);
        setField(resumeService, "resumeOcrService", new NoopResumeOcrService());
        setField(resumeService, "cloudStorageService", cloudStorageService);
        setField(resumeService, "activityLogService", activityLogService);

        candidate = new User();
        candidate.setId(1L);
        candidate.setName("ATS Candidate");
        candidate.setEmail("candidate@example.com");
        candidate.setPassword("password");
        candidate.setRole(Role.CANDIDATE);

        storedResume = new AtomicReference<>();

        lenient().when(userRepository.findByEmail(candidate.getEmail())).thenReturn(Optional.of(candidate));
        lenient().when(resumeRepository.findTopByUserIdOrderByUploadedAtDesc(candidate.getId()))
                .thenAnswer(invocation -> Optional.ofNullable(storedResume.get()));
        lenient().when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            if (resume.getId() == null) {
                resume.setId(100L);
            }
            storedResume.set(resume);
            return resume;
        });

        try {
            java.nio.file.Path fakeFile = tempDir.resolve("fake_resume.pdf");
            if (!java.nio.file.Files.exists(fakeFile)) {
                java.nio.file.Files.writeString(fakeFile, "fake content");
            }
            lenient().when(cloudStorageService.uploadFile(any(org.springframework.web.multipart.MultipartFile.class), anyString()))
                     .thenAnswer(invocation -> {
                         org.springframework.web.multipart.MultipartFile file = invocation.getArgument(0);
                         java.nio.file.Path target = tempDir.resolve("fake_resume_" + file.getOriginalFilename());
                         if (!java.nio.file.Files.exists(target)) {
                             java.nio.file.Files.writeString(target, "fake content");
                         }
                         return target.toAbsolutePath().toString();
                     });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void acceptsPdfUploadAndMakesResumeTextAvailableForAtsAnalysis() {
        uploadAndAnalyzeResume("resume.pdf", "application/pdf", "application/pdf");
    }

    @Test
    void recruiterCannotUploadResume() {
        User recruiter = new User();
        recruiter.setId(2L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setRole(Role.RECRUITER);
        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                VALID_RESUME_TEXT.getBytes(StandardCharsets.UTF_8)
        );

        try {
            resumeService.uploadResume(file, recruiter.getEmail());
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
            assertEquals("Only candidates can upload resumes.", exception.getReason());
            return;
        }

        throw new AssertionError("Expected the upload to be rejected for recruiters.");
    }

    @Test
    void acceptsDocUploadWithBrowserMimeAliasAndMakesResumeTextAvailableForAtsAnalysis() {
        uploadAndAnalyzeResume("resume.doc", "application/x-msword", "application/msword");
    }

    @Test
    void acceptsDocxUploadWithGenericMimeAndMakesResumeTextAvailableForAtsAnalysis() {
        uploadAndAnalyzeResume(
                "resume.docx",
                "application/octet-stream",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
    }

    @Test
    void rejectsInvalidExtensionWithCleanMessage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.png",
                "image/png",
                "not-a-resume".getBytes(StandardCharsets.UTF_8)
        );

        try {
            resumeService.uploadResume(file, candidate.getEmail());
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Please upload a PDF, DOC, or DOCX resume.", exception.getReason());
            return;
        }

        throw new AssertionError("Expected the upload to be rejected.");
    }

    @Test
    void rejectsBusTicketPdfAsNonResume() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bus-ticket.pdf",
                "application/pdf",
                """
                City Express Bus Ticket
                PNR 458921
                Departure 07:30
                Arrival 10:15
                Seat No 12A
                Total Paid 540 INR
                """.getBytes(StandardCharsets.UTF_8)
        );

        try {
            resumeService.uploadResume(file, candidate.getEmail());
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("This file does not appear to be a resume or CV. Please upload a valid resume document.", exception.getReason());
            return;
        }

        throw new AssertionError("Expected the upload to be rejected.");
    }

    @Test
    void rejectsBlankPdfWithFriendlyUnreadableMessage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "blank.pdf",
                "application/pdf",
                "   \n   ".getBytes(StandardCharsets.UTF_8)
        );

        try {
            resumeService.uploadResume(file, candidate.getEmail());
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("We could not read text from this file. Please upload a real resume PDF or DOCX with selectable text. Scanned/image-only PDFs are not supported.", exception.getReason());
            return;
        }

        throw new AssertionError("Expected the upload to be rejected.");
    }

    @Test
    void rejectsScreenshotStylePdfAsNonResume() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screenshot.pdf",
                "application/pdf",
                """
                Screenshot captured on 2026-05-27
                Screen shot from candidate portal
                """.getBytes(StandardCharsets.UTF_8)
        );

        try {
            resumeService.uploadResume(file, candidate.getEmail());
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("This file does not appear to be a resume or CV. Please upload a valid resume document.", exception.getReason());
            return;
        }

        throw new AssertionError("Expected the upload to be rejected.");
    }

    @Test
    void acceptsMediumConfidenceResumeAndReturnsWarningWhileContinuingAtsAnalysis() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "compact-resume.pdf",
                "application/pdf",
                MEDIUM_CONFIDENCE_RESUME_TEXT.getBytes(StandardCharsets.UTF_8)
        );

        ResumeFileResponse response = resumeService.uploadResume(file, candidate.getEmail());
        assertEquals("medium", response.getResumeValidationStatus());
        assertTrue(response.getResumeConfidenceScore() >= 50 && response.getResumeConfidenceScore() < 80);
        assertEquals(
                "This resume may have limited ATS readability, but analysis can continue.",
                response.getMessage()
        );

        AIService aiService = mock(AIService.class);
        AiRateLimitService aiRateLimitService = mock(AiRateLimitService.class);
        JobService jobService = mock(JobService.class);
        AnalyticsSnapshotService analyticsSnapshotService = mock(AnalyticsSnapshotService.class);
        PlatformAIController controller = new PlatformAIController(
                aiService,
                aiRateLimitService,
                resumeService,
                jobService,
                analyticsSnapshotService
        );

        doNothing().when(aiRateLimitService).assertAllowed(anyString(), anyString());
        when(aiService.analyzeResume(any(ResumeAnalysisRequest.class))).thenAnswer(invocation -> {
            ResumeAnalysisResponse analysisResponse = new ResumeAnalysisResponse();
            analysisResponse.setAtsScore(84);
            analysisResponse.setSummary("ATS analysis completed.");
            return analysisResponse;
        });

        ResumeAnalysisRequest request = new ResumeAnalysisRequest();
        request.setTargetRole("Backend Engineer");
        request.setJobDescription("Need Java, SQL, and Spring Boot.");

        Authentication authentication = mock(Authentication.class);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(authentication.getName()).thenReturn(candidate.getEmail());

        Object body = controller.analyzeResume(request, authentication, httpServletRequest).getBody();
        assertInstanceOf(ResumeAnalysisResponse.class, body);
        ResumeAnalysisResponse analysisResponse = (ResumeAnalysisResponse) body;
        assertEquals(84, analysisResponse.getAtsScore());
        assertEquals("medium", analysisResponse.getResumeValidationStatus());
        assertEquals(
                "This resume may have limited ATS readability, but analysis can continue.",
                analysisResponse.getMessage()
        );
    }

    private void uploadAndAnalyzeResume(String fileName, String contentType, String expectedMimeType) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                contentType,
                VALID_RESUME_TEXT.getBytes(StandardCharsets.UTF_8)
        );

        ResumeFileResponse response = resumeService.uploadResume(file, candidate.getEmail());
        assertEquals(fileName, response.getFileName());
        assertEquals(expectedMimeType, response.getMimeType());
        assertTrue(response.isHasResume());
        assertNotNull(response.getFilePath());
        assertEquals("high", response.getResumeValidationStatus());
        assertTrue(response.getResumeConfidenceScore() >= 80);

        String extractedResumeText = resumeService.getResumeTextForCandidate(candidate.getEmail());
        assertFalse(extractedResumeText.isBlank());

        AIService aiService = mock(AIService.class);
        AiRateLimitService aiRateLimitService = mock(AiRateLimitService.class);
        JobService jobService = mock(JobService.class);
        AnalyticsSnapshotService analyticsSnapshotService = mock(AnalyticsSnapshotService.class);
        PlatformAIController controller = new PlatformAIController(
                aiService,
                aiRateLimitService,
                resumeService,
                jobService,
                analyticsSnapshotService
        );

        doNothing().when(aiRateLimitService).assertAllowed(anyString(), anyString());
        when(aiService.analyzeResume(any(ResumeAnalysisRequest.class))).thenAnswer(invocation -> {
            ResumeAnalysisRequest request = invocation.getArgument(0);
            ResumeAnalysisResponse analysisResponse = new ResumeAnalysisResponse();
            analysisResponse.setAtsScore(request.getResumeText() != null && !request.getResumeText().isBlank() ? 91 : 0);
            analysisResponse.setSummary("ATS analysis completed.");
            return analysisResponse;
        });

        ResumeAnalysisRequest request = new ResumeAnalysisRequest();
        request.setTargetRole("Backend Engineer");
        request.setJobDescription("Need Java, Spring Boot, SQL, and API design.");

        Authentication authentication = mock(Authentication.class);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(authentication.getName()).thenReturn(candidate.getEmail());

        Object body = controller.analyzeResume(request, authentication, httpServletRequest).getBody();
        assertInstanceOf(ResumeAnalysisResponse.class, body);
        ResumeAnalysisResponse analysisResponse = (ResumeAnalysisResponse) body;
        assertEquals(91, analysisResponse.getAtsScore());
        assertEquals("ATS analysis completed.", analysisResponse.getSummary());
        assertEquals("high", analysisResponse.getResumeValidationStatus());
        assertTrue(analysisResponse.getResumeConfidenceScore() >= 80);
        assertTrue(storedResume.get().getFilePath().endsWith(expectedStoredExtension(fileName)));
    }

    private String expectedStoredExtension(String fileName) {
        return "." + fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
