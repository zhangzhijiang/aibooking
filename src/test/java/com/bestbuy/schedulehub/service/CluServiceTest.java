package com.bestbuy.schedulehub.service;

import com.bestbuy.schedulehub.dto.ExtractedEntities;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CluServiceTest {

    private CluService cluService;
    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        cluService = new CluService();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();

        ReflectionTestUtils.setField(cluService, "cluEndpoint",
                "http://localhost:" + mockWebServer.getPort());
        ReflectionTestUtils.setField(cluService, "cluKey", "test-key");
        ReflectionTestUtils.setField(cluService, "cluProjectName", "test-project");
        ReflectionTestUtils.setField(cluService, "cluDeploymentName", "test-deployment");
        ReflectionTestUtils.setField(cluService, "cluApiVersion", "2022-05-01");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testExtractIntentAndEntities_SimpleSchedule() {
        String mockResponse = """
                {
                    "kind": "ConversationResult",
                    "result": {
                        "query": "Book Mary for 2 PM tomorrow",
                        "prediction": {
                            "topIntent": "BookMeeting",
                            "intents": {
                                "BookMeeting": { "score": 0.95 }
                            },
                            "entities": [
                                {
                                    "category": "PersonName",
                                    "text": "Mary",
                                    "offset": 5,
                                    "length": 4,
                                    "confidenceScore": 0.98
                                },
                                {
                                    "category": "DateTime",
                                    "text": "2 PM tomorrow",
                                    "offset": 13,
                                    "length": 14,
                                    "confidenceScore": 0.95,
                                    "extraInformation": {
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

        ExtractedEntities result = cluService.extractIntentAndEntities("Book Mary for 2 PM tomorrow");

        assertNotNull(result);
        assertEquals("BookMeeting", result.getIntent());
        assertTrue(result.getAttendees().contains("Mary"));
        assertNotNull(result.getStartDateTime());
    }

    @Test
    void testExtractIntentAndEntities_RecurringSchedule() {
        String mockResponse = """
                {
                    "kind": "ConversationResult",
                    "result": {
                        "query": "Book myTeam 3 PM to 4 PM every weekday",
                        "prediction": {
                            "topIntent": "BookMeeting",
                            "intents": {
                                "BookMeeting": { "score": 0.95 }
                            },
                            "entities": [
                                {
                                    "category": "PersonName",
                                    "text": "myTeam",
                                    "offset": 5,
                                    "length": 6,
                                    "confidenceScore": 0.98
                                },
                                {
                                    "category": "DateTime",
                                    "text": "3 PM to 4 PM",
                                    "offset": 12,
                                    "length": 12,
                                    "confidenceScore": 0.95,
                                    "extraInformation": {
                                        "values": [
                                            {
                                                "timex": "2024-01-15T15:00",
                                                "type": "datetime"
                                            }
                                        ]
                                    }
                                },
                                {
                                    "category": "Recurrence",
                                    "text": "every weekday",
                                    "offset": 25,
                                    "length": 13,
                                    "confidenceScore": 0.92
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

        ExtractedEntities result = cluService.extractIntentAndEntities(
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
                    "kind": "ConversationResult",
                    "result": {
                        "query": "Cancel my meeting with Alex on Jan 15 at 10 AM",
                        "prediction": {
                            "topIntent": "CancelMeeting",
                            "intents": {
                                "CancelMeeting": { "score": 0.95 }
                            },
                            "entities": [
                                {
                                    "category": "PersonName",
                                    "text": "Alex",
                                    "offset": 22,
                                    "length": 4,
                                    "confidenceScore": 0.98
                                },
                                {
                                    "category": "DateTime",
                                    "text": "Jan 15 at 10 AM",
                                    "offset": 31,
                                    "length": 15,
                                    "confidenceScore": 0.95,
                                    "extraInformation": {
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

        ExtractedEntities result = cluService.extractIntentAndEntities(
                "Cancel my meeting with Alex on Jan 15 at 10 AM");

        assertNotNull(result);
        assertEquals("CancelMeeting", result.getIntent());
        assertTrue(result.getAttendees().contains("Alex"));
    }
}
