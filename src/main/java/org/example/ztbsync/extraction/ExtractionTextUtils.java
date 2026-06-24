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

    static String cleanCompanyName(String value) {
        String cleaned = cleanValue(value);
        if (cleaned == null) {
            return null;
        }
        for (int i = 0; i < 4; i++) {
            String before = cleaned;
            cleaned = cleaned.replaceAll("[（(]\\s*(?:单位)?(?:公章|盖章|签章|印章)\\s*[)）]", " ");
            cleaned = cleaned.replaceAll("^[\\s_＿?？:：,，;；.。·\\-—]+", "");
            cleaned = cleaned.replaceAll("[\\s_＿?？:：,，;；.。·\\-—]+$", "");
            cleaned = cleaned.trim();
            if (cleaned.equals(before)) {
                break;
            }
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
