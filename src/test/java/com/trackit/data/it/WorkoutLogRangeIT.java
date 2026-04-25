package com.trackit.data.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.data.dto.CreateWorkoutLogRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkoutLogRangeIT extends BaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void rangeFilterReturnsOnlyInRangeLogs() throws Exception {
        long userId = 777L;

        // January log
        CreateWorkoutLogRequest jan = new CreateWorkoutLogRequest(null,
                Instant.parse("2026-01-15T10:00:00Z"), null, "January", List.of());
        // March log
        CreateWorkoutLogRequest mar = new CreateWorkoutLogRequest(null,
                Instant.parse("2026-03-10T10:00:00Z"), null, "March", List.of());

        postLog(userId, jan);
        postLog(userId, mar);

        // Query only January
        mockMvc.perform(get("/api/v1/workout-logs")
                        .header("X-User-Id", String.valueOf(userId))
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notes").value("January"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private void postLog(long userId, CreateWorkoutLogRequest req) throws Exception {
        mockMvc.perform(post("/api/v1/workout-logs")
                        .header("X-User-Id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
