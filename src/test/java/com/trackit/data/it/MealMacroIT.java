package com.trackit.data.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.data.dto.CreateFoodRequest;
import com.trackit.data.dto.CreateMealRequest;
import com.trackit.data.dto.MealItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MealMacroIT extends BaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void mealTotalsAreComputedCorrectly() throws Exception {
        // Create chicken food: 100g serving = 165 kcal, 31g protein
        CreateFoodRequest chickenReq = new CreateFoodRequest("IT Chicken",
                null, new BigDecimal("100"), new BigDecimal("165"),
                new BigDecimal("31"), BigDecimal.ZERO, new BigDecimal("3.6"));
        String chickenBody = mockMvc.perform(post("/api/v1/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chickenReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long chickenId = objectMapper.readTree(chickenBody).get("id").asLong();

        // Create rice food: 100g serving = 130 kcal, 2.7g protein
        CreateFoodRequest riceReq = new CreateFoodRequest("IT Rice",
                null, new BigDecimal("100"), new BigDecimal("130"),
                new BigDecimal("2.7"), new BigDecimal("28.2"), new BigDecimal("0.3"));
        String riceBody = mockMvc.perform(post("/api/v1/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(riceReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long riceId = objectMapper.readTree(riceBody).get("id").asLong();

        // Log a meal: 200g chicken + 100g rice
        CreateMealRequest mealReq = new CreateMealRequest("LUNCH", Instant.now(), null, List.of(
                new MealItemRequest(chickenId, new BigDecimal("200")),
                new MealItemRequest(riceId, new BigDecimal("100"))
        ));

        mockMvc.perform(post("/api/v1/meals")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mealReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCaloriesKcal").value(460.00))
                .andExpect(jsonPath("$.totalProteinG").value(64.70));
    }
}
