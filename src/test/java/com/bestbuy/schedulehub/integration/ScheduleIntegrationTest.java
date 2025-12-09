package com.bestbuy.schedulehub.integration;

import com.bestbuy.schedulehub.dto.ScheduleRequest;
import com.bestbuy.schedulehub.dto.ScheduleResponse;
import com.bestbuy.schedulehub.service.GraphCalendarService;
import com.bestbuy.schedulehub.service.CluService;
import com.bestbuy.schedulehub.dto.ExtractedEntities;
import com.microsoft.graph.models.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScheduleIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @MockBean
        private CluService cluService;

        @MockBean
        private GraphCalendarService graphCalendarService;

        private String baseUrl;

        @BeforeEach
        void setUp() {
                baseUrl = "http://localhost:" + port + "/api";
        }

        @Test
        void testSimpleSchedule_Integration() {
                // Arrange
                ExtractedEntities entities = ExtractedEntities.builder()
                                .intent("BookMeeting")
                                .attendees(Arrays.asList("Mary"))
                                .startDateTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0))
                                .endDateTime(LocalDateTime.now().plusDays(1).withHour(15).withMinute(0))
                                .subject("Meeting with Mary")
                                .build();

                when(cluService.extractIntentAndEntities("Book Mary for 2 PM tomorrow"))
                                .thenReturn(entities);
                when(graphCalendarService.createEvent(any(ExtractedEntities.class), anyString()))
                                .thenReturn("event-123");

                ScheduleRequest request = new ScheduleRequest();
                request.setText("Book Mary for 2 PM tomorrow");

                // Act
                ResponseEntity<ScheduleResponse> response = restTemplate.postForEntity(
                                baseUrl + "/scheduleMeeting", request, ScheduleResponse.class);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("success", response.getBody().getStatus());
                assertEquals("event-123", response.getBody().getEventId());
                assertTrue(response.getBody().getAttendees().contains("Mary"));
        }

        @Test
        void testRecurringScheduleWithException_Integration() {
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

                when(cluService.extractIntentAndEntities(
                                "Book myTeam 3 PM to 4 PM every weekday from Jan to Jul, except every second Tuesday"))
                                .thenReturn(entities);
                when(graphCalendarService.createEvent(any(ExtractedEntities.class), anyString()))
                                .thenReturn("event-456");

                ScheduleRequest request = new ScheduleRequest();
                request.setText("Book myTeam 3 PM to 4 PM every weekday from Jan to Jul, except every second Tuesday");

                // Act
                ResponseEntity<ScheduleResponse> response = restTemplate.postForEntity(
                                baseUrl + "/scheduleMeeting", request, ScheduleResponse.class);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("success", response.getBody().getStatus());
                assertNotNull(response.getBody().getRecurrencePattern());
                assertNotNull(response.getBody().getExceptions());
        }

        @Test
        void testMultipleExclusions_Integration() {
                // Arrange
                ExtractedEntities entities = ExtractedEntities.builder()
                                .intent("BookMeeting")
                                .startDateTime(LocalDateTime.now().withHour(9).withMinute(0))
                                .endDateTime(LocalDateTime.now().withHour(10).withMinute(0))
                                .recurrencePattern("every weekday")
                                .exceptions(Arrays.asList("Monday in the first week",
                                                "Tuesday/Thursday in the second week"))
                                .subject("Team sync")
                                .build();

                when(cluService.extractIntentAndEntities(
                                "Schedule team sync 9 AM every weekday, but skip Monday in the first week and skip Tuesday/Thursday in the second week"))
                                .thenReturn(entities);
                when(graphCalendarService.createEvent(any(ExtractedEntities.class), anyString()))
                                .thenReturn("event-789");

                ScheduleRequest request = new ScheduleRequest();
                request.setText("Schedule team sync 9 AM every weekday, but skip Monday in the first week and skip Tuesday/Thursday in the second week");

                // Act
                ResponseEntity<ScheduleResponse> response = restTemplate.postForEntity(
                                baseUrl + "/scheduleMeeting", request, ScheduleResponse.class);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("success", response.getBody().getStatus());
                assertNotNull(response.getBody().getExceptions());
                assertTrue(response.getBody().getExceptions().size() >= 2);
        }

        @Test
        void testCancelMeeting_Integration() {
                // Arrange
                ExtractedEntities entities = ExtractedEntities.builder()
                                .intent("CancelMeeting")
                                .attendees(Arrays.asList("Alex"))
                                .startDateTime(LocalDateTime.of(2024, 1, 15, 10, 0))
                                .subject("Meeting with Alex")
                                .build();

                Event mockEvent = new Event();
                mockEvent.id = "event-999";
                mockEvent.subject = "Meeting with Alex";

                when(cluService.extractIntentAndEntities("Cancel my meeting with Alex on Jan 15 at 10 AM"))
                                .thenReturn(entities);
                when(graphCalendarService.findEvents(anyString(), any(), any(), anyString()))
                                .thenReturn(Collections.singletonList(mockEvent));

                ScheduleRequest request = new ScheduleRequest();
                request.setText("Cancel my meeting with Alex on Jan 15 at 10 AM");

                // Act
                ResponseEntity<ScheduleResponse> response = restTemplate.postForEntity(
                                baseUrl + "/scheduleMeeting", request, ScheduleResponse.class);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("success", response.getBody().getStatus());
        }

        @Test
        void testRescheduleMeeting_Integration() {
                // Arrange
                ExtractedEntities entities = ExtractedEntities.builder()
                                .intent("RescheduleMeeting")
                                .attendees(Arrays.asList("Sarah"))
                                .startDateTime(LocalDateTime.now().plusDays(7).withHour(15).withMinute(0))
                                .endDateTime(LocalDateTime.now().plusDays(7).withHour(16).withMinute(0))
                                .subject("Meeting with Sarah")
                                .build();

                Event mockEvent = new Event();
                mockEvent.id = "event-111";
                mockEvent.subject = "Meeting with Sarah";

                when(cluService.extractIntentAndEntities(
                                "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"))
                                .thenReturn(entities);
                when(graphCalendarService.findEvents(anyString(), any(), any(), anyString()))
                                .thenReturn(Collections.singletonList(mockEvent));

                ScheduleRequest request = new ScheduleRequest();
                request.setText("Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday");

                // Act
                ResponseEntity<ScheduleResponse> response = restTemplate.postForEntity(
                                baseUrl + "/scheduleMeeting", request, ScheduleResponse.class);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals("success", response.getBody().getStatus());
        }

        @Test
        void testHealthEndpoint() {
                // Act
                ResponseEntity<String> response = restTemplate.getForEntity(
                                baseUrl + "/health", String.class);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Service is running", response.getBody());
        }
}

