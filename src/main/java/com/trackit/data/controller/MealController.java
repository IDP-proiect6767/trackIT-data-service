package com.trackit.data.controller;

import com.trackit.data.dto.*;
import com.trackit.data.service.MealService;
import com.trackit.data.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/meals")
public class MealController {

    private final MealService service;

    public MealController(MealService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MealResponse> list(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Both 'from' and 'to' parameters must be provided together");
        }
        return service.list(userId, page, size, from, to);
    }

    @GetMapping("/{id}")
    public MealResponse get(@CurrentUser Long userId, @PathVariable Long id) {
        return service.get(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealResponse create(@CurrentUser Long userId, @Valid @RequestBody CreateMealRequest req) {
        return service.create(userId, req);
    }

    @PatchMapping("/{id}")
    public MealResponse update(@CurrentUser Long userId, @PathVariable Long id,
                                @Valid @RequestBody UpdateMealRequest req) {
        return service.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
