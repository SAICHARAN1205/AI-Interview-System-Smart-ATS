package com.aihiringplatform.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeOcrService {

    OcrAttemptResult attemptTextExtraction(MultipartFile file, String fileName, String mimeType);

    record OcrAttemptResult(
            boolean attempted,
            boolean extractedText,
            String text,
            String provider,
            String reason
    ) {
    }
}
