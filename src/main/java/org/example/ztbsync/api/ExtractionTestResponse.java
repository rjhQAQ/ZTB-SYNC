package org.example.ztbsync.api;

/**
 * 测试用同步抽取响应。
 *
 * <p>regexResult、llmResult 和 mergedResult 会根据文件类型返回招标或投标抽取对象。</p>
 */
public record ExtractionTestResponse(
        String fileName,
        String type,
        long fileSize,
        int textChars,
        String llmRawJson,
        Object regexResult,
        Object llmResult,
        Object mergedResult) {
}
