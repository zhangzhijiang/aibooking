package com.bestbuy.schedulehub.controller;

import com.bestbuy.schedulehub.dto.ScheduleRequest;
import com.bestbuy.schedulehub.dto.ScheduleResponse;
import com.bestbuy.schedulehub.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ScheduleController {

    private final ScheduleService ScheduleService;

    @PostMapping("/scheduleMeeting")
    public ResponseEntity<ScheduleResponse> scheduleMeeting(
            @Valid @RequestBody ScheduleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        log.info("Received schedule request: {}", request.getText());

        ScheduleResponse response = ScheduleService.processScheduleRequest(
                request.getText(),
                userId != null ? userId : "me");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}
