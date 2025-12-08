package com.aibooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRequest {
    @NotBlank(message = "Text input is required")
    private String text;
    
    private String userId; // Optional: for multi-user support
}

