package org.example.ztbsync.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.junit.jupiter.api.Test;

class RagHybridRankerTest {

    @Test
    void mergesVectorAndKeywordHitsWithRrf() {
        ZtbProperties properties = new ZtbProperties();
        RagHybridRanker ranker = new RagHybridRanker(properties);

        List<RagSearchHit> ranked = ranker.rank(
                List.of(
                        new RagSearchHit("file-a", "招标.docx", "TENDER", 1, "向量第一", "第一章", 0.9),
                        new RagSearchHit("file-b", "招标.docx", "TENDER", 2, "向量第二", "第二章", 0.8)),
                List.of(
                        new RagSearchHit("file-b", "招标.docx", "TENDER", 2, "关键词第一", "第二章", 1.0),
                        new RagSearchHit("file-a", "招标.docx", "TENDER", 1, "关键词第二", "第一章", 0.7)),
                2);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).fileId()).isEqualTo("file-a");
        assertThat(ranked.get(0).score()).isGreaterThan(0.9);
        assertThat(ranked.get(1).fileId()).isEqualTo("file-b");
    }
}
