package com.bestbuy.schedulehub.service;

import com.bestbuy.schedulehub.dto.ScheduleResponse;
import com.bestbuy.schedulehub.dto.ExtractedEntities;
import com.microsoft.graph.models.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Main service for processing scheduling requests.
 * 
 * Orchestrates the flow:
 * 1. Extract intent and entities from natural language using OpenAI
 * 2. Route to appropriate handler (BookMeeting, CancelMeeting,
 * RescheduleMeeting)
 * 3. Interact with Microsoft Graph API to manage calendar events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final OpenAIService openAIService;
    private final GraphCalendarService graphCalendarService;

    public ScheduleResponse processScheduleRequest(String text, String userId) {
        log.info("=== Schedule Service: Processing request ===");
        log.info("Input text: '{}'", text);
        log.info("User ID: {}", userId);

        try {
            // Extract intent and entities using OpenAI
            log.info("Calling OpenAI service to extract intent and entities...");
            ExtractedEntities entities = openAIService.extractIntentAndEntities(text);
            log.info("OpenAI extraction completed");
            log.info("Extracted intent: '{}'", entities.getIntent());
            log.info("Extracted entities details:");
            log.info("  - Attendees: {}", entities.getAttendees());
            log.info("  - Start DateTime: {}", entities.getStartDateTime());
            log.info("  - End DateTime: {}", entities.getEndDateTime());
            log.info("  - Subject: {}", entities.getSubject());
            log.info("  - Location: {}", entities.getLocation());
            log.info("  - Recurrence Pattern: {}", entities.getRecurrencePattern());
            log.info("  - Exceptions: {}", entities.getExceptions());

            String intent = entities.getIntent() != null ? entities.getIntent().toLowerCase() : "unknown";
            log.info("Normalized intent: '{}'", intent);

            // Prepare OpenAI output for UI
            Map<String, Object> openaiOutput = new HashMap<>();
            openaiOutput.put("intent", entities.getIntent());
            openaiOutput.put("attendees", entities.getAttendees());
            openaiOutput.put("startDateTime", entities.getStartDateTime());
            openaiOutput.put("endDateTime", entities.getEndDateTime());
            openaiOutput.put("subject", entities.getSubject());
            openaiOutput.put("location", entities.getLocation());
            openaiOutput.put("recurrencePattern", entities.getRecurrencePattern());
            openaiOutput.put("exceptions", entities.getExceptions());

            ScheduleResponse response;
            switch (intent) {
                case "bookmeeting":
                case "schedulemeeting":
                    log.info("Routing to handleBookMeeting");
                    response = handleBookMeeting(entities, userId, openaiOutput);
                    break;

                case "cancelmeeting":
                case "deletemeeting":
                    log.info("Routing to handleCancelMeeting");
                    response = handleCancelMeeting(entities, userId);
                    response.setOpenaiOutput(openaiOutput);
                    break;

                case "reschedulemeeting":
                case "updatemeeting":
                    log.info("Routing to handleRescheduleMeeting");
                    response = handleRescheduleMeeting(entities, userId);
                    response.setOpenaiOutput(openaiOutput);
                    break;

                default:
                    if ("openai_api_error".equalsIgnoreCase(intent) || "unknown".equalsIgnoreCase(intent)) {
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("❌ OpenAI API ERROR - Cannot process request");
                        log.error("═══════════════════════════════════════════════════════════════");
                        log.error("The OpenAI API call failed or returned an unknown intent.");
                        log.error("This is preventing intent and entity extraction.");
                        log.error("Please check the logs above for detailed error information.");
                        log.error("═══════════════════════════════════════════════════════════════");
                        response = ScheduleResponse.builder()
                                .status("error")
                                .message(
                                        "OpenAI API error. Please check your OpenAI API key in application.yml. See logs for details.")
                                .build();
                    } else {
                        log.warn("Unknown intent: '{}'", intent);
                        response = ScheduleResponse.builder()
                                .status("error")
                                .message("Unknown intent: " + intent
                                        + ". The AI service may not have recognized your request. Try rephrasing your request.")
                                .openaiOutput(openaiOutput)
                                .build();
                    }
                    break;
            }

            // Set OpenAI output for all responses
            if (response != null && response.getOpenaiOutput() == null) {
                response.setOpenaiOutput(openaiOutput);
            }

            log.info("=== Schedule Service: Request processing complete ===");
            log.info("Response status: {}, message: {}", response.getStatus(), response.getMessage());
            return response;
        } catch (Exception e) {
            log.error("Exception processing schedule request", e);
            log.error("Exception details - Message: {}, Cause: {}, Stack trace:",
                    e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to process request: " + e.getMessage())
                    .build();
        }
    }

    private ScheduleResponse handleBookMeeting(ExtractedEntities entities, String userId,
            Map<String, Object> openaiOutput) {
        log.info("=== Handling BookMeeting request ===");
        log.info("Entities: startDateTime={}, endDateTime={}, subject={}, attendees={}",
                entities.getStartDateTime(), entities.getEndDateTime(),
                entities.getSubject(), entities.getAttendees());

        try {
            // Prepare Graph API input for UI
            Map<String, Object> graphApiInput = new HashMap<>();
            graphApiInput.put("subject", entities.getSubject() != null ? entities.getSubject() : "Meeting");
            graphApiInput.put("startDateTime", entities.getStartDateTime());
            graphApiInput.put("endDateTime", entities.getEndDateTime());
            graphApiInput.put("timeZone", java.time.ZoneId.systemDefault().getId());
            graphApiInput.put("attendees", entities.getAttendees());
            graphApiInput.put("location", entities.getLocation());
            graphApiInput.put("recurrencePattern", entities.getRecurrencePattern());
            graphApiInput.put("exceptions", entities.getExceptions());
            graphApiInput.put("userId", userId);

            log.info("Calling GraphCalendarService.createEvent...");
            String eventId = graphCalendarService.createEvent(entities, userId);
            log.info("Event created successfully with ID: {}", eventId);

            // Prepare booking results
            List<Map<String, Object>> bookingResults = new ArrayList<>();
            Map<String, Object> bookingResult = new HashMap<>();
            bookingResult.put("eventId", eventId);
            bookingResult.put("subject", entities.getSubject() != null ? entities.getSubject() : "Meeting");
            bookingResult.put("startTime", entities.getStartDateTime());
            bookingResult.put("endTime", entities.getEndDateTime());
            bookingResult.put("attendees", entities.getAttendees());
            bookingResult.put("location", entities.getLocation());
            bookingResult.put("recurrencePattern", entities.getRecurrencePattern());
            bookingResult.put("status", "success");
            bookingResults.add(bookingResult);

            ScheduleResponse response = ScheduleResponse.builder()
                    .status("success")
                    .message("Meeting scheduled successfully")
                    .eventId(eventId)
                    .eventSubject(entities.getSubject())
                    .startTime(entities.getStartDateTime())
                    .endTime(entities.getEndDateTime())
                    .attendees(entities.getAttendees())
                    .recurrencePattern(entities.getRecurrencePattern())
                    .exceptions(entities.getExceptions())
                    .openaiOutput(openaiOutput)
                    .graphApiInput(graphApiInput)
                    .bookingResults(bookingResults)
                    .build();

            // ===== DETAILED BOOKING SUMMARY =====
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📅 MEETING BOOKED SUCCESSFULLY");
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("Event ID:        {}", eventId != null ? eventId : "N/A");
            log.info("Subject:         {}", entities.getSubject() != null ? entities.getSubject() : "Meeting");
            log.info("Start Time:      {}", entities.getStartDateTime() != null ? entities.getStartDateTime() : "N/A");
            log.info("End Time:        {}", entities.getEndDateTime() != null ? entities.getEndDateTime() : "N/A");

            if (entities.getAttendees() != null && !entities.getAttendees().isEmpty()) {
                log.info("Attendees:       {}", String.join(", ", entities.getAttendees()));
            } else {
                log.info("Attendees:       None");
            }

            if (entities.getLocation() != null && !entities.getLocation().isEmpty()) {
                log.info("Location:        {}", entities.getLocation());
            }

            if (entities.getRecurrencePattern() != null && !entities.getRecurrencePattern().isEmpty()) {
                log.info("Recurrence:      {}", entities.getRecurrencePattern());
            }

            if (entities.getExceptions() != null && !entities.getExceptions().isEmpty()) {
                log.info("Exceptions:      {}", String.join(", ", entities.getExceptions()));
            }

            log.info("User ID:         {}", userId);
            log.info("═══════════════════════════════════════════════════════════════");
            // ===== END BOOKING SUMMARY =====

            log.info("BookMeeting response prepared: status={}, eventId={}",
                    response.getStatus(), response.getEventId());
            return response;
        } catch (Exception e) {
            log.error("Error scheduling meeting", e);
            log.error("Exception details - Message: {}, Cause: {}",
                    e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to schedule meeting: " + e.getMessage())
                    .build();
        }
    }

    private ScheduleResponse handleCancelMeeting(ExtractedEntities entities, String userId) {
        log.info("=== Handling CancelMeeting request ===");
        log.info("Search criteria: subject={}, startDateTime={}, endDateTime={}",
                entities.getSubject(), entities.getStartDateTime(), entities.getEndDateTime());

        try {
            // Find the event to cancel
            log.info("Searching for events to cancel...");
            List<Event> events = graphCalendarService.findEvents(
                    entities.getSubject(),
                    entities.getStartDateTime(),
                    entities.getEndDateTime(),
                    userId);
            log.info("Found {} matching event(s)", events.size());

            if (events.isEmpty()) {
                log.warn("No matching events found to cancel");
                return ScheduleResponse.builder()
                        .status("error")
                        .message("No matching event found to cancel")
                        .build();
            }

            // Delete the first matching event
            Event eventToCancel = events.get(0);
            log.info("Cancelling event: id={}, subject={}", eventToCancel.id, eventToCancel.subject);
            graphCalendarService.deleteEvent(eventToCancel.id, userId);
            log.info("Event cancelled successfully");

            return ScheduleResponse.builder()
                    .status("success")
                    .message("Meeting cancelled successfully")
                    .eventId(eventToCancel.id)
                    .eventSubject(eventToCancel.subject)
                    .build();
        } catch (Exception e) {
            log.error("Error cancelling meeting", e);
            log.error("Exception details - Message: {}, Cause: {}",
                    e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to cancel meeting: " + e.getMessage())
                    .build();
        }
    }

    private ScheduleResponse handleRescheduleMeeting(ExtractedEntities entities, String userId) {
        log.info("=== Handling RescheduleMeeting request ===");
        log.info("Search criteria: subject={}", entities.getSubject());
        log.info("New schedule: startDateTime={}, endDateTime={}",
                entities.getStartDateTime(), entities.getEndDateTime());

        try {
            // Find the event to reschedule
            log.info("Searching for events to reschedule...");
            List<Event> events = graphCalendarService.findEvents(
                    entities.getSubject(),
                    null,
                    null,
                    userId);
            log.info("Found {} matching event(s)", events.size());

            if (events.isEmpty()) {
                log.warn("No matching events found to reschedule");
                return ScheduleResponse.builder()
                        .status("error")
                        .message("No matching event found to reschedule")
                        .build();
            }

            // Update the first matching event
            Event eventToUpdate = events.get(0);
            log.info("Rescheduling event: id={}, subject={}, current start={}, current end={}",
                    eventToUpdate.id, eventToUpdate.subject,
                    eventToUpdate.start != null ? eventToUpdate.start.dateTime : "N/A",
                    eventToUpdate.end != null ? eventToUpdate.end.dateTime : "N/A");
            graphCalendarService.updateEvent(eventToUpdate.id, entities, userId);
            log.info("Event rescheduled successfully");

            return ScheduleResponse.builder()
                    .status("success")
                    .message("Meeting rescheduled successfully")
                    .eventId(eventToUpdate.id)
                    .eventSubject(eventToUpdate.subject)
                    .startTime(entities.getStartDateTime())
                    .endTime(entities.getEndDateTime())
                    .build();
        } catch (Exception e) {
            log.error("Error rescheduling meeting", e);
            log.error("Exception details - Message: {}, Cause: {}",
                    e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to reschedule meeting: " + e.getMessage())
                    .build();
        }
    }
}
