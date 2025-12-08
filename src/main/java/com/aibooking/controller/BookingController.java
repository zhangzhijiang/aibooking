package com.aibooking.controller;

import com.aibooking.dto.BookingRequest;
import com.aibooking.dto.BookingResponse;
import com.aibooking.service.BookingService;
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
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookMeeting")
    public ResponseEntity<BookingResponse> bookMeeting(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        log.info("Received booking request: {}", request.getText());
        
        BookingResponse response = bookingService.processBookingRequest(
                request.getText(),
                userId != null ? userId : "me"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}

