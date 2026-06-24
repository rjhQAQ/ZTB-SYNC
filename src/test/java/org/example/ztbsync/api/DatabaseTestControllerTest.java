package org.example.ztbsync.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.example.ztbsync.service.DatabaseTestService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DatabaseTestControllerTest {

    @Test
    void databaseTestReturnsOkWhenSuccess() throws Exception {
        DatabaseTestService service = mock(DatabaseTestService.class);
        when(service.testConnection()).thenReturn(new DatabaseTestResponse(
                true,
                true,
                "Oscar",
                "8",
                "Oscar JDBC Driver",
                "8",
                "jdbc:oscar://127.0.0.1:2003/OSRDB",
                "SYSDBA",
                "SELECT 1",
                "1",
                12,
                "神通数据库连接和基础查询正常",
                LocalDateTime.now()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DatabaseTestController(service)).build();

        mvc.perform(get("/api/database/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.validationQuery").value("SELECT 1"))
                .andExpect(jsonPath("$.validationResult").value("1"));
    }

    @Test
    void databaseTestReturnsServiceUnavailableWhenFailed() throws Exception {
        DatabaseTestService service = mock(DatabaseTestService.class);
        when(service.testConnection()).thenReturn(new DatabaseTestResponse(
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
                3,
                "连接失败",
                LocalDateTime.now()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DatabaseTestController(service)).build();

        mvc.perform(get("/api/database/test"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("连接失败"));
    }
}
