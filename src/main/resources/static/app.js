// Azure Speech SDK Configuration
let speechConfig = null;
let recognizer = null;
let isRecording = false;

// API Configuration
const API_URL =
  document.getElementById("apiUrl").value || "http://localhost:8080/api";

// Initialize Speech SDK
async function initializeSpeechSDK() {
  const speechKey = document.getElementById("speechKey").value;
  const speechRegion = document.getElementById("speechRegion").value;

  if (!speechKey || !speechRegion) {
    updateStatus(
      "Please configure Azure Speech Service key and region",
      "error"
    );
    return false;
  }

  try {
    const speechSdk = SpeechSDK;
    const audioConfig = speechSdk.AudioConfig.fromDefaultMicrophoneInput();
    speechConfig = speechSdk.SpeechConfig.fromSubscription(
      speechKey,
      speechRegion
    );
    speechConfig.speechRecognitionLanguage = "en-US";

    recognizer = new speechSdk.SpeechRecognizer(speechConfig, audioConfig);

    recognizer.recognizing = (s, e) => {
      if (e.result.text) {
        document.getElementById("transcription").textContent =
          e.result.text + "...";
      }
    };

    recognizer.recognized = async (s, e) => {
      if (e.result.reason === speechSdk.ResultReason.RecognizedSpeech) {
        const text = e.result.text;
        document.getElementById("transcription").textContent = text;
        updateStatus("Processing your request...", "info");

        // Send to backend API
        await processScheduleRequest(text);
      } else if (e.result.reason === speechSdk.ResultReason.NoMatch) {
        updateStatus("No speech recognized. Please try again.", "error");
      }
    };

    recognizer.canceled = (s, e) => {
      updateStatus(`Recognition canceled: ${e.reason}`, "error");
      if (e.reason === speechSdk.CancellationReason.Error) {
        updateStatus(`Error details: ${e.errorDetails}`, "error");
      }
      stopRecording();
    };

    recognizer.sessionStopped = (s, e) => {
      stopRecording();
    };

    return true;
  } catch (error) {
    console.error("Error initializing Speech SDK:", error);
    updateStatus(
      "Failed to initialize speech recognition. Please check your configuration.",
      "error"
    );
    return false;
  }
}

// Start/Stop Recording
async function toggleRecording() {
  if (!isRecording) {
    const initialized = await initializeSpeechSDK();
    if (!initialized) {
      return;
    }

    try {
      recognizer.startContinuousRecognitionAsync(
        () => {
          isRecording = true;
          updateMicButton(true);
          updateStatus("Listening... Speak now", "info");
        },
        (error) => {
          console.error("Error starting recognition:", error);
          updateStatus("Failed to start recording", "error");
        }
      );
    } catch (error) {
      console.error("Error starting recognition:", error);
      updateStatus("Failed to start recording", "error");
    }
  } else {
    stopRecording();
  }
}

function stopRecording() {
  if (recognizer) {
    recognizer.stopContinuousRecognitionAsync(
      () => {
        isRecording = false;
        updateMicButton(false);
        updateStatus("Recording stopped", "info");
      },
      (error) => {
        console.error("Error stopping recognition:", error);
      }
    );
  }
}

function updateMicButton(recording) {
  const button = document.getElementById("micButton");
  if (recording) {
    button.classList.add("recording");
    button.textContent = "⏹️";
  } else {
    button.classList.remove("recording");
    button.textContent = "🎤";
  }
}

function updateStatus(message, type = "info") {
  const statusDiv = document.getElementById("status");
  statusDiv.textContent = message;
  statusDiv.className = `status ${type}`;
}

// Process Schedule Request
async function processScheduleRequest(text) {
  try {
    const response = await fetch(`${API_URL}/scheduleMeeting`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
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
}

// Event Listeners
document.getElementById("micButton").addEventListener("click", toggleRecording);

// Update API URL when changed
document.getElementById("apiUrl").addEventListener("change", (e) => {
  API_URL = e.target.value;
});

// Handle page unload
window.addEventListener("beforeunload", () => {
  if (recognizer) {
    recognizer.close();
  }
});
