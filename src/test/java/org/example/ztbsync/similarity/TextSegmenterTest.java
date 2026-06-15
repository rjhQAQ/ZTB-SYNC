package org.example.ztbsync.similarity;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.ztbsync.config.ZtbProperties;
import org.junit.jupiter.api.Test;

class TextSegmenterTest {

    @Test
    void normalizesAndFiltersFormalizedText() {
        ZtbProperties properties = new ZtbProperties();
        properties.getSimilarity().setMinSegmentChars(6);
        TextSegmenter segmenter = new TextSegmenter(properties);

        var segments = segmenter.segment("""
                目录
                第 1 页
                本项目实施团队将采用星河方案推进数据治理工作。
                """);

        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).normalizedText()).contains("本项目实施团队");
    }
}
