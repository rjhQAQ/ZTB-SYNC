package org.example.ztbsync.rag;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.ztbsync.config.ZtbProperties;
import org.springframework.stereotype.Component;

/**
 * 使用 Reciprocal Rank Fusion 合并向量召回和关键词召回。
 */
@Component
public class RagHybridRanker {

    private final ZtbProperties properties;

    public RagHybridRanker(ZtbProperties properties) {
        this.properties = properties;
    }

    public List<RagSearchHit> rank(List<RagSearchHit> vectorHits, List<RagSearchHit> keywordHits, int limit) {
        Map<String, ScoredHit> merged = new LinkedHashMap<>();
        add(merged, vectorHits, properties.getRagSearch().getVectorWeight());
        add(merged, keywordHits, properties.getRagSearch().getKeywordWeight());
        double maxScore = maxPossibleScore();
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(ScoredHit::score).reversed())
                .limit(Math.max(1, limit))
                .map(scored -> scored.hit().withScore(normalize(scored.score(), maxScore)))
                .toList();
    }

    private void add(Map<String, ScoredHit> merged, List<RagSearchHit> hits, double weight) {
        if (hits == null || hits.isEmpty() || weight <= 0) {
            return;
        }
        int rankConstant = Math.max(1, properties.getRagSearch().getRrfRankConstant());
        for (int i = 0; i < hits.size(); i++) {
            RagSearchHit hit = hits.get(i);
            String key = hit.key();
            double score = weight / (rankConstant + i + 1.0);
            ScoredHit existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new ScoredHit(hit, score));
            } else {
                merged.put(key, new ScoredHit(prefer(existing.hit(), hit), existing.score() + score));
            }
        }
    }

    private RagSearchHit prefer(RagSearchHit existing, RagSearchHit incoming) {
        if (incoming.score() > existing.score()) {
            return incoming;
        }
        return existing;
    }

    private double maxPossibleScore() {
        int rankConstant = Math.max(1, properties.getRagSearch().getRrfRankConstant());
        double weight = Math.max(0, properties.getRagSearch().getVectorWeight())
                + Math.max(0, properties.getRagSearch().getKeywordWeight());
        return weight / (rankConstant + 1.0);
    }

    private double normalize(double score, double maxScore) {
        if (maxScore <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, score / maxScore));
    }

    private record ScoredHit(RagSearchHit hit, double score) {
    }
}
