package org.example.ztbsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import org.example.ztbsync.api.DatabaseTestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class DatabaseTestServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void testConnectionReturnsDatabaseMetadataAndValidationResult() throws Exception {
        ObjectProvider<DataSource> provider = mock(ObjectProvider.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        DatabaseTestService service = new DatabaseTestService(provider);

        when(provider.getIfAvailable()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(connection.isValid(3)).thenReturn(true);
        when(metaData.getDatabaseProductName()).thenReturn("Oscar");
        when(metaData.getDatabaseProductVersion()).thenReturn("8");
        when(metaData.getDriverName()).thenReturn("Oscar JDBC Driver");
        when(metaData.getDriverVersion()).thenReturn("8");
        when(metaData.getURL()).thenReturn("jdbc:oscar://127.0.0.1:2003/OSRDB");
        when(metaData.getUserName()).thenReturn("SYSDBA");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("1");

        DatabaseTestResponse response = service.testConnection();

        assertThat(response.success()).isTrue();
        assertThat(response.connectionValid()).isTrue();
        assertThat(response.databaseProductName()).isEqualTo("Oscar");
        assertThat(response.validationQuery()).isEqualTo("SELECT 1");
        assertThat(response.validationResult()).isEqualTo("1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testConnectionReturnsFailedWhenDataSourceIsMissing() {
        ObjectProvider<DataSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DatabaseTestService service = new DatabaseTestService(provider);

        DatabaseTestResponse response = service.testConnection();

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("DataSource");
    }
}
