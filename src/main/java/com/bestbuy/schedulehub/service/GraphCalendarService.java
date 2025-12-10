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

/**
 * Service for interacting with Microsoft Graph API to manage calendar events.
 * 
 * Handles:
 * - Creating calendar events (BookMeeting)
 * - Canceling calendar events (CancelMeeting)
 * - Rescheduling calendar events (RescheduleMeeting)
 * - Managing recurring events with exceptions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphCalendarService {

    private final GraphServiceClient<Request> graphServiceClient;

    /**
     * Validates that userId is provided and not "me" for application
     * authentication.
     * With application authentication, .me() is not supported - must use
     * .users(userId).
     * 
     * Valid formats:
     * - Email address: user@domain.com
     * - Object ID: 12345678-1234-1234-1234-123456789abc (GUID)
     */
    private void validateUserId(String userId, String operation) {
        if (userId == null || "me".equals(userId)) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌ USER ID REQUIRED FOR APPLICATION AUTHENTICATION");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("Operation: {}", operation);
            log.error("With application permissions (service-driven), you must provide a specific user ID.");
            log.error("The '/me' endpoint is not available with application permissions.");
            log.error("Please provide a user ID (email or object ID) in the X-User-Id header.");
            log.error("Example: X-User-Id: user@example.com or X-User-Id: <object-id>");
            log.error("═══════════════════════════════════════════════════════════════");
            throw new IllegalArgumentException(
                    String.format(
                            "User ID is required for application authentication (%s). Please provide X-User-Id header with a valid user email or object ID.",
                            operation));
        }

        // Validate format: must be email (contains @) or object ID (GUID format)
        String trimmedUserId = userId.trim();
        boolean isValidEmail = trimmedUserId.contains("@") && trimmedUserId.length() > 3;
        boolean isValidObjectId = trimmedUserId
                .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

        if (!isValidEmail && !isValidObjectId) {
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌ INVALID USER ID FORMAT");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("Operation: {}", operation);
            log.error("Provided User ID: '{}'", userId);
            log.error("");
            log.error("Microsoft Graph API requires one of the following formats:");
            log.error("1. Email address: user@domain.com (e.g., zhangzhijiang@yourdomain.com)");
            log.error("2. Object ID: GUID format (e.g., 12345678-1234-1234-1234-123456789abc)");
            log.error("");
            log.error("Your input '{}' does not match either format.", userId);
            log.error("");
            log.error("To find the correct user identifier:");
            log.error("1. Go to Azure Portal → Azure Active Directory → Users");
            log.error("2. Find the user and check:");
            log.error("   - User principal name (email): Use this directly");
            log.error("   - Object ID: Copy the GUID from the Object ID field");
            log.error("═══════════════════════════════════════════════════════════════");
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid user ID format: '%s'. User ID must be either:\n" +
                                    "1. Email address (e.g., user@domain.com)\n" +
                                    "2. Object ID (GUID format, e.g., 12345678-1234-1234-1234-123456789abc)\n" +
                                    "Provided value does not match either format.",
                            userId));
        }

        log.debug("User ID format validated: {} (email: {}, objectId: {})",
                userId, isValidEmail, isValidObjectId);
    }

    public String createEvent(ExtractedEntities entities, String userId) {
        try {
            Event event = new Event();
            event.subject = entities.getSubject() != null ? entities.getSubject() : "Meeting";
            event.body = new ItemBody();
            event.body.contentType = BodyType.TEXT;
            event.body.content = "Meeting created via Schedule Hub";

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

            // Add attendees - Graph API expects Attendee objects with emailAddress
            // (emailAddress.address and emailAddress.name)
            if (entities.getAttendees() != null && !entities.getAttendees().isEmpty()) {
                List<Attendee> attendees = entities.getAttendees().stream()
                        .map(attendeeInput -> {
                            Attendee attendee = new Attendee();
                            attendee.emailAddress = new EmailAddress();
                            // If input contains @, treat as email; otherwise assume it's a name and
                            // construct email
                            if (attendeeInput.contains("@")) {
                                attendee.emailAddress.address = attendeeInput;
                                // Extract name from email (part before @) or use full email
                                String name = attendeeInput.substring(0, attendeeInput.indexOf("@"));
                                attendee.emailAddress.name = name.substring(0, 1).toUpperCase() +
                                        (name.length() > 1 ? name.substring(1) : "");
                            } else {
                                // Name only - construct email (in production, you'd look this up)
                                attendee.emailAddress.name = attendeeInput;
                                attendee.emailAddress.address = attendeeInput.toLowerCase().replace(" ", ".")
                                        + "@example.com";
                            }
                            attendee.type = AttendeeType.REQUIRED;
                            return attendee;
                        })
                        .collect(Collectors.toList());
                event.attendees = attendees;
            }

            // Set location - Graph API expects Location object with displayName property,
            // not a plain string
            if (entities.getLocation() != null && !entities.getLocation().trim().isEmpty()) {
                Location location = new Location();
                location.displayName = entities.getLocation();
                event.location = location;
            }

            // Handle recurrence - Graph API expects PatternedRecurrence object (not a
            // string)
            if (entities.getRecurrencePattern() != null && !entities.getRecurrencePattern().isEmpty()) {
                event.recurrence = createRecurrencePattern(entities);
            }

            // Create event
            // With application authentication, .me() is not supported - must use
            // .users(userId)
            validateUserId(userId, "createEvent");

            // Log the Graph API URL being called for debugging
            String graphApiUrl = String.format("https://graph.microsoft.com/v1.0/users/%s/calendar/events", userId);
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("📤 Calling Microsoft Graph API");
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("URL: {}", graphApiUrl);
            log.info("User ID: {}", userId);
            log.info("Method: POST");
            log.info("═══════════════════════════════════════════════════════════════");

            Event createdEvent = graphServiceClient
                    .users(userId)
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
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌ Error creating calendar event");
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("Exception type: {}", e.getClass().getSimpleName());
            log.error("Exception message: {}", e.getMessage());

            // Traverse exception chain to find root cause message
            Throwable cause = e;
            String rootCauseMessage = e.getMessage();
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
                if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                    rootCauseMessage = cause.getMessage();
                }
            }

            log.error("Root cause: {}", cause.getClass().getSimpleName());
            log.error("Root cause message: {}", rootCauseMessage);

            // Check for authentication errors in the full exception chain
            String fullErrorMessage = rootCauseMessage;
            if (e.getMessage() != null) {
                fullErrorMessage = e.getMessage() + " | " + rootCauseMessage;
            }

            if (fullErrorMessage != null) {
                // Check for 404 errors (user not found or permissions issue)
                if (fullErrorMessage.contains("404")
                        || fullErrorMessage.contains("NotFound")
                        || fullErrorMessage.contains("ResourceNotFound")
                        || e.getMessage() != null && e.getMessage().contains("404")) {
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("❌ GRAPH API 404 ERROR - User Not Found or Permission Issue");
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("The Graph API returned a 404 error for user: {}", userId);
                    log.error("URL attempted: https://graph.microsoft.com/v1.0/users/{}/calendar/events", userId);
                    log.error("");
                    log.error("Possible causes:");
                    log.error("1. User doesn't exist in Azure AD tenant");
                    log.error("   - Verify user exists: Azure Portal → Azure AD → Users");
                    log.error("   - Check the exact email address or Object ID");
                    log.error("");
                    log.error("2. Application permissions not configured correctly");
                    log.error("   - Go to Azure Portal → Azure AD → App registrations");
                    log.error("   - Select your app: {}", "a8445943-d967-4c1f-9701-4e167c999eb4");
                    log.error("   - Go to 'API permissions'");
                    log.error("   - Verify you have APPLICATION permissions (not Delegated):");
                    log.error("     ✅ Calendars.ReadWrite (Application permission)");
                    log.error("     ✅ User.Read.All (Application permission)");
                    log.error("   - Check that admin consent is GRANTED (green checkmark)");
                    log.error("");
                    log.error("3. User exists but in different tenant");
                    log.error("   - Verify the user belongs to tenant: {}", "4c626fb1-0203-4c28-bd18-e451f44d0048");
                    log.error("");
                    log.error("4. User account is disabled or deleted");
                    log.error("   - Check user status in Azure Portal");
                    log.error("");
                    log.error("How to verify Azure AD configuration:");
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("Step 1: Verify User Exists");
                    log.error("   Azure Portal → Azure Active Directory → Users");
                    log.error("   Search for: {}", userId);
                    log.error("   Check: User principal name should match exactly");
                    log.error("");
                    log.error("Step 2: Verify Application Permissions");
                    log.error("   Azure Portal → Azure AD → App registrations");
                    log.error("   → Your app → API permissions");
                    log.error("   Required permissions:");
                    log.error("   - Calendars.ReadWrite (Application) - Status: Granted for [org]");
                    log.error("   - User.Read.All (Application) - Status: Granted for [org]");
                    log.error("");
                    log.error("Step 3: Verify Admin Consent");
                    log.error("   If permissions show 'Not granted', click 'Grant admin consent'");
                    log.error("");
                    log.error("Step 4: Test with Object ID instead of email");
                    log.error("   Try using the user's Object ID (GUID) instead of email");
                    log.error("   Find it in: Azure AD → Users → [User] → Object ID");
                    log.error("═══════════════════════════════════════════════════════════════");
                    throw new IllegalArgumentException(
                            String.format(
                                    "Graph API 404 Error: User '%s' not found or application lacks permissions. " +
                                            "Please verify: 1) User exists in Azure AD, 2) Application has Calendars.ReadWrite "
                                            +
                                            "and User.Read.All APPLICATION permissions with admin consent, 3) User belongs to the correct tenant. "
                                            +
                                            "Error: %s",
                                    userId, rootCauseMessage));
                }

                // Check for invalid user errors
                if (fullErrorMessage.contains("ErrorInvalidUser")
                        || fullErrorMessage.contains("invalid user")
                        || (fullErrorMessage.contains("requested user") && fullErrorMessage.contains("invalid"))) {
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("❌ INVALID USER ID ERROR");
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("The user ID provided is not valid or not found in Azure AD.");
                    log.error("");
                    log.error("Error: {}", rootCauseMessage);
                    log.error("User ID provided: {}", userId);
                    log.error("");
                    log.error("Common causes:");
                    log.error("1. Username only provided (e.g., 'zhangzhijiang')");
                    log.error("   ❌ Wrong: zhangzhijiang");
                    log.error("   ✅ Correct: zhangzhijiang@yourdomain.com");
                    log.error("");
                    log.error("2. User doesn't exist in your Azure AD tenant");
                    log.error("   - Verify the user exists in Azure Portal → Azure AD → Users");
                    log.error("");
                    log.error("3. Wrong format - must be email or object ID");
                    log.error("   ✅ Email: user@domain.com");
                    log.error("   ✅ Object ID: 12345678-1234-1234-1234-123456789abc");
                    log.error("");
                    log.error("To find the correct user identifier:");
                    log.error("1. Go to Azure Portal → Azure Active Directory → Users");
                    log.error("2. Search for the user");
                    log.error("3. Use the 'User principal name' (email) or 'Object ID' (GUID)");
                    log.error("═══════════════════════════════════════════════════════════════");
                    throw new IllegalArgumentException(
                            String.format(
                                    "Invalid user ID: '%s'. The user was not found in Azure AD. " +
                                            "Please provide a valid email address (e.g., user@domain.com) or Object ID (GUID). "
                                            +
                                            "Error: %s",
                                    userId, rootCauseMessage));
                }

                if (fullErrorMessage.contains("AADSTS7000215") || fullErrorMessage.contains("Invalid client secret")) {
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("❌ AZURE AD AUTHENTICATION ERROR");
                    log.error("═══════════════════════════════════════════════════════════════");
                    log.error("The Azure AD client secret is invalid or expired.");
                    log.error("");
                    log.error("Error Code: AADSTS7000215");
                    log.error("Error: Invalid client secret provided.");
                    log.error("");
                    log.error("To fix this:");
                    log.error("1. Go to Azure Portal → Azure Active Directory → App registrations");
                    log.error("2. Select your app: {}", "a8445943-d967-4c1f-9701-4e167c999eb4");
                    log.error("3. Go to 'Certificates & secrets'");
                    log.error("4. Check if your secret has expired (look at 'Expires' column)");
                    log.error("5. If expired or missing, create a new client secret:");
                    log.error("   - Click 'New client secret'");
                    log.error("   - Description: 'Schedule Hub Secret'");
                    log.error("   - Expires: Choose 12 or 24 months");
                    log.error("   - Click 'Add'");
                    log.error("6. ⚠️  CRITICAL: Copy the 'Value' column (NOT the 'Secret ID')");
                    log.error("   - Secret Value: Long random string (40+ characters)");
                    log.error("   - Secret ID: GUID format (like 9872436b-1c57-48a3-93f6-11d4d283596a)");
                    log.error("7. Update application.yml with the new secret VALUE");
                    log.error("8. Restart the application");
                    log.error("");
                    log.error("Common mistakes:");
                    log.error("❌ Copying Secret ID (GUID) instead of Secret Value (long string)");
                    log.error("❌ Secret has expired (check expiration date in Azure Portal)");
                    log.error("❌ Extra spaces or newlines in the secret");
                    log.error("❌ Using quotes around the secret value in application.yml");
                    log.error("═══════════════════════════════════════════════════════════════");
                } else if (fullErrorMessage.contains("AADSTS700016")
                        || fullErrorMessage.contains("Application not found")) {
                    log.error("Application not found. Verify client-id in application.yml");
                } else if (fullErrorMessage.contains("AADSTS70011") || fullErrorMessage.contains("Invalid scope")) {
                    log.error("Invalid scope. Verify API permissions are granted with admin consent");
                }
            }

            log.error("Full stack trace:", e);
            log.error("═══════════════════════════════════════════════════════════════");
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
            validateUserId(userId, "applyExceptions");

            // Get all instances of the recurring event
            EventCollectionPage instances = graphServiceClient
                    .users(userId)
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
                        .users(userId)
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
            validateUserId(userId, "deleteEvent");

            graphServiceClient
                    .users(userId)
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

            // Update attendees - Graph API expects Attendee objects with emailAddress
            if (entities.getAttendees() != null && !entities.getAttendees().isEmpty()) {
                List<Attendee> attendees = entities.getAttendees().stream()
                        .map(attendeeInput -> {
                            Attendee attendee = new Attendee();
                            attendee.emailAddress = new EmailAddress();
                            if (attendeeInput.contains("@")) {
                                attendee.emailAddress.address = attendeeInput;
                                String name = attendeeInput.substring(0, attendeeInput.indexOf("@"));
                                attendee.emailAddress.name = name.substring(0, 1).toUpperCase() +
                                        (name.length() > 1 ? name.substring(1) : "");
                            } else {
                                attendee.emailAddress.name = attendeeInput;
                                attendee.emailAddress.address = attendeeInput.toLowerCase().replace(" ", ".")
                                        + "@example.com";
                            }
                            attendee.type = AttendeeType.REQUIRED;
                            return attendee;
                        })
                        .collect(Collectors.toList());
                event.attendees = attendees;
            }

            // Update location - Graph API expects Location object with displayName property
            if (entities.getLocation() != null && !entities.getLocation().trim().isEmpty()) {
                Location location = new Location();
                location.displayName = entities.getLocation();
                event.location = location;
            }

            validateUserId(userId, "updateEvent");

            graphServiceClient
                    .users(userId)
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
            String filter = "";
            if (startDate != null && endDate != null) {
                filter = String.format("start/dateTime ge '%s' and start/dateTime le '%s'",
                        startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            validateUserId(userId, "findEvents");

            EventCollectionPage events = graphServiceClient
                    .users(userId)
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
