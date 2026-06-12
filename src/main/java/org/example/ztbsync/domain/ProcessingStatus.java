package org.example.ztbsync.domain;

/**
 * 文件处理任务生命周期状态。
 */
public enum ProcessingStatus {
    /** 已受理，等待后台线程处理。 */
    PENDING,
    /** 后台线程正在下载、解析或抽取。 */
    PROCESSING,
    /** 已成功抽取并写入业务表。 */
    SUCCESS,
    /** 处理失败，错误信息写入任务表。 */
    FAILED,
    /** 已被同一文件的新任务覆盖，不再写入业务表。 */
    SUPERSEDED
}
