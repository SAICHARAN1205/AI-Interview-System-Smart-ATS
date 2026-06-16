package com.aihiringplatform.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoopResumeOcrService implements ResumeOcrService {

    @Override
    public OcrAttemptResult attemptTextExtraction(MultipartFile file, String fileName, String mimeType) {
        return new OcrAttemptResult(
                false,
                false,
                "",
                "ocr-placeholder",
                "OCR support is not enabled yet for scanned/image-only resume documents."
        );
    }
}
