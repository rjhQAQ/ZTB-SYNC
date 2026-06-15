package org.example.ztbsync.similarity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.example.ztbsync.config.ZtbProperties;
import org.springframework.stereotype.Component;

@Component
public class TextSegmenter {

    private static final Pattern LINE_SPLITTER = Pattern.compile("\\R+");
    private static final Pattern KEEP_TEXT = Pattern.compile("[^\\p{IsHan}A-Za-z0-9]");
    private static final List<String> FORMALIZED_KEYWORDS = List.of(
            "目录", "页码", "投标函", "授权委托书", "法定代表人", "声明", "承诺函",
            "签字", "盖章", "日期", "格式", "附件", "目录页", "封面", "正本", "副本");

    private final ZtbProperties properties;

    public TextSegmenter(ZtbProperties properties) {
        this.properties = properties;
    }

    public List<TextSegment> segment(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<TextSegment> segments = new ArrayList<>();
        String[] lines = LINE_SPLITTER.split(text);
        for (String line : lines) {
            String original = cleanOriginal(line);
            if (original.isBlank() || isFormalized(original)) {
                continue;
            }
            String normalized = normalize(original);
            if (normalized.length() < properties.getSimilarity().getMinSegmentChars()) {
                continue;
            }
            segments.add(new TextSegment(
                    segments.size(),
                    original,
                    normalized,
                    ngrams(normalized),
                    false,
                    0,
                    1.0));
        }
        return segments;
    }

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return KEEP_TEXT.matcher(normalized).replaceAll("");
    }

    public Set<String> ngrams(String normalizedText) {
        Set<String> grams = new LinkedHashSet<>();
        if (normalizedText == null || normalizedText.isBlank()) {
            return grams;
        }
        if (normalizedText.length() <= 3) {
            grams.add(normalizedText);
            return grams;
        }
        for (int i = 0; i <= normalizedText.length() - 3; i++) {
            grams.add(normalizedText.substring(i, i + 3));
        }
        return grams;
    }

    private String cleanOriginal(String line) {
        return line == null
                ? ""
                : line.replace('\u00a0', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();
    }

    private boolean isFormalized(String original) {
        String normalized = normalize(original);
        if (normalized.matches("^第?\\d+页共?\\d*页?$")) {
            return true;
        }
        if (normalized.length() <= 12) {
            return FORMALIZED_KEYWORDS.stream().anyMatch(normalized::contains);
        }
        long matched = FORMALIZED_KEYWORDS.stream().filter(normalized::contains).count();
        return matched >= 2 && normalized.length() < 30;
    }
}
