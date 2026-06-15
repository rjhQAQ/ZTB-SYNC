package org.example.ztbsync.domain;

/**
 * 投标文件雷同分析结果状态。
 */
public enum SimilarityStatus {
    /** 本组两两对比已成功完成。 */
    SUCCESS,
    /** 本组两两对比失败，错误信息写入结果表。 */
    FAILED
}
