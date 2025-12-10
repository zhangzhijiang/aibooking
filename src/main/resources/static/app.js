// API Configuration
const API_URL = "http://localhost:8080/api";

function updateStatus(message, type = "info") {
  const statusDiv = document.getElementById("status");
  statusDiv.textContent = message;
  statusDiv.className = `status ${type}`;
}

// Process Schedule Request
async function processScheduleRequest(text) {
  try {
    // Get user ID from input field
    const userId = document.getElementById("userIdInput").value.trim();
    
    if (!userId) {
      updateStatus("Please enter a User ID (email or object ID)", "error");
      return;
    }

    const response = await fetch(`${API_URL}/scheduleMeeting`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-User-Id": userId,
      },
      body: JSON.stringify({ text: text }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    displayResponse(result);
  } catch (error) {
    console.error("Error processing schedule request:", error);
    updateStatus(
      "Failed to process schedule request: " + error.message,
      "error"
    );
    
    // Show debug sections even on error for troubleshooting
    displayOpenAIOutput(null);
    displayGraphAPIInput(null);
    displayBookingResults(null);
  }
}

function displayResponse(result) {
  const responseDiv = document.getElementById("response");

  if (result.status === "success") {
    updateStatus("Meeting scheduled successfully!", "success");

    let html = "<h3>✅ Schedule Confirmation</h3>";
    html += `<div class="response-item"><strong>Status:</strong> ${result.status}</div>`;
    html += `<div class="response-item"><strong>Message:</strong> ${result.message}</div>`;

    if (result.eventId) {
      html += `<div class="response-item"><strong>Event ID:</strong> ${result.eventId}</div>`;
    }

    if (result.eventSubject) {
      html += `<div class="response-item"><strong>Subject:</strong> ${result.eventSubject}</div>`;
    }

    if (result.startTime) {
      html += `<div class="response-item"><strong>Start Time:</strong> ${new Date(
        result.startTime
      ).toLocaleString()}</div>`;
    }

    if (result.endTime) {
      html += `<div class="response-item"><strong>End Time:</strong> ${new Date(
        result.endTime
      ).toLocaleString()}</div>`;
    }

    if (result.attendees && result.attendees.length > 0) {
      html += `<div class="response-item"><strong>Attendees:</strong> ${result.attendees.join(
        ", "
      )}</div>`;
    }

    if (result.recurrencePattern) {
      html += `<div class="response-item"><strong>Recurrence:</strong> ${result.recurrencePattern}</div>`;
    }

    if (result.exceptions && result.exceptions.length > 0) {
      html += `<div class="response-item"><strong>Exceptions:</strong> ${result.exceptions.join(
        ", "
      )}</div>`;
    }

    responseDiv.innerHTML = html;
    responseDiv.classList.remove("hidden");
  } else {
    updateStatus("Error: " + result.message, "error");
    responseDiv.innerHTML = `<h3>❌ Error</h3><div class="response-item">${result.message}</div>`;
    responseDiv.classList.remove("hidden");
  }

  // Always display debug sections for troubleshooting
  // Display OpenAI Output
  displayOpenAIOutput(result.openaiOutput);

  // Display Graph API Input
  displayGraphAPIInput(result.graphApiInput);

  // Display Booking Results
  displayBookingResults(result.bookingResults);
}

function displayOpenAIOutput(openaiOutput) {
  const section = document.getElementById("openaiOutput");
  const content = document.getElementById("openaiOutputContent");

  if (openaiOutput && Object.keys(openaiOutput).length > 0) {
    content.textContent = JSON.stringify(openaiOutput, null, 2);
    section.classList.remove("hidden");
  } else {
    content.textContent = "No OpenAI output data available";
    section.classList.remove("hidden"); // Always show for debugging
  }
}

function displayGraphAPIInput(graphApiInput) {
  const section = document.getElementById("graphApiInput");
  const content = document.getElementById("graphApiInputContent");

  if (graphApiInput && Object.keys(graphApiInput).length > 0) {
    content.textContent = JSON.stringify(graphApiInput, null, 2);
    section.classList.remove("hidden");
  } else {
    content.textContent = "No Graph API input data available";
    section.classList.remove("hidden"); // Always show for debugging
  }
}

function displayBookingResults(bookingResults) {
  const section = document.getElementById("bookingResults");
  const content = document.getElementById("bookingResultsContent");

  if (bookingResults && bookingResults.length > 0) {
    let html = "";
    bookingResults.forEach((booking, index) => {
      html += `<div class="booking-item">`;
      html += `<h4>Meeting ${index + 1}</h4>`;
      
      if (booking.eventId) {
        html += `<div class="booking-field"><strong>Event ID:</strong> ${booking.eventId}</div>`;
      }
      
      if (booking.subject) {
        html += `<div class="booking-field"><strong>Subject:</strong> ${booking.subject}</div>`;
      }
      
      if (booking.startTime) {
        html += `<div class="booking-field"><strong>Start Time:</strong> ${new Date(booking.startTime).toLocaleString()}</div>`;
      }
      
      if (booking.endTime) {
        html += `<div class="booking-field"><strong>End Time:</strong> ${new Date(booking.endTime).toLocaleString()}</div>`;
      }
      
      if (booking.attendees && booking.attendees.length > 0) {
        html += `<div class="booking-field"><strong>Attendees:</strong> ${booking.attendees.join(", ")}</div>`;
      }
      
      if (booking.location) {
        html += `<div class="booking-field"><strong>Location:</strong> ${booking.location}</div>`;
      }
      
      if (booking.recurrencePattern) {
        html += `<div class="booking-field"><strong>Recurrence:</strong> ${booking.recurrencePattern}</div>`;
      }
      
      if (booking.status) {
        html += `<div class="booking-field"><strong>Status:</strong> <span style="color: green;">${booking.status}</span></div>`;
      }
      
      html += `</div>`;
    });
    
    content.innerHTML = html;
    section.classList.remove("hidden");
  } else {
    // Always show section for debugging, even if empty
    content.innerHTML = `<p style="color: #999; text-align: center; padding: 20px;">No booking results available. Check the error message above or review the Graph API input section.</p>`;
    section.classList.remove("hidden");
  }
}

// Handle text input submission
function handleTextSubmit() {
  const textInput = document.getElementById("textInput");
  const text = textInput.value.trim();

  if (!text) {
    updateStatus("Please enter a request", "error");
    return;
  }

  // Update transcription display
  document.getElementById("transcription").textContent = text;
  updateStatus("Processing your request...", "info");

  // Process the request
  processScheduleRequest(text);

  // Clear the input
  textInput.value = "";
}

// Event Listeners
document.getElementById("submitButton").addEventListener("click", handleTextSubmit);

document.getElementById("textInput").addEventListener("keypress", (e) => {
  if (e.key === "Enter") {
    handleTextSubmit();
  }
});

