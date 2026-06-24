package org.example.ztbsync.api;

import org.example.ztbsync.service.DatabaseTestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试神通数据库连接和基础查询能力的接口。
 */
@RestController
@RequestMapping("/api/database")
public class DatabaseTestController {

    private final DatabaseTestService databaseTestService;

    public DatabaseTestController(DatabaseTestService databaseTestService) {
        this.databaseTestService = databaseTestService;
    }

    /**
     * 获取 JDBC 连接并执行固定校验查询。
     */
    @GetMapping("/test")
    public ResponseEntity<DatabaseTestResponse> test() {
        DatabaseTestResponse response = databaseTestService.testConnection();
        return ResponseEntity.status(response.success() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
