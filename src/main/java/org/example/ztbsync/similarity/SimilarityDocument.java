package org.example.ztbsync.similarity;

/**
 * 一份参与雷同分析的文件文本，仅在内存中使用，不写入数据库。
 */
public record SimilarityDocument(
        String fileId,
        String fileName,
        String companyName,
        String text) {
}
