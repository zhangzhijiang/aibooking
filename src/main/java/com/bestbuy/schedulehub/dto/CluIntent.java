package com.bestbuy.schedulehub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CluIntent {
    private String kind;
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String query;
        private Prediction prediction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Prediction {
        private String topIntent;
        private Map<String, Intent> intents;
        private List<Entity> entities;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Intent {
        private Double score;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entity {
        private String category;
        private String text;
        private Integer offset;
        private Integer length;
        private Double confidenceScore;
        private Map<String, Object> extraInformation;
    }
}
