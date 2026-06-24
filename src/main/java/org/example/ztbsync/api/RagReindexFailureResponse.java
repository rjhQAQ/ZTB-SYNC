package org.example.ztbsync.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagReindexFailureResponse(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_type") String fileType,
        String message) {
}
