package com.bestbuy.schedulehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private String status;
    private String message;
    private String eventId;
    private String eventSubject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> attendees;
    private String recurrencePattern;
    private List<String> exceptions;
    
    // Additional output fields for UI display
    private Map<String, Object> openaiOutput;  // Extracted entities from OpenAI
    private Map<String, Object> graphApiInput; // Event data sent to Graph API
    private List<Map<String, Object>> bookingResults; // List of booked meetings
}
