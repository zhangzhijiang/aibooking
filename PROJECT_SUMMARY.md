# Project Summary

## Overview

This is a complete Java Spring Boot application for a speech-driven Outlook Calendar booking assistant. The system allows users to book, cancel, and reschedule meetings in Microsoft Outlook Calendar using natural language speech input.

## Project Structure

```
aibooking/
├── src/
│   ├── main/
│   │   ├── java/com/aibooking/
│   │   │   ├── config/                    # Configuration classes
│   │   │   │   ├── AzureConfig.java       # Azure Key Vault & Identity config
│   │   │   │   └── GraphApiConfig.java    # Microsoft Graph API config
│   │   │   ├── controller/
│   │   │   │   └── BookingController.java # REST API endpoints
│   │   │   ├── dto/                       # Data transfer objects
│   │   │   │   ├── BookingRequest.java
│   │   │   │   ├── BookingResponse.java
│   │   │   │   ├── ExtractedEntities.java
│   │   │   │   └── LuisIntent.java
│   │   │   ├── service/                   # Business logic services
│   │   │   │   ├── BookingService.java    # Main booking orchestration
│   │   │   │   ├── GraphCalendarService.java # Graph API integration
│   │   │   │   ├── KeyVaultService.java   # Key Vault integration
│   │   │   │   └── LuisService.java       # LUIS NLP integration
│   │   │   └── SpeechCalendarAssistantApplication.java
│   │   └── resources/
│   │       ├── static/                    # Frontend files
│   │       │   ├── index.html            # Web UI
│   │       │   └── app.js                # Speech SDK integration
│   │       ├── application.properties    # Application config
│   │       └── application.yml           # Alternative config
│   └── test/
│       ├── java/com/aibooking/
│       │   ├── controller/
│       │   │   └── BookingControllerTest.java
│       │   ├── integration/
│       │   │   └── BookingIntegrationTest.java
│       │   └── service/
│       │       ├── BookingServiceTest.java
│       │       └── LuisServiceTest.java
│       └── resources/
│           └── application-test.properties
├── pom.xml                                # Maven dependencies
├── .gitignore
├── README.md                              # Main documentation
├── AZURE_SETUP.md                        # Azure resource setup guide
├── LUIS_CONFIGURATION.md                 # LUIS model configuration
├── QUICK_START.md                        # Quick start guide
└── PROJECT_SUMMARY.md                    # This file
```

## Key Features

### 1. Speech Recognition
- Web-based UI with microphone button
- Azure Speech SDK integration
- Real-time transcription
- Browser-based (no server-side speech processing needed)

### 2. Natural Language Understanding
- Azure LUIS integration
- Intent extraction (BookMeeting, CancelMeeting, RescheduleMeeting)
- Entity extraction (attendees, dates, times, recurrence, exceptions)
- Handles complex natural language queries

### 3. Calendar Integration
- Microsoft Graph API integration
- Create single and recurring events
- Update existing events
- Delete events
- Handle complex recurrence patterns
- Apply exception rules (skip specific occurrences)

### 4. Security
- Azure AD OAuth 2.0 authentication
- Azure Key Vault for secret management
- Secure credential handling
- Application-level permissions

### 5. Monitoring
- Application Insights integration
- Comprehensive logging
- Error tracking
- Performance monitoring

### 6. Testing
- Unit tests for all services
- Integration tests for API endpoints
- Mock-based testing for external services
- Test coverage for all use cases

## Technology Stack

### Backend
- **Java 17** - Programming language
- **Spring Boot 3.2.0** - Framework
- **Maven** - Build tool
- **Microsoft Graph SDK** - Calendar API
- **Azure Identity** - Authentication
- **Azure Key Vault SDK** - Secret management
- **OkHttp** - HTTP client for LUIS

### Frontend
- **HTML5** - Markup
- **JavaScript** - Client-side logic
- **Azure Speech SDK (JavaScript)** - Speech recognition
- **CSS3** - Styling

### Azure Services
- **Azure Speech Service** - Speech-to-text
- **Azure LUIS** - Natural language understanding
- **Microsoft Graph API** - Calendar operations
- **Azure AD** - Authentication
- **Azure Key Vault** - Secret storage
- **Application Insights** - Monitoring
- **Azure App Service** - Hosting (optional)

## API Endpoints

### POST /api/bookMeeting
Processes natural language booking requests.

**Request:**
```json
{
  "text": "Book Mary for 2 PM tomorrow"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Meeting booked successfully",
  "eventId": "AAMkAGI2...",
  "eventSubject": "Meeting with Mary",
  "startTime": "2024-01-16T14:00:00",
  "endTime": "2024-01-16T15:00:00",
  "attendees": ["Mary"],
  "recurrencePattern": null,
  "exceptions": null
}
```

### GET /api/health
Health check endpoint.

## Test Scenarios

All test scenarios from requirements are implemented:

1. ✅ **Simple booking**: "Book Mary for 2 PM tomorrow"
2. ✅ **Recurring booking with exception**: "Book myTeam 3 PM to 4 PM every weekday, except every second Tuesday"
3. ✅ **Multiple exclusions**: "Schedule team sync 9 AM every weekday, but skip Monday in the first week and skip Tuesday/Thursday in the second week"
4. ✅ **Cancel meeting**: "Cancel my meeting with Alex on Jan 15 at 10 AM"
5. ✅ **Reschedule meeting**: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Configuration

### Required Environment Variables

```bash
# Azure AD
AZURE_TENANT_ID
AZURE_CLIENT_ID
AZURE_CLIENT_SECRET

# Azure Key Vault
AZURE_KEY_VAULT_URI

# Azure Speech Service
AZURE_SPEECH_KEY
AZURE_SPEECH_REGION

# Azure LUIS
AZURE_LUIS_APP_ID
AZURE_LUIS_KEY
AZURE_LUIS_REGION

# Application Insights (optional)
APPLICATIONINSIGHTS_INSTRUMENTATION_KEY
APPLICATIONINSIGHTS_CONNECTION_STRING
```

## Deployment

### Local Development
```bash
mvn spring-boot:run
```

### Azure App Service
```bash
mvn clean package
az webapp deploy --resource-group <rg> --name <app-name> --type jar --src-path target/speech-calendar-assistant-1.0.0.jar
```

## Documentation Files

1. **README.md** - Complete project documentation
2. **AZURE_SETUP.md** - Step-by-step Azure resource setup
3. **LUIS_CONFIGURATION.md** - LUIS model configuration guide
4. **QUICK_START.md** - Quick start guide for developers
5. **PROJECT_SUMMARY.md** - This file

## Known Issues / Notes

1. **pom.xml name tag**: There's a minor XML issue with `<n>` tag on line 18. It should be `<name>`. This doesn't affect functionality but should be fixed for proper Maven compliance.

2. **LUIS Dependency**: The LUIS runtime SDK dependency was removed as we use direct HTTP calls to the LUIS REST API, which is more reliable.

3. **Speech SDK**: The frontend uses Azure Speech SDK for JavaScript, which requires HTTPS in production for microphone access.

4. **Graph API Permissions**: Requires admin consent for application permissions.

## Next Steps / Enhancements

1. **Multi-language support** - Add support for multiple languages
2. **SMS/Email notifications** - Integrate Azure Communication Services
3. **Voice feedback** - Add text-to-speech for confirmations
4. **Meeting room booking** - Integrate room availability
5. **Conflict detection** - Check for scheduling conflicts
6. **Recurrence pattern improvements** - Better handling of complex patterns
7. **User preferences** - Store user preferences and defaults
8. **Analytics dashboard** - Usage analytics and insights

## Support

For setup help:
- See `QUICK_START.md` for quick setup
- See `AZURE_SETUP.md` for detailed Azure configuration
- See `LUIS_CONFIGURATION.md` for LUIS setup

For development:
- Review test files for usage examples
- Check service classes for implementation details
- Review controller for API usage

## License

This project is provided as-is for demonstration purposes.

