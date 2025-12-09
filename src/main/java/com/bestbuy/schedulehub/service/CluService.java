package com.bestbuy.schedulehub.service;

import com.bestbuy.schedulehub.dto.CluIntent;
import com.bestbuy.schedulehub.dto.ExtractedEntities;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CluService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${azure.clu.endpoint}")
    private String cluEndpoint;

    @Value("${azure.clu.key}")
    private String cluKey;

    @Value("${azure.clu.project-name}")
    private String cluProjectName;

    @Value("${azure.clu.deployment-name}")
    private String cluDeploymentName;

    @Value("${azure.clu.api-version:2022-05-01}")
    private String cluApiVersion;

    public ExtractedEntities extractIntentAndEntities(String text) {
        try {
            String url = String.format("%s/language/:analyze-conversations?api-version=%s",
                    cluEndpoint, cluApiVersion);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("kind", "Conversation");

            Map<String, Object> analysisInput = new HashMap<>();
            Map<String, Object> conversationItem = new HashMap<>();
            conversationItem.put("id", "1");
            conversationItem.put("text", text);
            conversationItem.put("modality", "text");
            conversationItem.put("language", "en-US");
            analysisInput.put("conversationItem", conversationItem);
            requestBody.put("analysisInput", analysisInput);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("projectName", cluProjectName);
            parameters.put("deploymentName", cluDeploymentName);
            parameters.put("stringIndexType", "Utf16CodeUnit");
            requestBody.put("parameters", parameters);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            RequestBody body = RequestBody.create(jsonBody, JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Ocp-Apim-Subscription-Key", cluKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("CLU API call failed: {} - {}", response.code(), response.message());
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("Error response: {}", errorBody);
                    return ExtractedEntities.builder()
                            .intent("Unknown")
                            .build();
                }

                String responseBody = response.body().string();
                log.debug("CLU API response: {}", responseBody);

                CluIntent cluResponse = objectMapper.readValue(responseBody, CluIntent.class);

                return parseCluResponse(cluResponse, text);
            }
        } catch (IOException e) {
            log.error("Error calling CLU API", e);
            return ExtractedEntities.builder()
                    .intent("Unknown")
                    .build();
        }
    }

    private ExtractedEntities parseCluResponse(CluIntent cluResponse, String originalText) {
        ExtractedEntities.ExtractedEntitiesBuilder builder = ExtractedEntities.builder();

        if (cluResponse.getResult() != null && cluResponse.getResult().getPrediction() != null) {
            CluIntent.Prediction prediction = cluResponse.getResult().getPrediction();

            String topIntent = prediction.getTopIntent();
            builder.intent(topIntent != null ? topIntent : "Unknown");

            List<CluIntent.Entity> entities = prediction.getEntities();
            if (entities != null && !entities.isEmpty()) {
                List<String> attendees = new ArrayList<>();
                LocalDateTime startDateTime = null;
                LocalDateTime endDateTime = null;
                String recurrencePattern = null;
                List<String> exceptions = new ArrayList<>();
                String subject = null;
                String location = null;

                for (CluIntent.Entity entity : entities) {
                    String category = entity.getCategory();
                    String text = entity.getText();
                    Map<String, Object> extraInformation = entity.getExtraInformation();

                    // CLU entities may have category patterns like "PersonName", "DateTime", etc.
                    switch (category.toLowerCase()) {
                        case "personname":
                        case "attendee":
                        case "person":
                            attendees.add(text);
                            break;
                        case "datetime":
                        case "datetimev2":
                        case "time":
                        case "date":
                            if (extraInformation != null && extraInformation.containsKey("values")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> values = (List<Map<String, Object>>) extraInformation
                                        .get("values");
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
                            } else if (text != null) {
                                // Fallback: try to parse the text directly
                                LocalDateTime parsed = tryParseDateTime(text);
                                if (parsed != null && startDateTime == null) {
                                    startDateTime = parsed;
                                } else if (parsed != null && endDateTime == null) {
                                    endDateTime = parsed;
                                }
                            }
                            break;
                        case "recurrence":
                        case "recurringpattern":
                            recurrencePattern = text;
                            break;
                        case "exception":
                            exceptions.add(text);
                            break;
                        case "subject":
                        case "meetingtitle":
                        case "title":
                            subject = text;
                            break;
                        case "location":
                            location = text;
                            break;
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

    private LocalDateTime tryParseDateTime(String text) {
        // Simple fallback parsing for common date/time patterns
        try {
            // This is a basic implementation - can be enhanced with better date parsing
            if (text.toLowerCase().contains("tomorrow")) {
                return LocalDateTime.now().plusDays(1);
            }
            // Add more patterns as needed
        } catch (Exception e) {
            log.warn("Could not parse datetime text: {}", text, e);
        }
        return null;
    }
}
