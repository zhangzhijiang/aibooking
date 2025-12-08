package com.aibooking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LuisIntent {
    private String query;
    private Prediction prediction;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Prediction {
        private String topIntent;
        private Map<String, Double> intents;
        private Map<String, List<Entity>> entities;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        private String category;
        private String text;
        private Integer offset;
        private Integer length;
        private Double score;
        private Map<String, Object> resolution;
    }
}

