package org.example.ztbsync.rag;

import java.util.Locale;

import org.example.ztbsync.domain.FileType;

/**
 * RAG 查询文件类型过滤，ALL 表示不限制招标/投标文件。
 */
public enum RagFileTypeFilter {
    ALL(null),
    TENDER(FileType.TENDER.code()),
    BID(FileType.BID.code());

    private final String fileType;

    RagFileTypeFilter(String fileType) {
        this.fileType = fileType;
    }

    public String fileType() {
        return fileType;
    }

    public boolean hasFilter() {
        return fileType != null;
    }

    public static RagFileTypeFilter from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL", "全部" -> ALL;
            case "TENDER", "招标文件" -> TENDER;
            case "BID", "投标文件", "投投标文件" -> BID;
            default -> throw new IllegalArgumentException("不支持的 RAG 文件类型: " + value);
        };
    }
}
