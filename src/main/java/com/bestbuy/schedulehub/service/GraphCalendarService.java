package com.bestbuy.schedulehub.service;

import com.bestbuy.schedulehub.dto.ExtractedEntities;
import com.microsoft.graph.core.DateOnly;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.EventCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphCalendarService {

    private final GraphServiceClient<Request> graphServiceClient;

    public String createEvent(ExtractedEntities entities, String userId) {
        try {
            Event event = new Event();
            event.subject = entities.getSubject() != null ? entities.getSubject() : "Meeting";
            event.body = new ItemBody();
            event.body.contentType = BodyType.TEXT;
            event.body.content = "Meeting created via speech assistant";

            // Set start and end times
            LocalDateTime start = entities.getStartDateTime() != null
                    ? entities.getStartDateTime()
                    : LocalDateTime.now().plusHours(1);
            LocalDateTime end = entities.getEndDateTime() != null
                    ? entities.getEndDateTime()
                    : start.plusHours(1);

            event.start = new DateTimeTimeZone();
            event.start.dateTime = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            event.start.timeZone = ZoneId.systemDefault().getId();

            event.end = new DateTimeTimeZone();
            event.end.dateTime = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            event.end.timeZone = ZoneId.systemDefault().getId();

            // Add attendees
            if (entities.getAttendees() != null && !entities.getAttendees().isEmpty()) {
                List<Attendee> attendees = entities.getAttendees().stream()
                        .map(email -> {
                            Attendee attendee = new Attendee();
                            attendee.emailAddress = new EmailAddress();
                            attendee.emailAddress.address = email.contains("@") ? email : email + "@example.com";
                            attendee.emailAddress.name = email;
                            attendee.type = AttendeeType.REQUIRED;
                            return attendee;
                        })
                        .collect(Collectors.toList());
                event.attendees = attendees;
            }

            // Handle recurrence
            if (entities.getRecurrencePattern() != null && !entities.getRecurrencePattern().isEmpty()) {
                event.recurrence = createRecurrencePattern(entities);
            }

            // Create event
            String userPath = userId != null ? "users/" + userId : "me";
            Event createdEvent = graphServiceClient
                    .users(userPath)
                    .calendar()
                    .events()
                    .buildRequest()
                    .post(event);

            log.info("Event created successfully: {}", createdEvent.id);

            // Handle exceptions if recurrence exists
            if (createdEvent.recurrence != null && entities.getExceptions() != null
                    && !entities.getExceptions().isEmpty()) {
                applyExceptions(createdEvent.id, entities, userId);
            }

            return createdEvent.id;
        } catch (Exception e) {
            log.error("Error creating event", e);
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }

    private PatternedRecurrence createRecurrencePattern(ExtractedEntities entities) {
        PatternedRecurrence recurrence = new PatternedRecurrence();
        RecurrencePattern pattern = new RecurrencePattern();
        RecurrenceRange range = new RecurrenceRange();

        String recurrenceText = entities.getRecurrencePattern().toLowerCase();

        // Parse recurrence pattern
        if (recurrenceText.contains("daily")) {
            pattern.type = RecurrencePatternType.DAILY;
            pattern.interval = 1;
        } else if (recurrenceText.contains("weekly")) {
            pattern.type = RecurrencePatternType.WEEKLY;
            pattern.interval = 1;

            // Extract days of week
            List<DayOfWeek> daysOfWeek = new ArrayList<>();
            if (recurrenceText.contains("monday") || recurrenceText.contains("weekday")) {
                daysOfWeek.add(DayOfWeek.MONDAY);
            }
            if (recurrenceText.contains("tuesday") || recurrenceText.contains("weekday")) {
                daysOfWeek.add(DayOfWeek.TUESDAY);
            }
            if (recurrenceText.contains("wednesday") || recurrenceText.contains("weekday")) {
                daysOfWeek.add(DayOfWeek.WEDNESDAY);
            }
            if (recurrenceText.contains("thursday") || recurrenceText.contains("weekday")) {
                daysOfWeek.add(DayOfWeek.THURSDAY);
            }
            if (recurrenceText.contains("friday") || recurrenceText.contains("weekday")) {
                daysOfWeek.add(DayOfWeek.FRIDAY);
            }
            if (recurrenceText.contains("saturday")) {
                daysOfWeek.add(DayOfWeek.SATURDAY);
            }
            if (recurrenceText.contains("sunday")) {
                daysOfWeek.add(DayOfWeek.SUNDAY);
            }

            if (!daysOfWeek.isEmpty()) {
                pattern.daysOfWeek = daysOfWeek;
            }
        } else if (recurrenceText.contains("monthly")) {
            pattern.type = RecurrencePatternType.ABSOLUTE_MONTHLY;
            pattern.interval = 1;
        } else {
            // Default to weekly
            pattern.type = RecurrencePatternType.WEEKLY;
            pattern.interval = 1;
        }

        // Set range (default: 6 months)
        range.type = RecurrenceRangeType.END_DATE;
        LocalDateTime startDate = entities.getStartDateTime() != null
                ? entities.getStartDateTime()
                : LocalDateTime.now();
        LocalDate startLocalDate = startDate.toLocalDate();
        LocalDate endLocalDate = startDate.plusMonths(6).toLocalDate();
        range.startDate = new DateOnly(startLocalDate.getYear(), startLocalDate.getMonthValue(),
                startLocalDate.getDayOfMonth());
        range.endDate = new DateOnly(endLocalDate.getYear(), endLocalDate.getMonthValue(),
                endLocalDate.getDayOfMonth());

        recurrence.pattern = pattern;
        recurrence.range = range;

        return recurrence;
    }

    private void applyExceptions(String eventId, ExtractedEntities entities, String userId) {
        try {
            String userPath = userId != null ? "users/" + userId : "me";

            // Get all instances of the recurring event
            EventCollectionPage instances = graphServiceClient
                    .users(userPath)
                    .calendar()
                    .events(eventId)
                    .instances()
                    .buildRequest()
                    .get();

            List<Event> eventsToDelete = new ArrayList<>();

            for (Event instance : instances.getCurrentPage()) {
                // Check if this instance matches any exception rule
                if (matchesException(instance, entities.getExceptions())) {
                    eventsToDelete.add(instance);
                }
            }

            // Delete matching instances
            for (Event eventToDelete : eventsToDelete) {
                graphServiceClient
                        .users(userPath)
                        .calendar()
                        .events(eventToDelete.id)
                        .buildRequest()
                        .delete();
                log.info("Deleted exception instance: {}", eventToDelete.id);
            }
        } catch (Exception e) {
            log.error("Error applying exceptions", e);
        }
    }

    private boolean matchesException(Event instance, List<String> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return false;
        }

        ZonedDateTime instanceStart = parseDateTime(instance.start.dateTime);
        String dayOfWeek = instanceStart.getDayOfWeek().toString().toLowerCase();

        for (String exception : exceptions) {
            String exc = exception.toLowerCase();

            // Check for "second Tuesday", "first Monday", etc.
            if (exc.contains("second") && dayOfWeek.contains("tuesday")) {
                int weekOfMonth = (instanceStart.getDayOfMonth() - 1) / 7 + 1;
                if (weekOfMonth == 2) {
                    return true;
                }
            }

            if (exc.contains("first") && dayOfWeek.contains("monday")) {
                int weekOfMonth = (instanceStart.getDayOfMonth() - 1) / 7 + 1;
                if (weekOfMonth == 1) {
                    return true;
                }
            }

            // Check for specific days
            if (exc.contains("monday") && dayOfWeek.contains("monday")) {
                return true;
            }
            if (exc.contains("tuesday") && dayOfWeek.contains("tuesday")) {
                return true;
            }
            if (exc.contains("wednesday") && dayOfWeek.contains("wednesday")) {
                return true;
            }
            if (exc.contains("thursday") && dayOfWeek.contains("thursday")) {
                return true;
            }
            if (exc.contains("friday") && dayOfWeek.contains("friday")) {
                return true;
            }
        }

        return false;
    }

    private ZonedDateTime parseDateTime(String dateTimeString) {
        try {
            return ZonedDateTime.parse(dateTimeString);
        } catch (Exception e) {
            log.warn("Could not parse datetime: {}", dateTimeString);
            return ZonedDateTime.now();
        }
    }

    public void deleteEvent(String eventId, String userId) {
        try {
            String userPath = userId != null ? "users/" + userId : "me";
            graphServiceClient
                    .users(userPath)
                    .calendar()
                    .events(eventId)
                    .buildRequest()
                    .delete();
            log.info("Event deleted successfully: {}", eventId);
        } catch (Exception e) {
            log.error("Error deleting event", e);
            throw new RuntimeException("Failed to delete event: " + e.getMessage(), e);
        }
    }

    public void updateEvent(String eventId, ExtractedEntities entities, String userId) {
        try {
            String userPath = userId != null ? "users/" + userId : "me";

            Event event = new Event();

            if (entities.getStartDateTime() != null) {
                event.start = new DateTimeTimeZone();
                event.start.dateTime = entities.getStartDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                event.start.timeZone = ZoneId.systemDefault().getId();
            }

            if (entities.getEndDateTime() != null) {
                event.end = new DateTimeTimeZone();
                event.end.dateTime = entities.getEndDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                event.end.timeZone = ZoneId.systemDefault().getId();
            }

            if (entities.getSubject() != null) {
                event.subject = entities.getSubject();
            }

            graphServiceClient
                    .users(userPath)
                    .calendar()
                    .events(eventId)
                    .buildRequest()
                    .patch(event);

            log.info("Event updated successfully: {}", eventId);
        } catch (Exception e) {
            log.error("Error updating event", e);
            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
    }

    public List<Event> findEvents(String subject, LocalDateTime startDate, LocalDateTime endDate, String userId) {
        try {
            String userPath = userId != null ? "users/" + userId : "me";

            String filter = "";
            if (startDate != null && endDate != null) {
                filter = String.format("start/dateTime ge '%s' and start/dateTime le '%s'",
                        startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            EventCollectionPage events = graphServiceClient
                    .users(userPath)
                    .calendar()
                    .events()
                    .buildRequest()
                    .filter(filter)
                    .get();

            List<Event> matchingEvents = new ArrayList<>();
            for (Event event : events.getCurrentPage()) {
                if (subject == null
                        || (event.subject != null && event.subject.toLowerCase().contains(subject.toLowerCase()))) {
                    matchingEvents.add(event);
                }
            }

            return matchingEvents;
        } catch (Exception e) {
            log.error("Error finding events", e);
            return new ArrayList<>();
        }
    }
}
