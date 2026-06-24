package org.example.ztbsync.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RAG 查询成功响应中的 data 节点。
 */
public record RagSearchData(
        @JsonProperty("project_id") String projectId,
        @JsonProperty("matched_documents") List<RagMatchedDocument> matchedDocuments) {
}
