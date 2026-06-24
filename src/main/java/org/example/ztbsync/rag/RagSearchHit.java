package org.example.ztbsync.rag;

/**
 * Elasticsearch 向量查询返回的原始 chunk 命中。
 */
public record RagSearchHit(
        String fileId,
        String fileName,
        String fileType,
        int chunkIndex,
        String chunkText,
        String sectionPath,
        double score) {

    public String key() {
        return nullToEmpty(fileType) + "|" + nullToEmpty(fileId) + "|" + chunkIndex;
    }

    public RagSearchHit withScore(double score) {
        return new RagSearchHit(fileId, fileName, fileType, chunkIndex, chunkText, sectionPath, score);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
