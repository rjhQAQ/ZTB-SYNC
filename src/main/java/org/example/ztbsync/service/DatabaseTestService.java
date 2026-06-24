package org.example.ztbsync.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import javax.sql.DataSource;

import org.example.ztbsync.api.DatabaseTestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class DatabaseTestService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTestService.class);

    private final ObjectProvider<DataSource> dataSourceProvider;

    public DatabaseTestService(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    /**
     * 测试数据源是否可用，并执行固定的轻量查询。
     */
    public DatabaseTestResponse testConnection() {
        long startNanos = System.nanoTime();
        LocalDateTime testedAt = LocalDateTime.now();
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return failed(startNanos, testedAt, "未找到 DataSource，请检查 spring.datasource 配置");
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            boolean connectionValid = isConnectionValid(connection);
            QueryResult queryResult = executeValidationQuery(connection);
            log.info("Database test succeeded: product={}, driver={}, query={}, elapsedMs={}",
                    metaData.getDatabaseProductName(), metaData.getDriverName(),
                    queryResult.query(), elapsedMs(startNanos));
            return new DatabaseTestResponse(
                    true,
                    connectionValid,
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getDriverName(),
                    metaData.getDriverVersion(),
                    metaData.getURL(),
                    metaData.getUserName(),
                    queryResult.query(),
                    queryResult.value(),
                    elapsedMs(startNanos),
                    "神通数据库连接和基础查询正常",
                    testedAt);
        } catch (Exception exception) {
            String message = rootMessage(exception);
            log.warn("Database test failed: message={}, elapsedMs={}", message, elapsedMs(startNanos), exception);
            return failed(startNanos, testedAt, message);
        }
    }

    private boolean isConnectionValid(Connection connection) {
        try {
            return connection.isValid(3);
        } catch (SQLException exception) {
            log.debug("JDBC isValid check failed, fallback to validation query only", exception);
            return false;
        }
    }

    private QueryResult executeValidationQuery(Connection connection) throws SQLException {
        SQLException lastException = null;
        for (String query : new String[] {"SELECT 1", "SELECT 1 FROM DUAL"}) {
            try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    return new QueryResult(query, resultSet.getString(1));
                }
            } catch (SQLException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new SQLException("数据库校验查询无返回结果") : lastException;
    }

    private DatabaseTestResponse failed(long startNanos, LocalDateTime testedAt, String message) {
        return new DatabaseTestResponse(
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                elapsedMs(startNanos),
                message,
                testedAt);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record QueryResult(String query, String value) {
    }
}
