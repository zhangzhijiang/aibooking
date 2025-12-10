package com.bestbuy.schedulehub.controller;

import com.bestbuy.schedulehub.dto.ScheduleRequest;
import com.bestbuy.schedulehub.dto.ScheduleResponse;
import com.bestbuy.schedulehub.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for scheduling calendar events.
 * 
 * Provides endpoints for:
 * - POST /api/scheduleMeeting - Process natural language scheduling requests
 * - GET /api/health - Health check endpoint
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ScheduleController {

    private final ScheduleService ScheduleService;

    /**
     * Processes a natural language scheduling request.
     * 
     * @param request The scheduling request containing natural language text
     * @param userId  Required user ID header (user email or object ID).
     *                With application authentication, "me" is not supported.
     * @return ScheduleResponse with booking status and event details
     */
    @PostMapping("/scheduleMeeting")
    public ResponseEntity<ScheduleResponse> scheduleMeeting(
            @Valid @RequestBody ScheduleRequest request,
            @RequestHeader(value = "X-User-Id", required = true) String userId) {

        log.info("Received schedule request: {}", request.getText());
        log.info("User ID: {}", userId);

        if (userId == null || userId.trim().isEmpty()) {
            log.error("User ID is required. Please provide X-User-Id header with a valid user email or object ID.");
            ScheduleResponse errorResponse = ScheduleResponse.builder()
                    .status("error")
                    .message(
                            "User ID is required. Please provide X-User-Id header with a valid user email or object ID (e.g., user@example.com)")
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }

        ScheduleResponse response = ScheduleService.processScheduleRequest(
                request.getText(),
                userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}
