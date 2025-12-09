package com.bestbuy.schedulehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScheduleRequest {
    @NotBlank(message = "Text input is required")
    private String text;

    private String userId; // Optional: for multi-user support
}
