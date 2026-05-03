package com.trackit.data.controller;

import com.trackit.data.dto.goals.GoalsResponse;
import com.trackit.data.dto.goals.UpdateGoalsRequest;
import com.trackit.data.service.GoalsService;
import com.trackit.data.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalsController {

    private final GoalsService service;

    public GoalsController(GoalsService service) {
        this.service = service;
    }

    @GetMapping
    public GoalsResponse get(@CurrentUser Long userId) {
        return service.get(userId);
    }

    @PutMapping
    public GoalsResponse upsert(@CurrentUser Long userId, @Valid @RequestBody UpdateGoalsRequest req) {
        return service.upsert(userId, req);
    }
}
