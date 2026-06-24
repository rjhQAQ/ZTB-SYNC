package org.example.ztbsync.api;

import java.util.List;

/**
 * RAG 查询接口和 MCP 工具共用响应。
 */
public record RagSearchResponse(
        int code,
        String message,
        RagSearchData data) {

    public static RagSearchResponse success(String projectId, List<RagMatchedDocument> matchedDocuments) {
        return new RagSearchResponse(200, "success", new RagSearchData(projectId, matchedDocuments));
    }

    public static RagSearchResponse error(int code, String message) {
        return new RagSearchResponse(code, message, null);
    }
}
