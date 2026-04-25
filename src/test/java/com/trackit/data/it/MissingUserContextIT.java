package com.trackit.data.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MissingUserContextIT extends BaseIT {

    @Autowired MockMvc mockMvc;

    @Test
    void workoutsWithoutHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/workouts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_USER_CONTEXT"));
    }

    @Test
    void workoutsWithInvalidHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/workouts").header("X-User-Id", "notanumber"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_USER_CONTEXT"));
    }

    @Test
    void workoutsWithNegativeUserIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/workouts").header("X-User-Id", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_USER_CONTEXT"));
    }

    @Test
    void mealsWithoutHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/meals"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_USER_CONTEXT"));
    }

    @Test
    void workoutLogsWithoutHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/workout-logs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_USER_CONTEXT"));
    }
}
