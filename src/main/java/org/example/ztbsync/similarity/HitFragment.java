package org.example.ztbsync.similarity;

/**
 * 两份投标文件命中的相似原文片段。
 */
public record HitFragment(
        String type,
        String reason,
        String leftOriginalText,
        String rightOriginalText,
        double similarity,
        boolean tenderDerived,
        double tenderSimilarity,
        double weight) {
}
