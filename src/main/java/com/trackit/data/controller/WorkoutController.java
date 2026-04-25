package com.trackit.data.controller;

import com.trackit.data.dto.*;
import com.trackit.data.service.WorkoutService;
import com.trackit.data.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workouts")
public class WorkoutController {

    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<WorkoutResponse> list(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(userId, page, size);
    }

    @GetMapping("/{id}")
    public WorkoutResponse get(@CurrentUser Long userId, @PathVariable Long id) {
        return service.get(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse create(@CurrentUser Long userId, @Valid @RequestBody CreateWorkoutRequest req) {
        return service.create(userId, req);
    }

    @PatchMapping("/{id}")
    public WorkoutResponse update(@CurrentUser Long userId, @PathVariable Long id,
                                   @Valid @RequestBody UpdateWorkoutRequest req) {
        return service.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
