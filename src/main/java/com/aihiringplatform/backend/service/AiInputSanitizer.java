package com.aihiringplatform.backend.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AiInputSanitizer {

    public String sanitizeText(String value, int maxChars) {
        if (value == null) {
            return "";
        }

        String cleaned = value
                .replace("\r", "\n")
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();

        if (cleaned.length() <= maxChars) {
            return cleaned;
        }

        return cleaned.substring(0, maxChars).trim();
    }

    public String sanitizeOption(String value, String fallback, int maxChars) {
        String cleaned = sanitizeText(value, maxChars);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    public List<String> sanitizeSkills(List<String> values, int maxItems, int maxCharsPerValue) {
        Set<String> normalized = new LinkedHashSet<>();

        if (values != null) {
            for (String value : values) {
                String cleaned = sanitizeText(value, maxCharsPerValue);
                if (!cleaned.isBlank()) {
                    normalized.add(cleaned);
                }
                if (normalized.size() >= maxItems) {
                    break;
                }
            }
        }

        return new ArrayList<>(normalized);
    }

    public boolean containsText(String value) {
        return value != null && !sanitizeText(value, Math.max(1, value.length())).isBlank();
    }

    public String wrapUntrustedText(String label, String value) {
        String cleanedLabel = sanitizeText(label, 40).toUpperCase(Locale.ROOT).replace(' ', '_');
        String cleanedValue = value == null ? "" : value;
        return "BEGIN_" + cleanedLabel + "\n" + cleanedValue + "\nEND_" + cleanedLabel;
    }
}
