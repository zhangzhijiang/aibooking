package com.aibooking.service;

import com.aibooking.dto.BookingResponse;
import com.aibooking.dto.ExtractedEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private LuisService luisService;

    @Mock
    private GraphCalendarService graphCalendarService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void testProcessBookingRequest_SimpleBooking() {
        // Arrange
        ExtractedEntities entities = ExtractedEntities.builder()
                .intent("BookMeeting")
                .attendees(Arrays.asList("Mary"))
                .startDateTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0))
                .endDateTime(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0))
                .subject("Meeting with Mary")
                .build();

        when(luisService.extractIntentAndEntities(anyString())).thenReturn(entities);
        when(graphCalendarService.createEvent(any(ExtractedEntities.class), anyString()))
                .thenReturn("event-123");

        // Act
        BookingResponse response = bookingService.processBookingRequest(
                "Book Mary for 2 PM tomorrow", "me");

        // Assert
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("event-123", response.getEventId());
        assertTrue(response.getAttendees().contains("Mary"));
        verify(graphCalendarService, times(1)).createEvent(any(ExtractedEntities.class), anyString());
    }

    @Test
    void testProcessBookingRequest_RecurringBookingWithException() {
        // Arrange
        ExtractedEntities entities = ExtractedEntities.builder()
                .intent("BookMeeting")
                .attendees(Arrays.asList("myTeam"))
                .startDateTime(LocalDateTime.now().withHour(15).withMinute(0))
                .endDateTime(LocalDateTime.now().withHour(16).withMinute(0))
                .recurrencePattern("every weekday")
                .exceptions(Arrays.asList("every second Tuesday"))
                .subject("Team Sync")
                .build();

        when(luisService.extractIntentAndEntities(anyString())).thenReturn(entities);
        when(graphCalendarService.createEvent(any(ExtractedEntities.class), anyString()))
                .thenReturn("event-456");

        // Act
        BookingResponse response = bookingService.processBookingRequest(
                "Book myTeam 3 PM to 4 PM every weekday from Jan to Jul, except every second Tuesday", 
                "me");

        // Assert
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertNotNull(response.getRecurrencePattern());
        assertNotNull(response.getExceptions());
        verify(graphCalendarService, times(1)).createEvent(any(ExtractedEntities.class), anyString());
    }

    @Test
    void testProcessBookingRequest_CancelMeeting() {
        // Arrange
        ExtractedEntities entities = ExtractedEntities.builder()
                .intent("CancelMeeting")
                .attendees(Arrays.asList("Alex"))
                .startDateTime(LocalDateTime.of(2024, 1, 15, 10, 0))
                .subject("Meeting with Alex")
                .build();

        when(luisService.extractIntentAndEntities(anyString())).thenReturn(entities);
        when(graphCalendarService.findEvents(anyString(), any(), any(), anyString()))
                .thenReturn(Collections.singletonList(createMockEvent("event-789", "Meeting with Alex")));

        // Act
        BookingResponse response = bookingService.processBookingRequest(
                "Cancel my meeting with Alex on Jan 15 at 10 AM", "me");

        // Assert
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        verify(graphCalendarService, times(1)).deleteEvent(anyString(), anyString());
    }

    @Test
    void testProcessBookingRequest_RescheduleMeeting() {
        // Arrange
        ExtractedEntities entities = ExtractedEntities.builder()
                .intent("RescheduleMeeting")
                .attendees(Arrays.asList("Sarah"))
                .startDateTime(LocalDateTime.now().plusDays(7).withHour(15).withMinute(0))
                .endDateTime(LocalDateTime.now().plusDays(7).withHour(16).withMinute(0))
                .subject("Meeting with Sarah")
                .build();

        when(luisService.extractIntentAndEntities(anyString())).thenReturn(entities);
        when(graphCalendarService.findEvents(anyString(), any(), any(), anyString()))
                .thenReturn(Collections.singletonList(createMockEvent("event-999", "Meeting with Sarah")));

        // Act
        BookingResponse response = bookingService.processBookingRequest(
                "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday", "me");

        // Assert
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        verify(graphCalendarService, times(1)).updateEvent(anyString(), any(ExtractedEntities.class), anyString());
    }

    @Test
    void testProcessBookingRequest_UnknownIntent() {
        // Arrange
        ExtractedEntities entities = ExtractedEntities.builder()
                .intent("Unknown")
                .build();

        when(luisService.extractIntentAndEntities(anyString())).thenReturn(entities);

        // Act
        BookingResponse response = bookingService.processBookingRequest("Some random text", "me");

        // Assert
        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertTrue(response.getMessage().contains("Unknown intent"));
    }

    private com.microsoft.graph.models.Event createMockEvent(String id, String subject) {
        com.microsoft.graph.models.Event event = new com.microsoft.graph.models.Event();
        event.id = id;
        event.subject = subject;
        return event;
    }
}

