package org.example.ztbsync.api;

import java.time.LocalDateTime;

/**
 * 神通数据库测试接口响应。
 *
 * <p>只返回连接和固定校验查询结果，不包含数据库密码等敏感信息。</p>
 */
public record DatabaseTestResponse(
        boolean success,
        boolean connectionValid,
        String databaseProductName,
        String databaseProductVersion,
        String driverName,
        String driverVersion,
        String url,
        String username,
        String validationQuery,
        String validationResult,
        long elapsedMs,
        String message,
        LocalDateTime testedAt) {
}
