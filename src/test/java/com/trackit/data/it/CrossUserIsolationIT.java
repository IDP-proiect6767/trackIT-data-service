package com.trackit.data.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.data.dto.CreateWorkoutRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CrossUserIsolationIT extends BaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void user99CannotAccessUser42Workout() throws Exception {
        CreateWorkoutRequest req = new CreateWorkoutRequest("User42 Workout", null, List.of());

        // User 42 creates a workout
        String body = mockMvc.perform(post("/api/v1/workouts")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long workoutId = objectMapper.readTree(body).get("id").asLong();

        // User 99 tries GET → 404
        mockMvc.perform(get("/api/v1/workouts/" + workoutId)
                        .header("X-User-Id", "99"))
                .andExpect(status().isNotFound());

        // User 99 tries PATCH → 404
        mockMvc.perform(patch("/api/v1/workouts/" + workoutId)
                        .header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        // User 99 tries DELETE → 404
        mockMvc.perform(delete("/api/v1/workouts/" + workoutId)
                        .header("X-User-Id", "99"))
                .andExpect(status().isNotFound());

        // Verify workout still exists for user 42
        mockMvc.perform(get("/api/v1/workouts/" + workoutId)
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User42 Workout"));
    }
}
