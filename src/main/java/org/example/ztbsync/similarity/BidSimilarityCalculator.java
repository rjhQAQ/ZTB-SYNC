package org.example.ztbsync.similarity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.example.ztbsync.config.ZtbProperties;
import org.example.ztbsync.domain.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class BidSimilarityCalculator {

    private static final Pattern IMPORTANT_NUMBER_PATTERN = Pattern.compile(
            "(1[3-9]\\d{9}|0\\d{2,3}[- ]?\\d{7,8}|[A-Za-z0-9][A-Za-z0-9\\-]{5,}|\\d{4,})");
    private static final List<String> ERROR_KEYWORDS = List.of("备案", "评标", "单位", "材料", "结果", "证书", "编号");

    private final ZtbProperties properties;
    private final TextSegmenter segmenter;
    private final NGramJaccardSimilarity jaccard;

    public BidSimilarityCalculator(
            ZtbProperties properties,
            TextSegmenter segmenter,
            NGramJaccardSimilarity jaccard) {
        this.properties = properties;
        this.segmenter = segmenter;
        this.jaccard = jaccard;
    }

    public SimilarityComputationResult calculate(
            SimilarityDocument leftDocument,
            SimilarityDocument rightDocument,
            String tenderText) {
        ZtbProperties.Similarity config = properties.getSimilarity();
        List<TextSegment> tenderSegments = segmenter.segment(tenderText);
        List<TextSegment> leftSegments = markTenderDerived(segmenter.segment(leftDocument.text()), tenderSegments);
        List<TextSegment> rightSegments = markTenderDerived(segmenter.segment(rightDocument.text()), tenderSegments);

        List<HitFragment> allHits = findHits(leftDocument, rightDocument, leftSegments, rightSegments);
        double totalLeft = totalLength(leftSegments);
        double totalRight = totalLength(rightSegments);
        double denominator = Math.min(totalLeft, totalRight);
        double matchedWeight = allHits.stream()
                .mapToDouble(hit -> Math.max(0, hit.weight()) * Math.min(hit.leftOriginalText().length(), hit.rightOriginalText().length()))
                .sum();
        double coverage = denominator <= 0 ? 0 : Math.min(1.0, matchedWeight / denominator);
        double avgSimilarity = weightedAverageSimilarity(allHits);
        double errorBonus = Math.min(20, allHits.stream()
                .filter(hit -> !"TEXT_SIMILARITY".equals(hit.type()))
                .mapToDouble(this::bonusFor)
                .sum());
        double score = Math.min(100, 100 * (0.65 * coverage + 0.35 * avgSimilarity) + errorBonus);
        score = round(score);

        List<HitFragment> topHits = allHits.stream()
                .sorted(Comparator.comparingDouble(HitFragment::similarity).reversed())
                .limit(Math.max(0, config.getTopHitLimit()))
                .toList();
        return new SimilarityComputationResult(
                score,
                riskLevel(score),
                round(coverage),
                round(avgSimilarity),
                round(errorBonus),
                topHits);
    }

    private List<TextSegment> markTenderDerived(List<TextSegment> bidSegments, List<TextSegment> tenderSegments) {
        ZtbProperties.Similarity config = properties.getSimilarity();
        if (tenderSegments.isEmpty()) {
            return bidSegments;
        }
        List<TextSegment> marked = new ArrayList<>();
        for (TextSegment segment : bidSegments) {
            double tenderSimilarity = tenderSegments.stream()
                    .mapToDouble(tender -> jaccard.similarity(segment.grams(), tender.grams()))
                    .max()
                    .orElse(0);
            boolean tenderDerived = tenderSimilarity >= config.getTenderMatchThreshold();
            marked.add(new TextSegment(
                    segment.index(),
                    segment.originalText(),
                    segment.normalizedText(),
                    segment.grams(),
                    tenderDerived,
                    round(tenderSimilarity),
                    tenderDerived ? config.getTenderDerivedWeight() : 1.0));
        }
        return marked;
    }

    private List<HitFragment> findHits(
            SimilarityDocument leftDocument,
            SimilarityDocument rightDocument,
            List<TextSegment> leftSegments,
            List<TextSegment> rightSegments) {
        ZtbProperties.Similarity config = properties.getSimilarity();
        List<HitFragment> hits = new ArrayList<>();
        for (TextSegment left : leftSegments) {
            TextSegment bestRight = null;
            double bestSimilarity = 0;
            for (TextSegment right : rightSegments) {
                double similarity = jaccard.similarity(left.grams(), right.grams());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestRight = right;
                }
            }
            if (bestRight != null && bestSimilarity >= config.getSegmentMatchThreshold()) {
                hits.add(buildHit(leftDocument, rightDocument, left, bestRight, round(bestSimilarity)));
            }
        }
        return hits;
    }

    private HitFragment buildHit(
            SimilarityDocument leftDocument,
            SimilarityDocument rightDocument,
            TextSegment left,
            TextSegment right,
            double similarity) {
        boolean tenderDerived = left.tenderDerived() || right.tenderDerived();
        double weight = Math.min(left.weight(), right.weight());
        String type = "TEXT_SIMILARITY";
        String reason = tenderDerived ? "TENDER_DERIVED_TEXT" : "NON_STANDARD_TEXT";
        if (hasCrossCompanyName(leftDocument, rightDocument, left, right)) {
            type = "COMPANY_NAME_CROSS_USE";
            reason = "片段疑似包含对方投标公司名称";
        } else if (hasSharedImportantNumber(left, right)) {
            type = "NUMBER_TOKEN_MATCH";
            reason = "片段包含相同异常数字或编号";
        } else if (hasErrorKeyword(left, right)) {
            type = "KEY_MATERIAL_MATCH";
            reason = "备案、评标、单位、材料或结果类片段高度相似";
        }
        return new HitFragment(
                type,
                reason,
                left.originalText(),
                right.originalText(),
                similarity,
                tenderDerived,
                Math.max(left.tenderSimilarity(), right.tenderSimilarity()),
                round(weight));
    }

    private double totalLength(List<TextSegment> segments) {
        return segments.stream()
                .mapToDouble(segment -> segment.normalizedText().length())
                .sum();
    }

    private double weightedAverageSimilarity(List<HitFragment> hits) {
        double totalWeight = hits.stream().mapToDouble(HitFragment::weight).sum();
        if (totalWeight <= 0) {
            return 0;
        }
        return hits.stream()
                .mapToDouble(hit -> hit.similarity() * hit.weight())
                .sum() / totalWeight;
    }

    private double bonusFor(HitFragment hit) {
        return switch (hit.type()) {
            case "COMPANY_NAME_CROSS_USE" -> 10;
            case "NUMBER_TOKEN_MATCH" -> 5;
            case "KEY_MATERIAL_MATCH" -> 3;
            default -> 0;
        };
    }

    private boolean hasCrossCompanyName(
            SimilarityDocument leftDocument,
            SimilarityDocument rightDocument,
            TextSegment left,
            TextSegment right) {
        String leftCompany = segmenter.normalize(leftDocument.companyName());
        String rightCompany = segmenter.normalize(rightDocument.companyName());
        return !leftCompany.isBlank() && right.normalizedText().contains(leftCompany)
                || !rightCompany.isBlank() && left.normalizedText().contains(rightCompany);
    }

    private boolean hasSharedImportantNumber(TextSegment left, TextSegment right) {
        Set<String> leftTokens = importantTokens(left.originalText());
        if (leftTokens.isEmpty()) {
            return false;
        }
        Set<String> rightTokens = importantTokens(right.originalText());
        leftTokens.retainAll(rightTokens);
        return !leftTokens.isEmpty();
    }

    private Set<String> importantTokens(String value) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = IMPORTANT_NUMBER_PATTERN.matcher(value == null ? "" : value);
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        return tokens;
    }

    private boolean hasErrorKeyword(TextSegment left, TextSegment right) {
        return ERROR_KEYWORDS.stream().anyMatch(keyword ->
                left.originalText().contains(keyword) && right.originalText().contains(keyword));
    }

    private String riskLevel(double score) {
        ZtbProperties.Similarity config = properties.getSimilarity();
        if (score >= config.getHighRiskThreshold()) {
            return RiskLevel.HIGH.name();
        }
        if (score >= config.getSuspectedThreshold()) {
            return RiskLevel.SUSPECTED.name();
        }
        return RiskLevel.LOW.name();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
