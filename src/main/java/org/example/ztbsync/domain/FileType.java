package org.example.ztbsync.domain;

import java.util.Locale;

/**
 * 上传文件业务类型。
 *
 * <p>TENDER 表示招标文件，BID 表示投标文件。from 方法负责兼容接口传入的英文和中文值。</p>
 */
public enum FileType {
    /** 招标文件。 */
    TENDER,
    /** 投标文件。 */
    BID;

    public static FileType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TENDER", "招标文件" -> TENDER;
            case "BID", "投标文件", "投投标文件" -> BID;
            default -> throw new IllegalArgumentException("不支持的文件类型: " + value);
        };
    }

    public String code() {
        return name();
    }
}
