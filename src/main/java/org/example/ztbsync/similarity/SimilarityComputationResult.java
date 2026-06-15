package org.example.ztbsync.similarity;

import java.util.List;

/**
 * 单组投标文件两两相似度计算结果。
 */
public record SimilarityComputationResult(
        double score,
        String riskLevel,
        double coverage,
        double averageSimilarity,
        double errorBonus,
        List<HitFragment> hitFragments) {
}
