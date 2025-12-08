package com.aibooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedEntities {
    private String intent;
    private List<String> attendees = new ArrayList<>();
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String recurrencePattern;
    private List<String> exceptions = new ArrayList<>();
    private String subject;
    private String location;
}

