package com.aibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String status;
    private String message;
    private String eventId;
    private String eventSubject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> attendees;
    private String recurrencePattern;
    private List<String> exceptions;
}

