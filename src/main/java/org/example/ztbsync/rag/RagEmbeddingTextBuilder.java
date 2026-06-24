package org.example.ztbsync.rag;

import org.example.ztbsync.domain.FileProcessingTask;
import org.example.ztbsync.domain.FileType;
import org.springframework.stereotype.Component;

/**
 * 生成用于向量化和重排的上下文增强文本，返回给用户时仍使用原始 chunkText。
 */
@Component
public class RagEmbeddingTextBuilder {

    public String build(FileProcessingTask task, RagChunk chunk) {
        StringBuilder builder = new StringBuilder();
        builder.append("文件类型：").append(chineseFileType(task == null ? null : task.getFileType())).append('\n');
        builder.append("文件名称：").append(nullToEmpty(task == null ? null : task.getFileName())).append('\n');
        if (chunk != null && hasText(chunk.sectionPath())) {
            builder.append("章节路径：").append(chunk.sectionPath()).append('\n');
        }
        builder.append("正文：").append(chunk == null ? "" : nullToEmpty(chunk.chunkText()));
        return builder.toString();
    }

    public String buildForSearchHit(RagSearchHit hit) {
        StringBuilder builder = new StringBuilder();
        builder.append("文件类型：").append(chineseFileType(hit == null ? null : hit.fileType())).append('\n');
        builder.append("文件名称：").append(hit == null ? "" : nullToEmpty(hit.fileName())).append('\n');
        if (hit != null && hasText(hit.sectionPath())) {
            builder.append("章节路径：").append(hit.sectionPath()).append('\n');
        }
        builder.append("正文：").append(hit == null ? "" : nullToEmpty(hit.chunkText()));
        return builder.toString();
    }

    private String chineseFileType(String fileType) {
        try {
            return switch (FileType.from(fileType)) {
                case TENDER -> "招标文件";
                case BID -> "投标文件";
            };
        } catch (Exception exception) {
            return hasText(fileType) ? fileType : "未知文件";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
