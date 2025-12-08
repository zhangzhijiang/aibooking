package com.aibooking.service;

import com.aibooking.dto.ExtractedEntities;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LuisServiceTest {

    private LuisService luisService;
    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        luisService = new LuisService();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();

        ReflectionTestUtils.setField(luisService, "luisEndpoint", 
            "http://localhost:" + mockWebServer.getPort());
        ReflectionTestUtils.setField(luisService, "luisAppId", "test-app-id");
        ReflectionTestUtils.setField(luisService, "luisKey", "test-key");
        ReflectionTestUtils.setField(luisService, "luisRegion", "eastus");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testExtractIntentAndEntities_SimpleBooking() {
        String mockResponse = """
            {
                "query": "Book Mary for 2 PM tomorrow",
                "prediction": {
                    "topIntent": "BookMeeting",
                    "intents": {
                        "BookMeeting": { "score": 0.95 }
                    },
                    "entities": {
                        "personName": [
                            { "text": "Mary", "category": "personName" }
                        ],
                        "datetime": [
                            {
                                "text": "2 PM tomorrow",
                                "category": "datetime",
                                "resolution": {
                                    "values": [
                                        {
                                            "timex": "2024-01-16T14:00",
                                            "type": "datetime"
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        ExtractedEntities result = luisService.extractIntentAndEntities("Book Mary for 2 PM tomorrow");

        assertNotNull(result);
        assertEquals("BookMeeting", result.getIntent());
        assertTrue(result.getAttendees().contains("Mary"));
        assertNotNull(result.getStartDateTime());
    }

    @Test
    void testExtractIntentAndEntities_RecurringBooking() {
        String mockResponse = """
            {
                "query": "Book myTeam 3 PM to 4 PM every weekday",
                "prediction": {
                    "topIntent": "BookMeeting",
                    "intents": {
                        "BookMeeting": { "score": 0.95 }
                    },
                    "entities": {
                        "personName": [
                            { "text": "myTeam", "category": "personName" }
                        ],
                        "datetime": [
                            {
                                "text": "3 PM to 4 PM",
                                "category": "datetime",
                                "resolution": {
                                    "values": [
                                        {
                                            "timex": "2024-01-15T15:00",
                                            "type": "datetime"
                                        }
                                    ]
                                }
                            }
                        ],
                        "recurrence": [
                            { "text": "every weekday", "category": "recurrence" }
                        ]
                    }
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        ExtractedEntities result = luisService.extractIntentAndEntities(
            "Book myTeam 3 PM to 4 PM every weekday");

        assertNotNull(result);
        assertEquals("BookMeeting", result.getIntent());
        assertNotNull(result.getRecurrencePattern());
        assertTrue(result.getRecurrencePattern().contains("weekday"));
    }

    @Test
    void testExtractIntentAndEntities_CancelMeeting() {
        String mockResponse = """
            {
                "query": "Cancel my meeting with Alex on Jan 15 at 10 AM",
                "prediction": {
                    "topIntent": "CancelMeeting",
                    "intents": {
                        "CancelMeeting": { "score": 0.95 }
                    },
                    "entities": {
                        "personName": [
                            { "text": "Alex", "category": "personName" }
                        ],
                        "datetime": [
                            {
                                "text": "Jan 15 at 10 AM",
                                "category": "datetime",
                                "resolution": {
                                    "values": [
                                        {
                                            "timex": "2024-01-15T10:00",
                                            "type": "datetime"
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        ExtractedEntities result = luisService.extractIntentAndEntities(
            "Cancel my meeting with Alex on Jan 15 at 10 AM");

        assertNotNull(result);
        assertEquals("CancelMeeting", result.getIntent());
        assertTrue(result.getAttendees().contains("Alex"));
    }
}

