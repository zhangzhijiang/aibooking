package com.aibooking.controller;

import com.aibooking.dto.BookingRequest;
import com.aibooking.dto.BookingResponse;
import com.aibooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testBookMeeting() throws Exception {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setText("Book Mary for 2 PM tomorrow");

        BookingResponse response = BookingResponse.builder()
                .status("success")
                .message("Meeting booked successfully")
                .eventId("event-123")
                .eventSubject("Meeting with Mary")
                .startTime(LocalDateTime.now().plusDays(1).withHour(14))
                .endTime(LocalDateTime.now().plusDays(1).withHour(15))
                .attendees(Arrays.asList("Mary"))
                .build();

        when(bookingService.processBookingRequest(anyString(), anyString()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/bookMeeting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.eventId").value("event-123"))
                .andExpect(jsonPath("$.attendees[0]").value("Mary"));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Service is running"));
    }
}

