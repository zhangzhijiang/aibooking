package com.bestbuy.schedulehub.service;

import com.bestbuy.schedulehub.dto.ScheduleResponse;
import com.bestbuy.schedulehub.dto.ExtractedEntities;
import com.microsoft.graph.models.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CluService cluService;
    private final GraphCalendarService graphCalendarService;

    public ScheduleResponse processScheduleRequest(String text, String userId) {
        try {
            // Extract intent and entities using CLU
            ExtractedEntities entities = cluService.extractIntentAndEntities(text);
            log.info("Extracted intent: {}, entities: {}", entities.getIntent(), entities);

            String intent = entities.getIntent() != null ? entities.getIntent().toLowerCase() : "unknown";

            switch (intent) {
                case "bookmeeting":
                case "schedulemeeting":
                    return handleBookMeeting(entities, userId);

                case "cancelmeeting":
                case "deletemeeting":
                    return handleCancelMeeting(entities, userId);

                case "reschedulemeeting":
                case "updatemeeting":
                    return handleRescheduleMeeting(entities, userId);

                default:
                    return ScheduleResponse.builder()
                            .status("error")
                            .message("Unknown intent: " + intent)
                            .build();
            }
        } catch (Exception e) {
            log.error("Error processing schedule request", e);
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to process request: " + e.getMessage())
                    .build();
        }
    }

    private ScheduleResponse handleBookMeeting(ExtractedEntities entities, String userId) {
        try {
            String eventId = graphCalendarService.createEvent(entities, userId);

            return ScheduleResponse.builder()
                    .status("success")
                    .message("Meeting scheduled successfully")
                    .eventId(eventId)
                    .eventSubject(entities.getSubject())
                    .startTime(entities.getStartDateTime())
                    .endTime(entities.getEndDateTime())
                    .attendees(entities.getAttendees())
                    .recurrencePattern(entities.getRecurrencePattern())
                    .exceptions(entities.getExceptions())
                    .build();
        } catch (Exception e) {
            log.error("Error scheduling meeting", e);
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to schedule meeting: " + e.getMessage())
                    .build();
        }
    }

    private ScheduleResponse handleCancelMeeting(ExtractedEntities entities, String userId) {
        try {
            // Find the event to cancel
            List<Event> events = graphCalendarService.findEvents(
                    entities.getSubject(),
                    entities.getStartDateTime(),
                    entities.getEndDateTime(),
                    userId);

            if (events.isEmpty()) {
                return ScheduleResponse.builder()
                        .status("error")
                        .message("No matching event found to cancel")
                        .build();
            }

            // Delete the first matching event
            Event eventToCancel = events.get(0);
            graphCalendarService.deleteEvent(eventToCancel.id, userId);

            return ScheduleResponse.builder()
                    .status("success")
                    .message("Meeting cancelled successfully")
                    .eventId(eventToCancel.id)
                    .eventSubject(eventToCancel.subject)
                    .build();
        } catch (Exception e) {
            log.error("Error cancelling meeting", e);
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to cancel meeting: " + e.getMessage())
                    .build();
        }
    }

    private ScheduleResponse handleRescheduleMeeting(ExtractedEntities entities, String userId) {
        try {
            // Find the event to reschedule
            List<Event> events = graphCalendarService.findEvents(
                    entities.getSubject(),
                    null,
                    null,
                    userId);

            if (events.isEmpty()) {
                return ScheduleResponse.builder()
                        .status("error")
                        .message("No matching event found to reschedule")
                        .build();
            }

            // Update the first matching event
            Event eventToUpdate = events.get(0);
            graphCalendarService.updateEvent(eventToUpdate.id, entities, userId);

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
            return ScheduleResponse.builder()
                    .status("error")
                    .message("Failed to reschedule meeting: " + e.getMessage())
                    .build();
        }
    }
}
