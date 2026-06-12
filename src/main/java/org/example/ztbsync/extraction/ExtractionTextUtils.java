package org.example.ztbsync.extraction;

import java.util.Arrays;
import java.util.List;

final class ExtractionTextUtils {

    private ExtractionTextUtils() {
    }

    static List<String> lines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.replace("\r\n", "\n").replace('\r', '\n').split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
                .replace('\u00a0', ' ')
                .replaceAll("[|｜]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = cleaned.replaceAll("^[：:：\\s]+", "");
        cleaned = cleaned.replaceAll("[。；;，,\\s]+$", "");
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200).trim();
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String firstText(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }
}
