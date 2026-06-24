package org.example.ztbsync.api;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * RAG 查询请求，兼容文档中的 snake_case 和 Java 常用 camelCase。
 */
public record RagSearchRequest(
        @JsonAlias("project_id") String projectId,
        @JsonAlias("user_question") String userQuestion,
        @JsonAlias("top_k") Integer topK,
        @JsonAlias("file_type") String fileType) {

    public RagSearchRequest(String projectId, String userQuestion, Integer topK) {
        this(projectId, userQuestion, topK, null);
    }
}
