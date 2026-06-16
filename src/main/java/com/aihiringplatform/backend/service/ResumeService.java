package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.config.ResumeStorageProperties;
import com.aihiringplatform.backend.dto.ResumeFileResponse;
import com.aihiringplatform.backend.dto.ResumeStatusResponse;
import com.aihiringplatform.backend.entity.Resume;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.ApplicationRepository;
import com.aihiringplatform.backend.repository.ResumeRepository;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.util.ResumeValidationUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> GENERIC_MIME_TYPES = Set.of(
            "application/octet-stream",
            "binary/octet-stream"
    );
    private static final Set<String> PDF_MIME_TYPES = Set.of(
            "application/pdf",
            "application/x-pdf",
            "application/acrobat",
            "text/pdf"
    );
    private static final Set<String> WORD_MIME_TYPES = Set.of(
            "application/msword",
            "application/x-msword",
            "application/vnd.ms-word",
            "application/vnd.msword",
            "application/doc",
            "application/x-doc",
            "application/word",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/x-tika-msoffice",
            "application/x-tika-ooxml",
            "application/zip",
            "application/x-zip-compressed"
    );
    private static final Map<String, String> CANONICAL_MIME_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final String INVALID_RESUME_MESSAGE = "Please upload a PDF, DOC, or DOCX resume.";
    private static final String UNREADABLE_RESUME_MESSAGE = "We could not read text from this file. Please upload a real resume PDF or DOCX with selectable text. Scanned/image-only PDFs are not supported.";
    private static final String NON_RESUME_MESSAGE = "This file does not appear to be a resume or CV. Please upload a valid resume document.";

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ResumeStorageProperties resumeStorageProperties;

    @Autowired
    private ResumeOcrService resumeOcrService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private CloudStorageService cloudStorageService;

    @Autowired
    private ActivityLogService activityLogService;

    @Transactional
    public ResumeFileResponse uploadResume(MultipartFile file, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));

        if (user.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only candidates can upload resumes.");
        }

        validateResumeFile(file);

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = getExtension(originalFileName);
        String resolvedMimeType = resolveMimeType(file, originalFileName);
        Tika tika = new Tika();
        TextExtractionResult extractionResult = extractTextSafely(file, tika, originalFileName, resolvedMimeType);
        ValidatedResumeText validatedResume = assertValidResumeContent(originalFileName, extractionResult, "resume-upload");
        String extractedText = validatedResume.text();
        
        String fileHash = computeFileHash(file);
        
        Resume resume = resumeRepository.findTopByUserIdOrderByUploadedAtDesc(user.getId())
                .orElseGet(Resume::new);

        if (resume.getFileHash() != null && resume.getFileHash().equals(fileHash)) {
            logger.info("Candidate {} attempted to upload the exact same resume. Skipped duplicate processing.", email);
            return toResumeFileResponse(resume, validatedResume.validationResult());
        }

        String cloudUrl;
        try {
            cloudUrl = cloudStorageService.uploadFile(file, "resumes/" + user.getId());
        } catch (IOException exception) {
            logger.error("Failed to store resume for user {}", user.getEmail(), exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Resume upload failed. Please try again.");
        }

        if (resume.getFilePath() != null) {
            String existingPath = resume.getFilePath();
            if (existingPath.startsWith("s3://") || existingPath.startsWith("http://") || existingPath.startsWith("https://") || existingPath.startsWith("local-mock-path/")) {
                cloudStorageService.deleteFile(existingPath);
            } else {
                deleteStoredFileIfPresent(existingPath, resolveStorageRoot());
            }
        }

        if (cloudUrl != null && cloudUrl.startsWith("local-mock-path/")) {
            try {
                Path storageDir = resolveStorageRoot();
                if (!Files.exists(storageDir)) {
                    Files.createDirectories(storageDir);
                }
                String uniqueFileName = UUID.randomUUID() + "_" + originalFileName;
                Path targetLocation = storageDir.resolve(uniqueFileName);
                file.transferTo(targetLocation.toFile());
                
                resume.setFilePath(targetLocation.toAbsolutePath().normalize().toString());
                logger.info("Saved resume binary data locally to file system: {}", targetLocation);
            } catch (IOException e) {
                logger.error("Failed to save resume locally. Falling back to mock URL.", e);
                resume.setFilePath(cloudUrl);
            }
        } else {
            resume.setFilePath(cloudUrl);
        }

        resume.setFileName(originalFileName);
        resume.setMimeType(resolvedMimeType);
        resume.setUploadedAt(LocalDateTime.now());
        resume.setFileHash(fileHash);
        resume.setData(null); // Ensure db is not bloated

        resume.setExtractedText(extractedText);
        resume.setUser(user);

        Resume savedResume = resumeRepository.save(resume);
        
        // Schedule background score recalculation AFTER this transaction commits
        applicationService.scheduleScoreUpdateAfterCommit(user);

        activityLogService.logSuccess(email, user.getRole().name(), "RESUME_UPLOAD", "Candidate successfully uploaded resume: " + originalFileName, null);

        return toResumeFileResponse(savedResume, validatedResume.validationResult());
    }

    @Transactional(readOnly = true)
    public ResumeStatusResponse hasResumeForCandidate(Long candidateId, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can view candidate resumes.");
        }

        boolean recruiterOwnsApplication = applicationRepository.existsByCandidateIdAndJobRecruiterEmail(candidateId, recruiterEmail);
        if (!recruiterOwnsApplication) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access resumes for your applicants.");
        }

        ResumeStatusResponse response = new ResumeStatusResponse();
        response.setCandidateId(candidateId);
        response.setHasResume(hasAccessibleResume(candidateId));
        response.setMessage(response.isHasResume() ? "Resume available" : "Resume not uploaded");
        return response;
    }

    @Transactional(readOnly = true)
    public ResumeFileResponse getCurrentUserResume(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));

        Resume resume = findAccessibleResumeByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not uploaded."));

        return toResumeFileResponse(resume);
    }

    @Transactional(readOnly = true)
    public ResumeFileResponse getCandidateResumeForRecruiter(Long candidateId, String recruiterEmail) {
        assertRecruiterOwnsCandidateApplication(candidateId, recruiterEmail);
        Resume resume = findAccessibleResumeByUserId(candidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not uploaded."));
        return toResumeFileResponse(resume);
    }

    @Transactional(readOnly = true)
    public DownloadableResume downloadCurrentUserResume(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));
        Resume resume = findAccessibleResumeByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not uploaded."));
        return toDownloadableResume(resume);
    }

    @Transactional(readOnly = true)
    public DownloadableResume downloadResumeForCandidate(Long candidateId, String recruiterEmail) {
        assertRecruiterOwnsCandidateApplication(candidateId, recruiterEmail);
        Resume resume = findAccessibleResumeByUserId(candidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not uploaded."));
        return toDownloadableResume(resume);
    }

    public String getResumeTextForCandidate(String candidateEmail) {
        return getValidatedResumeTextForCandidate(candidateEmail).text();
    }

    public ValidatedResumeText getValidatedResumeTextForCandidate(String candidateEmail) {
        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate account was not found."));

        Resume resume = findAccessibleResumeByUserId(candidate.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not uploaded."));

        TextExtractionResult storedResumeText = new TextExtractionResult(
                resume.getExtractedText() == null ? "" : resume.getExtractedText().trim(),
                "stored-resume-text",
                null,
                false,
                "ocr-placeholder",
                "Stored resume text is reused until OCR support is enabled."
        );

        return assertValidResumeContent(resume.getFileName(), storedResumeText, "ats-analysis");
    }

    public ValidatedResumeText validateResumeTextForAtsAnalysis(String resumeText, String sourceLabel) {
        TextExtractionResult inlineResumeText = new TextExtractionResult(
                resumeText == null ? "" : resumeText.trim(),
                "inline-resume-text",
                null,
                false,
                "inline-resume-text",
                "Resume text supplied directly in ATS analysis request."
        );
        return assertValidResumeContent(sourceLabel, inlineResumeText, "ats-analysis");
    }

    private void assertRecruiterOwnsCandidateApplication(Long candidateId, String recruiterEmail) {
        User recruiter = userRepository.findByEmail(recruiterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter account was not found."));

        if (recruiter.getRole() != Role.RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can access candidate resumes.");
        }

        boolean recruiterOwnsApplication = applicationRepository.existsByCandidateIdAndJobRecruiterEmail(candidateId, recruiterEmail);
        if (!recruiterOwnsApplication) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access resumes for your applicants.");
        }
    }

    private boolean hasAccessibleResume(Long userId) {
        return findAccessibleResumeByUserId(userId).isPresent();
    }

    private java.util.Optional<Resume> findAccessibleResumeByUserId(Long userId) {
        return resumeRepository.findTopByUserIdOrderByUploadedAtDesc(userId)
                .filter(this::hasStoredContent);
    }

    private boolean hasStoredContent(Resume resume) {
        if (resume == null) {
            return false;
        }

        if (resume.getFilePath() != null && !resume.getFilePath().isBlank()) {
            String path = resume.getFilePath();
            if (path.startsWith("s3://") || path.startsWith("http://") || path.startsWith("https://") || path.startsWith("local-mock-path/")) {
                return true;
            }
            return Files.exists(Paths.get(path));
        }

        return resume.getData() != null && resume.getData().length > 0;
    }

    private void validateResumeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required.");
        }

        if (file.getSize() > resumeStorageProperties.getMaxSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is too large. Maximum size is 10 MB.");
        }

        String sanitizedName = sanitizeFileName(file.getOriginalFilename());
        String extension = getExtension(sanitizedName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_RESUME_MESSAGE);
        }

        String mimeType = normalizeMimeType(file.getContentType());
        if (mimeType != null && !mimeType.isBlank() && !GENERIC_MIME_TYPES.contains(mimeType) && !isAllowedMimeType(extension, mimeType)) {
            logger.warn("Rejected resume upload for {} with extension {} and content type {}", sanitizedName, extension, mimeType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_RESUME_MESSAGE);
        }
    }

    private TextExtractionResult extractTextSafely(MultipartFile file, Tika tika, String fileName, String mimeType) {
        String parserUsed = resolveParserUsed(fileName);
        try {
            String extractedText = tika.parseToString(file.getInputStream());
            String normalizedText = extractedText == null ? "" : extractedText.trim();
            if (!normalizedText.isBlank()) {
                return new TextExtractionResult(normalizedText, parserUsed, null, false, "ocr-placeholder", null);
            }

            String fallbackText = extractPlainTextFallback(file);
            if (!fallbackText.isBlank()) {
                return new TextExtractionResult(fallbackText, parserUsed + "+plain-text-fallback", "no_extractable_text", false, "ocr-placeholder", null);
            }
        } catch (Exception exception) {
            logger.warn("Resume text extraction failed for {}", file.getOriginalFilename(), exception);
            String fallbackText = extractPlainTextFallback(file);
            if (!fallbackText.isBlank()) {
                return new TextExtractionResult(
                        fallbackText,
                        parserUsed + "+plain-text-fallback",
                        exception.getClass().getSimpleName(),
                        false,
                        "ocr-placeholder",
                        null
                );
            }

            ResumeOcrService.OcrAttemptResult ocrAttempt = resumeOcrService.attemptTextExtraction(file, fileName, mimeType);
            if (ocrAttempt.extractedText() && ocrAttempt.text() != null && !ocrAttempt.text().trim().isBlank()) {
                return new TextExtractionResult(
                        ocrAttempt.text().trim(),
                        parserUsed,
                        exception.getClass().getSimpleName(),
                        ocrAttempt.attempted(),
                        ocrAttempt.provider(),
                        ocrAttempt.reason()
                );
            }
            return new TextExtractionResult(
                    "",
                    parserUsed,
                    exception.getClass().getSimpleName(),
                    ocrAttempt.attempted(),
                    ocrAttempt.provider(),
                    ocrAttempt.reason()
            );
        }

        ResumeOcrService.OcrAttemptResult ocrAttempt = resumeOcrService.attemptTextExtraction(file, fileName, mimeType);
        if (ocrAttempt.extractedText() && ocrAttempt.text() != null && !ocrAttempt.text().trim().isBlank()) {
            return new TextExtractionResult(
                    ocrAttempt.text().trim(),
                    parserUsed,
                    "no_extractable_text",
                    ocrAttempt.attempted(),
                    ocrAttempt.provider(),
                    ocrAttempt.reason()
            );
        }

        return new TextExtractionResult(
                "",
                parserUsed,
                "no_extractable_text",
                ocrAttempt.attempted(),
                ocrAttempt.provider(),
                ocrAttempt.reason()
        );
    }

    private String extractPlainTextFallback(MultipartFile file) {
        try {
            String decoded = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            if (decoded.isBlank()) {
                return "";
            }

            long printableCharacters = decoded.chars()
                    .filter(character -> character == '\n'
                            || character == '\r'
                            || character == '\t'
                            || (character >= 32 && character <= 126)
                            || Character.isLetterOrDigit(character))
                    .count();

            double printableRatio = decoded.isEmpty() ? 0 : (double) printableCharacters / decoded.length();
            return printableRatio >= 0.75 ? decoded : "";
        } catch (IOException exception) {
            logger.debug("Plain-text fallback extraction failed for {}", file.getOriginalFilename(), exception);
            return "";
        }
    }

    private ValidatedResumeText assertValidResumeContent(String fileName, TextExtractionResult extractionResult, String stage) {
        ResumeValidationUtils.ResumeValidationResult validationResult =
                ResumeValidationUtils.analyze(extractionResult.text());

        if (extractionResult.text().isBlank()) {
            logger.warn(
                    "Rejected resume {} during {}: parserUsed={} extractedTextLength={} extractionFailureReason={} detectedResumeSections={} confidenceScore={} rejectionReason={} suspiciousIndicators={} ocrAttempted={} ocrProvider={} ocrReason={}",
                    fileName,
                    stage,
                    extractionResult.parserUsed(),
                    extractionResult.extractedTextLength(),
                    extractionResult.failureReason(),
                    validationResult.detectedSections(),
                    validationResult.resumeConfidenceScore(),
                    validationResult.rejectionReason(),
                    validationResult.suspiciousIndicators(),
                    extractionResult.ocrAttempted(),
                    extractionResult.ocrProvider(),
                    extractionResult.ocrReason()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNREADABLE_RESUME_MESSAGE);
        }

        if (!validationResult.likelyResume()) {
            logger.warn(
                    "Rejected resume {} during {}: parserUsed={} extractedTextLength={} extractionFailureReason={} detectedResumeSections={} confidenceScore={} rejectionReason={} suspiciousIndicators={} ocrAttempted={} ocrProvider={} ocrReason={}",
                    fileName,
                    stage,
                    extractionResult.parserUsed(),
                    extractionResult.extractedTextLength(),
                    extractionResult.failureReason(),
                    validationResult.detectedSections(),
                    validationResult.resumeConfidenceScore(),
                    validationResult.rejectionReason(),
                    validationResult.suspiciousIndicators(),
                    extractionResult.ocrAttempted(),
                    extractionResult.ocrProvider(),
                    extractionResult.ocrReason()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, NON_RESUME_MESSAGE);
        }

        logger.info(
                "Accepted resume {} during {}: parserUsed={} extractedTextLength={} extractionFailureReason={} detectedResumeSections={} confidenceScore={} validationStatus={} warningMessage={} ocrAttempted={} ocrProvider={} ocrReason={}",
                fileName,
                stage,
                extractionResult.parserUsed(),
                extractionResult.extractedTextLength(),
                extractionResult.failureReason(),
                validationResult.detectedSections(),
                validationResult.resumeConfidenceScore(),
                validationResult.confidenceBand(),
                validationResult.warningMessage(),
                extractionResult.ocrAttempted(),
                extractionResult.ocrProvider(),
                extractionResult.ocrReason()
        );

        return new ValidatedResumeText(extractionResult.text(), validationResult);
    }

    private String computeFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.warn("Could not compute file hash", e);
            return UUID.randomUUID().toString(); // Fallback so we don't crash
        }
    }

    private Path resolveStorageRoot() {
        return Paths.get(resumeStorageProperties.getStoragePath()).toAbsolutePath().normalize();
    }

    private String sanitizeFileName(String originalFileName) {
        String fallback = "resume.pdf";
        if (originalFileName == null || originalFileName.isBlank()) {
            return fallback;
        }

        String fileName = Paths.get(originalFileName).getFileName().toString().trim();
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private String getExtension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        if (separator < 0 || separator == fileName.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_RESUME_MESSAGE);
        }

        return fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveMimeType(MultipartFile file, String fileName) {
        String extension = getExtension(fileName);
        String mimeType = normalizeMimeType(file.getContentType());

        if (mimeType != null && !mimeType.isBlank() && !GENERIC_MIME_TYPES.contains(mimeType) && isAllowedMimeType(extension, mimeType)) {
            return CANONICAL_MIME_TYPES.get(extension);
        }

        return CANONICAL_MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }

        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(';');
        return separator >= 0 ? normalized.substring(0, separator).trim() : normalized;
    }

    private boolean isAllowedMimeType(String extension, String mimeType) {
        if ("pdf".equals(extension)) {
            return PDF_MIME_TYPES.contains(mimeType);
        }

        return WORD_MIME_TYPES.contains(mimeType) || ALLOWED_MIME_TYPES.contains(mimeType);
    }

    private String resolveParserUsed(String fileName) {
        String extension = getExtension(fileName);
        return switch (extension) {
            case "pdf" -> "apache-tika-pdf";
            case "doc" -> "apache-tika-doc";
            case "docx" -> "apache-tika-docx";
            default -> "apache-tika";
        };
    }

    private void deleteStoredFileIfPresent(String filePath, Path storageRoot) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            Path candidatePath = Paths.get(filePath).toAbsolutePath().normalize();
            if (candidatePath.startsWith(storageRoot) && Files.exists(candidatePath)) {
                Files.delete(candidatePath);
            }
        } catch (Exception exception) {
            logger.warn("Unable to delete previous resume file {}", filePath, exception);
        }
    }

    private DownloadableResume toDownloadableResume(Resume resume) {
        try {
            if (resume.getFilePath() != null && !resume.getFilePath().isBlank()) {
                String path = resume.getFilePath();
                if (path.startsWith("s3://") || path.startsWith("http://") || path.startsWith("https://") || path.startsWith("local-mock-path/")) {
                    if (resume.getData() != null && resume.getData().length > 0) {
                        logger.info("Serving stored resume binary data for cloud/mock path: {}", resume.getFileName());
                        return new DownloadableResume(
                                resume.getFileName(),
                                resume.getMimeType() == null ? "application/octet-stream" : resume.getMimeType(),
                                resume.getData()
                        );
                    }
                } else {
                    Path storedPath = Paths.get(path);
                    if (Files.exists(storedPath)) {
                        logger.info("Serving local file for resume: {}", resume.getFileName());
                        return new DownloadableResume(
                                resume.getFileName(),
                                resume.getMimeType(),
                                Files.readAllBytes(storedPath)
                        );
                    }
                }
            }

            if (resume.getData() != null && resume.getData().length > 0) {
                logger.info("Serving stored resume binary data directly: {}", resume.getFileName());
                return new DownloadableResume(
                        resume.getFileName(),
                        resume.getMimeType() == null ? "application/octet-stream" : resume.getMimeType(),
                        resume.getData()
                );
            }
            
            logger.warn("Could not find resume binary for {}, falling back to extracted plain text.", resume.getFileName());
            return new DownloadableResume(
                    resume.getFileName() + ".txt",
                    "text/plain",
                    resume.getExtractedText() != null ? resume.getExtractedText().getBytes() : new byte[0]
            );

        } catch (IOException exception) {
            logger.error("Failed to load stored resume {}", resume.getFilePath(), exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Resume unavailable.");
        }
    }

    private ResumeFileResponse toResumeFileResponse(Resume resume) {
        ResumeValidationUtils.ResumeValidationResult validationResult = resume == null || resume.getExtractedText() == null
                ? null
                : ResumeValidationUtils.analyze(resume.getExtractedText());
        return toResumeFileResponse(resume, validationResult);
    }

    private ResumeFileResponse toResumeFileResponse(Resume resume, ResumeValidationUtils.ResumeValidationResult validationResult) {
        ResumeFileResponse response = new ResumeFileResponse();
        response.setId(resume.getId());
        response.setUserId(resume.getUser() == null ? null : resume.getUser().getId());
        response.setFileName(resume.getFileName());
        response.setMimeType(resume.getMimeType());
        response.setFilePath(resume.getFilePath());
        response.setUploadedAt(resume.getUploadedAt());
        response.setHasResume(hasStoredContent(resume));
        if (validationResult != null) {
            response.setResumeConfidenceScore(validationResult.resumeConfidenceScore());
            response.setResumeValidationStatus(validationResult.confidenceBand());
            response.setDetectedResumeSignals(validationResult.detectedSections());
            response.setMessage(validationResult.warningMessage());
        }
        return response;
    }

    public record DownloadableResume(String fileName, String mimeType, byte[] content) {
    }

    private record TextExtractionResult(
            String text,
            String parserUsed,
            String failureReason,
            boolean ocrAttempted,
            String ocrProvider,
            String ocrReason
    ) {
        private int extractedTextLength() {
            return text == null ? 0 : text.length();
        }
    }

    public record ValidatedResumeText(
            String text,
            ResumeValidationUtils.ResumeValidationResult validationResult
    ) {
    }
}
