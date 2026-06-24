package org.example.ztbsync.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.example.ztbsync.config.ZtbProperties;
import org.junit.jupiter.api.Test;

class RagChunkerTest {

    @Test
    void mergesShortBlocksAndCarriesSectionPath() {
        ZtbProperties properties = properties(80, 120, 20, 10);
        RagChunker chunker = new RagChunker(properties);

        List<RagChunk> chunks = chunker.chunk(List.of(
                new RagDocumentBlock(0, RagBlockType.HEADING, "第一章 项目说明", 1, 0, 8),
                new RagDocumentBlock(1, RagBlockType.PARAGRAPH, "这是第一段项目背景内容，说明系统建设目标。", 0, 9, 30),
                new RagDocumentBlock(2, RagBlockType.TABLE_ROW, "模块 | 数据接入服务", 0, 31, 43)));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).sectionPath()).isEqualTo("第一章 项目说明");
        assertThat(chunks.get(0).blockTypes()).containsExactly("PARAGRAPH", "TABLE_ROW");
    }

    @Test
    void splitsLongBlocksWithOverlapAndDoesNotCarryOverlapToNewSection() {
        ZtbProperties properties = properties(30, 50, 10, 10);
        RagChunker chunker = new RagChunker(properties);
        String longText = "甲".repeat(120);

        List<RagChunk> chunks = chunker.chunk(List.of(
                new RagDocumentBlock(0, RagBlockType.HEADING, "第一章 技术方案", 1, 0, 8),
                new RagDocumentBlock(1, RagBlockType.PARAGRAPH, longText, 0, 9, 129),
                new RagDocumentBlock(2, RagBlockType.HEADING, "第二章 商务响应", 1, 130, 138),
                new RagDocumentBlock(3, RagBlockType.PARAGRAPH, "乙".repeat(20), 0, 139, 159)));

        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(0).chunkText()).hasSize(50);
        assertThat(chunks.get(1).chunkText()).startsWith("甲".repeat(10));
        assertThat(chunks.get(3).sectionPath()).isEqualTo("第二章 商务响应");
        assertThat(chunks.get(3).chunkText()).doesNotContain("甲");
    }

    @Test
    void marksBoilerplateAndCanFilterIt() {
        ZtbProperties properties = properties(30, 80, 10, 10);
        RagChunker chunker = new RagChunker(properties);

        List<RagChunk> kept = chunker.chunk(List.of(
                new RagDocumentBlock(0, RagBlockType.PARAGRAPH, "授权委托书：法定代表人签字盖章。", 0, 0, 17)));
        assertThat(kept).singleElement().extracting(RagChunk::boilerplate).isEqualTo(true);

        properties.getEmbedding().setKeepBoilerplate(false);
        List<RagChunk> filtered = chunker.chunk(List.of(
                new RagDocumentBlock(0, RagBlockType.PARAGRAPH, "授权委托书：法定代表人签字盖章。", 0, 0, 17)));
        assertThat(filtered).isEmpty();
    }

    private ZtbProperties properties(int target, int max, int overlap, int min) {
        ZtbProperties properties = new ZtbProperties();
        properties.getEmbedding().setTargetChunkChars(target);
        properties.getEmbedding().setMaxChunkChars(max);
        properties.getEmbedding().setOverlapChars(overlap);
        properties.getEmbedding().setMinChunkChars(min);
        return properties;
    }
}
