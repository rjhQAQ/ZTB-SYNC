package org.example.ztbsync.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RAG 查询命中的单个片段。
 */
public record RagMatchedDocument(
        @JsonProperty("doc_id") String docId,
        @JsonProperty("doc_type") String docType,
        @JsonProperty("doc_name") String docName,
        @JsonProperty("relevant_chapters") List<String> relevantChapters,
        @JsonProperty("source_text") String sourceText,
        double score) {
}
