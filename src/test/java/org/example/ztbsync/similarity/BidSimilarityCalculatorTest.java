package org.example.ztbsync.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.ztbsync.config.ZtbProperties;
import org.junit.jupiter.api.Test;

class BidSimilarityCalculatorTest {

    @Test
    void downweightsSegmentsThatComeFromTenderDocument() {
        ZtbProperties properties = new ZtbProperties();
        properties.getSimilarity().setMinSegmentChars(8);
        properties.getSimilarity().setSegmentMatchThreshold(0.75);
        properties.getSimilarity().setTenderMatchThreshold(0.80);
        properties.getSimilarity().setTenderDerivedWeight(0.2);
        TextSegmenter segmenter = new TextSegmenter(properties);
        BidSimilarityCalculator calculator = new BidSimilarityCalculator(
                properties,
                segmenter,
                new NGramJaccardSimilarity());

        SimilarityDocument left = new SimilarityDocument("bid-a", "A.docx", "甲公司", """
                本项目投标保证金金额为人民币壹万元整。
                我方项目实施团队将采用星河数据治理方案推进系统迁移，备案编号ABC12345。
                """);
        SimilarityDocument right = new SimilarityDocument("bid-b", "B.docx", "乙公司", """
                本项目投标保证金金额为人民币壹万元整。
                我方项目实施团队将采用星河数据治理方案推进系统迁移，备案编号ABC12345。
                """);
        String tender = "本项目投标保证金金额为人民币壹万元整。";

        SimilarityComputationResult withoutTender = calculator.calculate(left, right, "");
        SimilarityComputationResult withTender = calculator.calculate(left, right, tender);

        assertThat(withTender.score()).isLessThan(withoutTender.score());
        assertThat(withTender.hitFragments()).anyMatch(HitFragment::tenderDerived);
        assertThat(withTender.hitFragments()).anyMatch(hit -> "NUMBER_TOKEN_MATCH".equals(hit.type()));
    }
}
