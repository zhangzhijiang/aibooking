package com.bestbuy.schedulehub.service;

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
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for extracting intents and entities from natural language text using
 * OpenAI API.
 * 
 * This service replaces the previous CLU (Conversational Language
 * Understanding) implementation
 * with OpenAI GPT models for better natural language understanding without
 * requiring training.
 * 
 * The service uses structured prompts to extract:
 * - Intent: BookMeeting, CancelMeeting, or RescheduleMeeting
 * - Entities: Attendees, DateTime, Subject, Location, Recurrence, Exceptions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-5-chat}")
    private String openAiModel;

    @Value("${openai.endpoint:}")
    private String openAiEndpoint; // Optional: Azure OpenAI endpoint (e.g.,
                                   // https://shedulehubopenai.openai.azure.com)

    @Value("${openai.deployment-name:}")
    private String deploymentName; // Optional: Azure OpenAI deployment name

    @Value("${openai.api-version:2025-01-01-preview}")
    private String apiVersion; // Azure OpenAI API version

    @PostConstruct
    public void logConfiguration() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔧 OpenAI Service Configuration");
        log.info("═══════════════════════════════════════════════════════════════");
        boolean isAzure = openAiEndpoint != null && !openAiEndpoint.trim().isEmpty();
        log.info("Service Type: {}", isAzure ? "Azure OpenAI" : "Standard OpenAI");
        log.info("API Key: {} (length: {})",
                openAiApiKey != null && openAiApiKey.length() > 10
                        ? openAiApiKey.substring(0, 10) + "..."
                        : "null or empty",
                openAiApiKey != null ? openAiApiKey.length() : 0);
        log.info("Model/Deployment: {}", openAiModel);
        if (isAzure) {
            log.info("Endpoint: {}", openAiEndpoint);
            log.info("Deployment Name: {}",
                    deploymentName != null && !deploymentName.trim().isEmpty()
                            ? deploymentName
                            : openAiModel + " (using model as deployment)");
            log.info("API Version: {}", apiVersion);
            String baseUrl = openAiEndpoint.endsWith("/")
                    ? openAiEndpoint.substring(0, openAiEndpoint.length() - 1)
                    : openAiEndpoint;
            String deployment = deploymentName != null && !deploymentName.trim().isEmpty()
                    ? deploymentName
                    : openAiModel;
            String fullUrl = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                    baseUrl, deployment, apiVersion);
            log.info("Full URL: {}", fullUrl);
        } else {
            log.info("Using standard OpenAI endpoint: https://api.openai.com/v1/chat/completions");
        }
        log.info("═══════════════════════════════════════════════════════════════");
    }

    private static final String SYSTEM_PROMPT = """
            You are a calendar scheduling assistant. Extract intent and entities from user requests about scheduling meetings.

            Extract the following information:
            - Intent: One of "BookMeeting", "CancelMeeting", or "RescheduleMeeting"
            - Attendees: List of person names or email addresses mentioned (e.g., ["John", "mary@example.com", "Bob Smith"])
            - Start DateTime: When the meeting starts (ISO 8601 format: yyyy-MM-ddTHH:mm). Use current date as reference for relative dates like "tomorrow", "next Friday", "3PM today"
            - End DateTime: When the meeting ends (ISO 8601 format: yyyy-MM-ddTHH:mm). If not specified, calculate 1 hour after start time
            - Subject: Meeting title/subject (default to "Meeting" if not specified)
            - Location: Meeting location name if mentioned (e.g., "Conference Room A", "Building 5", "Teams Meeting")
            - Recurrence Pattern: If recurring, use one of: "daily", "weekly", "weekday", "monthly" (or null if not recurring)
            - Exceptions: List of exception rules for recurring meetings (e.g., ["monday", "first tuesday", "second friday"] or null)

            Current date and time context: Use this to resolve relative dates like "tomorrow", "today", "next week", etc.

            Return ONLY valid JSON in this exact format (use null for optional fields that are not mentioned):
            {
              "intent": "BookMeeting",
              "attendees": ["John", "mary@example.com"],
              "startDateTime": "2025-12-10T15:00",
              "endDateTime": "2025-12-10T16:00",
              "subject": "Meeting with John",
              "location": null,
              "recurrencePattern": null,
              "exceptions": null
            }

            IMPORTANT JSON Format Rules:
            - All date/time values MUST be in ISO 8601 format: yyyy-MM-ddTHH:mm (use literal 'T' between date and time)
            - Use null (not empty string or "null" string) for optional fields that are not mentioned
            - Attendees array can contain names or email addresses (both are acceptable)
            - Location should be a string (location name) or null
            - RecurrencePattern should be one of: "daily", "weekly", "weekday", "monthly", or null
            - Exceptions should be an array of strings (e.g., ["monday", "first tuesday"]) or null
            - Subject should never be null (use "Meeting" as default if not specified)

            Date/Time Rules:
            - Use current date as reference for relative dates like "tomorrow", "next Friday"
            - If only time is given (e.g., "3PM"), assume today's date
            - If no end time specified, calculate 1 hour after start time
            - Always use 24-hour format (e.g., "15:00" for 3PM, "09:30" for 9:30AM)

            Intent Classification Rules:
            - Intent must be exactly one of: BookMeeting, CancelMeeting, RescheduleMeeting
            - For "book", "schedule", "create", "set up", "add" -> BookMeeting
            - For "cancel", "delete", "remove", "delete" -> CancelMeeting
            - For "reschedule", "move", "change", "update", "modify" -> RescheduleMeeting
            """;

    /**
     * Extracts intent and entities from natural language text using OpenAI API.
     * 
     * @param text The user's natural language scheduling request
     * @return ExtractedEntities containing intent, attendees, dates, and other
     *         meeting details
     */
    public ExtractedEntities extractIntentAndEntities(String text) {
        log.info("=== OpenAI Service: Starting intent and entity extraction ===");
        log.info("Input text: '{}'", text);

        // Determine if using Azure OpenAI or standard OpenAI
        boolean isAzureOpenAI = openAiEndpoint != null && !openAiEndpoint.trim().isEmpty();

        // Validate API key format based on service type
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty() ||
                openAiApiKey.equals("sk-your-openai-api-key-here")) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌ INVALID OPENAI API KEY");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("OpenAI API key is missing or invalid.");
            if (isAzureOpenAI) {
                log.error("Azure OpenAI API key is required. Get it from Azure Portal.");
            } else {
                log.error("API keys must start with 'sk-' and be obtained from: https://platform.openai.com/api-keys");
            }
            log.error("Please update your application.yml with a valid OpenAI API key.");
            log.error("═══════════════════════════════════════════════════════════════");
            return ExtractedEntities.builder()
                    .intent("Unknown")
                    .build();
        }

        // Validate standard OpenAI key format (only if not using Azure)
        if (!isAzureOpenAI && !openAiApiKey.startsWith("sk-")) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌ INVALID OPENAI API KEY FORMAT");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("Standard OpenAI API keys must start with 'sk-'.");
            log.error("If you're using Azure OpenAI, set openai.endpoint in application.yml");
            log.error("Current key format: {}",
                    openAiApiKey.length() > 10 ? openAiApiKey.substring(0, 10) + "..." : openAiApiKey);
            log.error("═══════════════════════════════════════════════════════════════");
            return ExtractedEntities.builder()
                    .intent("Unknown")
                    .build();
        }

        try {
            // Build URL based on service type
            String url;
            if (isAzureOpenAI) {
                // Azure OpenAI endpoint format
                String baseUrl = openAiEndpoint.endsWith("/")
                        ? openAiEndpoint.substring(0, openAiEndpoint.length() - 1)
                        : openAiEndpoint;
                String deployment = deploymentName != null && !deploymentName.trim().isEmpty()
                        ? deploymentName
                        : openAiModel; // Use model name as deployment if not specified
                url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                        baseUrl, deployment, apiVersion);
                log.info("Using Azure OpenAI endpoint: {}", url);
            } else {
                // Standard OpenAI endpoint
                url = "https://api.openai.com/v1/chat/completions";
                log.info("Using standard OpenAI endpoint: {}", url);
            }

            // Get current date/time for context
            LocalDateTime now = LocalDateTime.now();
            String dateContext = String.format("Current date: %s, Current time: %s",
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    now.format(DateTimeFormatter.ofPattern("HH:mm")));

            Map<String, Object> requestBody = new HashMap<>();
            // Azure OpenAI doesn't use "model" field, it's in the URL
            if (!isAzureOpenAI) {
                requestBody.put("model", openAiModel);
            }
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);
            requestBody.put("temperature", 0.1);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT + "\n\n" + dateContext));
            messages.add(Map.of("role", "user", "content", text));
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📤 OpenAI API Request Details");
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("URL: {}", url);
            log.info("Service Type: {}", isAzureOpenAI ? "Azure OpenAI" : "Standard OpenAI");
            if (isAzureOpenAI) {
                log.info("Deployment: {}", deploymentName != null && !deploymentName.trim().isEmpty()
                        ? deploymentName
                        : openAiModel);
                log.info("API Version: {}", apiVersion);
                log.info("Header: api-key (length: {})", openAiApiKey != null ? openAiApiKey.length() : 0);
            } else {
                log.info("Model: {}", openAiModel);
                log.info("Header: Authorization: Bearer (key length: {})",
                        openAiApiKey != null ? openAiApiKey.length() : 0);
            }
            log.info("Request Body Size: {} bytes", jsonBody.length());
            log.info("Request Body Preview: {}",
                    jsonBody.length() > 500 ? jsonBody.substring(0, 500) + "..." : jsonBody);
            log.info("═══════════════════════════════════════════════════════════════");

            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json");

            // Azure OpenAI uses "api-key" header, standard OpenAI uses "Authorization:
            // Bearer"
            if (isAzureOpenAI) {
                requestBuilder.addHeader("api-key", openAiApiKey);
            } else {
                requestBuilder.addHeader("Authorization", "Bearer " + openAiApiKey);
            }

            Request request = requestBuilder.build();

            log.info("Sending OpenAI API request...");
            long startTime = System.currentTimeMillis();

            try (Response response = httpClient.newCall(request).execute()) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("OpenAI API response received in {} ms - Status: {} {}",
                        duration, response.code(), response.message());

                if (!response.isSuccessful()) {
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("❌ OpenAI API Call Failed");
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("Status Code: {} {}", response.code(), response.message());
                    log.error("Request URL: {}", url);
                    log.error("Service Type: {}", isAzureOpenAI ? "Azure OpenAI" : "Standard OpenAI");

                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("Error Response Body: {}", errorBody);

                    // Parse error details if available
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorJson = objectMapper.readValue(errorBody, Map.class);
                        if (errorJson.containsKey("error")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> error = (Map<String, Object>) errorJson.get("error");
                            log.error("Error Code: {}", error.get("code"));
                            log.error("Error Message: {}", error.get("message"));
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }

                    if (response.code() == 401) {
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("❌ OPENAI API AUTHENTICATION FAILED (401)");
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("Your OpenAI API key is invalid or expired.");
                        log.error("Please check your application.yml openai.api-key configuration.");
                        log.error("Get your API key from: https://platform.openai.com/api-keys");
                        log.error("═══════════════════════════════════════════════════════════════");
                    } else if (response.code() == 404) {
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("❌ RESOURCE NOT FOUND (404)");
                        log.error("═══════════════════════════════════════════════════════════════");
                        if (isAzureOpenAI) {
                            log.error("The Azure OpenAI deployment or endpoint was not found.");
                            log.error("Please verify:");
                            log.error("1. Endpoint: {}", openAiEndpoint);
                            log.error("2. Deployment Name: {}",
                                    deploymentName != null && !deploymentName.trim().isEmpty()
                                            ? deploymentName
                                            : openAiModel);
                            log.error("3. API Version: {}", apiVersion);
                            log.error("4. Check Azure Portal → Your OpenAI Resource → Deployments");
                            log.error("   - Verify the deployment name matches exactly (case-sensitive)");
                            log.error("   - Verify the deployment is active and not deleted");
                            log.error("5. Common API versions: 2024-02-15-preview, 2024-06-01, 2024-08-01-preview");
                            log.error("   Current API version: {}", apiVersion);
                            log.error("6. Verify the endpoint URL is correct (should end with .azure.com)");
                        } else {
                            log.error("The OpenAI endpoint was not found.");
                            log.error("Verify the endpoint URL is correct.");
                        }
                        log.error("═══════════════════════════════════════════════════════════════");
                    } else if (response.code() == 403) {
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("❌ FORBIDDEN (403)");
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("Access denied. Possible causes:");
                        log.error("1. API key doesn't have permission for this resource");
                        log.error("2. Resource is in a different subscription");
                        log.error("3. Network restrictions or firewall blocking access");
                        log.error("═══════════════════════════════════════════════════════════════");
                    }

                    return ExtractedEntities.builder()
                            .intent("Unknown")
                            .build();
                }

                String responseBody = response.body().string();
                log.debug("OpenAI API response body: {}", responseBody);

                // Parse OpenAI response
                @SuppressWarnings("unchecked")
                Map<String, Object> openAiResponse = objectMapper.readValue(responseBody, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) openAiResponse.get("choices");
                if (choices == null || choices.isEmpty()) {
                    log.error("No choices in OpenAI response");
                    return ExtractedEntities.builder().intent("Unknown").build();
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                log.info("OpenAI extracted content: {}", content);

                // Parse the JSON content
                @SuppressWarnings("unchecked")
                Map<String, Object> extractedData = (Map<String, Object>) objectMapper.readValue(content, Map.class);

                // Log the raw extracted data for debugging
                log.debug("Raw extracted data from OpenAI: {}", extractedData);

                ExtractedEntities result = parseOpenAIResponse(extractedData);

                log.info("=== OpenAI Service: Extraction complete ===");
                log.info(
                        "Final extracted entities: intent={}, startDateTime={}, endDateTime={}, attendees={}, subject={}, location={}",
                        result.getIntent(), result.getStartDateTime(), result.getEndDateTime(),
                        result.getAttendees(), result.getSubject(), result.getLocation());
                return result;
            }
        } catch (IOException e) {
            log.error("IOException calling OpenAI API", e);
            return ExtractedEntities.builder()
                    .intent("Unknown")
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error in OpenAI service", e);
            return ExtractedEntities.builder()
                    .intent("Unknown")
                    .build();
        }
    }

    private ExtractedEntities parseOpenAIResponse(Map<String, Object> data) {
        log.info("=== Parsing OpenAI Response ===");
        ExtractedEntities.ExtractedEntitiesBuilder builder = ExtractedEntities.builder();

        // Intent
        String intent = (String) data.get("intent");
        log.info("Extracted intent: {}", intent);
        builder.intent(intent != null ? intent : "Unknown");

        // Attendees
        Object attendeesObj = data.get("attendees");
        if (attendeesObj instanceof List) {
            List<String> attendees = new ArrayList<>();
            for (Object item : (List<?>) attendeesObj) {
                if (item != null) {
                    attendees.add(item.toString());
                }
            }
            log.info("Extracted attendees: {}", attendees);
            builder.attendees(attendees);
        }

        // Start DateTime
        String startDateTimeStr = (String) data.get("startDateTime");
        if (startDateTimeStr != null && !startDateTimeStr.isEmpty()) {
            try {
                LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                log.info("Parsed startDateTime: {}", startDateTime);
                builder.startDateTime(startDateTime);
            } catch (Exception e) {
                log.warn("Could not parse startDateTime: {}", startDateTimeStr, e);
            }
        }

        // End DateTime
        String endDateTimeStr = (String) data.get("endDateTime");
        if (endDateTimeStr != null && !endDateTimeStr.isEmpty()) {
            try {
                LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                log.info("Parsed endDateTime: {}", endDateTime);
                builder.endDateTime(endDateTime);
            } catch (Exception e) {
                log.warn("Could not parse endDateTime: {}", endDateTimeStr, e);
            }
        }

        // Subject - provide default if null or empty
        Object subjectObj = data.get("subject");
        String subject = (subjectObj != null && !subjectObj.toString().equalsIgnoreCase("null"))
                ? subjectObj.toString().trim()
                : null;
        if (subject == null || subject.isEmpty()) {
            subject = "Meeting"; // Default subject
            log.info("Subject not provided, using default: {}", subject);
        } else {
            log.info("Extracted subject: {}", subject);
        }
        builder.subject(subject);

        // Location - handle null, empty string, and "null" string
        Object locationObj = data.get("location");
        if (locationObj != null && !locationObj.toString().equalsIgnoreCase("null")) {
            String location = locationObj.toString().trim();
            if (!location.isEmpty()) {
                log.info("Extracted location: {}", location);
                builder.location(location);
            }
        }

        // Recurrence Pattern - handle null, empty string, and "null" string
        Object recurrencePatternObj = data.get("recurrencePattern");
        if (recurrencePatternObj != null && !recurrencePatternObj.toString().equalsIgnoreCase("null")) {
            String recurrencePattern = recurrencePatternObj.toString().trim();
            if (!recurrencePattern.isEmpty()) {
                log.info("Extracted recurrencePattern: {}", recurrencePattern);
                builder.recurrencePattern(recurrencePattern);
            }
        }

        // Exceptions
        Object exceptionsObj = data.get("exceptions");
        if (exceptionsObj instanceof List) {
            List<String> exceptions = new ArrayList<>();
            for (Object item : (List<?>) exceptionsObj) {
                if (item != null) {
                    exceptions.add(item.toString());
                }
            }
            if (!exceptions.isEmpty()) {
                log.info("Extracted exceptions: {}", exceptions);
                builder.exceptions(exceptions);
            }
        }

        ExtractedEntities result = builder.build();
        log.info("=== OpenAI Response Parsing Complete ===");
        return result;
    }
}
