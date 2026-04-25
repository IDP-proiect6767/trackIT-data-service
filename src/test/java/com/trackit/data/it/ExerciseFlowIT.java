package com.trackit.data.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.data.dto.CreateExerciseRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExerciseFlowIT extends BaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void fullCrud() throws Exception {
        CreateExerciseRequest req = new CreateExerciseRequest(
                "IT Test Exercise", "CHEST", "BARBELL", "BEGINNER", "Integration test exercise");

        // Create
        String responseBody = mockMvc.perform(post("/api/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("IT Test Exercise"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();

        // Get
        mockMvc.perform(get("/api/v1/exercises/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muscleGroup").value("CHEST"));

        // List with filter
        mockMvc.perform(get("/api/v1/exercises").param("muscleGroup", "CHEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Delete
        mockMvc.perform(delete("/api/v1/exercises/" + id))
                .andExpect(status().isNoContent());

        // Verify gone
        mockMvc.perform(get("/api/v1/exercises/" + id))
                .andExpect(status().isNotFound());
    }
}
