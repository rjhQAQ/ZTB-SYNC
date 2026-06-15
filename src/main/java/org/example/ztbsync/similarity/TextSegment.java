package org.example.ztbsync.similarity;

import java.util.Set;

/**
 * 雷同分析使用的文本片段。
 */
public record TextSegment(
        int index,
        String originalText,
        String normalizedText,
        Set<String> grams,
        boolean tenderDerived,
        double tenderSimilarity,
        double weight) {

    public int weightedLength() {
        return (int) Math.round(normalizedText.length() * weight);
    }
}
