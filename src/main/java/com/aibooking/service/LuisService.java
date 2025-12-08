package com.aibooking.service;

import com.aibooking.dto.ExtractedEntities;
import com.aibooking.dto.LuisIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LuisService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${azure.luis.endpoint}")
    private String luisEndpoint;

    @Value("${azure.luis.app-id}")
    private String luisAppId;

    @Value("${azure.luis.key}")
    private String luisKey;

    @Value("${azure.luis.region}")
    private String luisRegion;

    public ExtractedEntities extractIntentAndEntities(String text) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("%s/luis/prediction/v3.0/apps/%s/slots/production/predict?subscription-key=%s&verbose=true&show-all-intents=true&log=true&query=%s",
                    luisEndpoint, luisAppId, luisKey, encodedText);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("LUIS API call failed: {}", response.code());
                    return ExtractedEntities.builder()
                            .intent("Unknown")
                            .build();
                }

                String responseBody = response.body().string();
                LuisIntent luisResponse = objectMapper.readValue(responseBody, LuisIntent.class);

                return parseLuisResponse(luisResponse, text);
            }
        } catch (IOException e) {
            log.error("Error calling LUIS API", e);
            return ExtractedEntities.builder()
                    .intent("Unknown")
                    .build();
        }
    }

    private ExtractedEntities parseLuisResponse(LuisIntent luisResponse, String originalText) {
        ExtractedEntities.ExtractedEntitiesBuilder builder = ExtractedEntities.builder();

        if (luisResponse.getPrediction() != null) {
            String topIntent = luisResponse.getPrediction().getTopIntent();
            builder.intent(topIntent != null ? topIntent : "Unknown");

            Map<String, List<LuisIntent.Entity>> entities = luisResponse.getPrediction().getEntities();
            if (entities != null) {
                List<String> attendees = new ArrayList<>();
                LocalDateTime startDateTime = null;
                LocalDateTime endDateTime = null;
                String recurrencePattern = null;
                List<String> exceptions = new ArrayList<>();
                String subject = null;
                String location = null;

                for (Map.Entry<String, List<LuisIntent.Entity>> entry : entities.entrySet()) {
                    String category = entry.getKey();
                    List<LuisIntent.Entity> entityList = entry.getValue();

                    for (LuisIntent.Entity entity : entityList) {
                        String text = entity.getText();
                        Map<String, Object> resolution = entity.getResolution();

                        switch (category.toLowerCase()) {
                            case "personname":
                            case "attendee":
                                attendees.add(text);
                                break;
                            case "datetime":
                            case "datetimev2":
                                if (resolution != null && resolution.containsKey("values")) {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> values = (List<Map<String, Object>>) resolution.get("values");
                                    if (!values.isEmpty()) {
                                        Map<String, Object> firstValue = values.get(0);
                                        String timex = (String) firstValue.get("timex");
                                        String type = (String) firstValue.get("type");
                                        
                                        if (startDateTime == null && timex != null) {
                                            startDateTime = parseDateTime(timex, type);
                                        } else if (endDateTime == null && timex != null) {
                                            endDateTime = parseDateTime(timex, type);
                                        }
                                    }
                                }
                                break;
                            case "recurrence":
                                recurrencePattern = text;
                                break;
                            case "exception":
                                exceptions.add(text);
                                break;
                            case "subject":
                            case "meetingtitle":
                                subject = text;
                                break;
                            case "location":
                                location = text;
                                break;
                        }
                    }
                }

                builder.attendees(attendees)
                        .startDateTime(startDateTime)
                        .endDateTime(endDateTime)
                        .recurrencePattern(recurrencePattern)
                        .exceptions(exceptions)
                        .subject(subject != null ? subject : "Meeting")
                        .location(location);
            }
        }

        return builder.build();
    }

    private LocalDateTime parseDateTime(String timex, String type) {
        try {
            // Handle common timex patterns
            if (timex.startsWith("XXXX")) {
                // Relative date like "tomorrow", "next week"
                return LocalDateTime.now().plusDays(1);
            }
            
            // Try parsing ISO format
            if (timex.contains("T")) {
                String dateTimeStr = timex.replace("T", "T").split("\\.")[0];
                if (dateTimeStr.length() == 16) {
                    return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                }
            }
            
            // Default: return tomorrow at current time
            return LocalDateTime.now().plusDays(1);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse datetime: {}", timex, e);
            return LocalDateTime.now().plusDays(1);
        }
    }
}

