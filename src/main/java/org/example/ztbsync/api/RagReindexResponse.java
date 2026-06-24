package org.example.ztbsync.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagReindexResponse(
        @JsonProperty("project_id") String projectId,
        @JsonProperty("total_files") int totalFiles,
        @JsonProperty("success_count") int successCount,
        @JsonProperty("failed_count") int failedCount,
        List<RagReindexFailureResponse> failures) {
}
