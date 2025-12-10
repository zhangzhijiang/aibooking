# Schedule Hub - Architecture & Design Documentation

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Component Design](#component-design)
4. [Data Flow](#data-flow)
5. [API Design](#api-design)
6. [Security Architecture](#security-architecture)
7. [Configuration Management](#configuration-management)
8. [Error Handling](#error-handling)
9. [Logging & Monitoring](#logging--monitoring)
10. [Testing Strategy](#testing-strategy)

## Overview

Schedule Hub is a Spring Boot application that enables natural language scheduling of Microsoft Outlook Calendar events. The system uses OpenAI for intent and entity extraction, and Microsoft Graph API for calendar operations.

### Key Design Principles

- **Simplicity**: Minimal dependencies, straightforward configuration
- **Reliability**: Comprehensive error handling and logging
- **Extensibility**: Service-based architecture for easy modifications
- **Security**: Azure AD authentication with secure credential management

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        User Browser                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  index.html (UI) + app.js (Client Logic)            │  │
│  └───────────────────────┬─────────────────────────────┘  │
└──────────────────────────┼─────────────────────────────────┘
                           │ HTTP POST /api/scheduleMeeting
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Application (Port 8080)           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  ScheduleController                                  │  │
│  │  - POST /api/scheduleMeeting                         │  │
│  │  - GET /api/health                                   │  │
│  └───────────────────────┬─────────────────────────────┘  │
│                           │                                 │
│  ┌───────────────────────▼─────────────────────────────┐  │
│  │  ScheduleService (Orchestrator)                      │  │
│  │  - Intent routing                                    │  │
│  │  - Business logic coordination                       │  │
│  └───────┬───────────────────────────────┬─────────────┘  │
│           │                               │                 │
│  ┌────────▼────────┐            ┌────────▼────────┐        │
│  │ OpenAIService   │            │GraphCalendar    │        │
│  │ - NLP/Intent    │            │Service          │        │
│  │   Extraction    │            │ - Create Events │        │
│  └────────┬────────┘            │ - Update Events │        │
│           │                     │ - Delete Events │        │
│           │                     └────────┬─────────┘        │
│           │                              │                 │
└───────────┼──────────────────────────────┼─────────────────┘
            │                              │
            ▼                              ▼
┌──────────────────────┐      ┌──────────────────────────┐
│   OpenAI API         │      │  Microsoft Graph API     │
│   (GPT Models)       │      │  (Calendar Operations)   │
└──────────────────────┘      └──────────────────────────┘
```

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Build Tool**: Maven 3.6+
- **HTTP Client**: OkHttp (for OpenAI API)
- **JSON Processing**: Jackson
- **Microsoft Graph SDK**: For calendar operations
- **Azure Identity**: For authentication

#### Frontend
- **HTML5**: Structure
- **CSS3**: Styling (inline)
- **JavaScript (ES6+)**: Client-side logic
- **Fetch API**: HTTP requests

#### External Services
- **OpenAI API**: Natural language understanding
- **Microsoft Graph API**: Calendar operations
- **Azure AD**: Authentication
- **Application Insights**: Monitoring (optional)

## Component Design

### 1. ScheduleController

**Purpose**: REST API endpoint handler

**Responsibilities**:
- Receive HTTP requests from frontend
- Validate request payload
- Delegate to ScheduleService
- Return HTTP responses

**Key Methods**:
- `POST /api/scheduleMeeting`: Process scheduling requests
- `GET /api/health`: Health check

**Request/Response**:
```java
// Request
{
  "text": "Book Mary for 2 PM tomorrow"
}

// Response
{
  "status": "success",
  "message": "Meeting scheduled successfully",
  "eventId": "...",
  "eventSubject": "...",
  "startTime": "...",
  "endTime": "...",
  "attendees": [...],
  "recurrencePattern": null,
  "exceptions": null
}
```

### 2. ScheduleService

**Purpose**: Main orchestration service

**Responsibilities**:
- Coordinate between OpenAIService and GraphCalendarService
- Route requests based on extracted intent
- Handle business logic for different operations
- Format responses

**Key Methods**:
- `processScheduleRequest(String text, String userId)`: Main entry point
- `handleBookMeeting(...)`: Create new events
- `handleCancelMeeting(...)`: Delete existing events
- `handleRescheduleMeeting(...)`: Update existing events

**Intent Routing**:
- `BookMeeting` → Create new calendar event
- `CancelMeeting` → Find and delete event
- `RescheduleMeeting` → Find and update event

### 3. OpenAIService

**Purpose**: Natural language understanding

**Responsibilities**:
- Extract intent from user text
- Extract entities (attendees, dates, times, etc.)
- Parse OpenAI API responses
- Handle date/time normalization

**Key Methods**:
- `extractIntentAndEntities(String text)`: Main extraction method
- `parseOpenAIResponse(Map<String, Object>)`: Parse JSON response

**System Prompt**:
The service uses a structured prompt to guide OpenAI:
- Intent classification (BookMeeting, CancelMeeting, RescheduleMeeting)
- Entity extraction (attendees, dates, times, subject, location, recurrence)
- Date/time resolution (relative dates like "tomorrow", "next Friday")
- Default values (1-hour duration if not specified)

**Response Format**:
```json
{
  "intent": "BookMeeting",
  "attendees": ["John"],
  "startDateTime": "2025-12-10T14:00",
  "endDateTime": "2025-12-10T15:00",
  "subject": "Meeting with John",
  "location": null,
  "recurrencePattern": null,
  "exceptions": null
}
```

### 4. GraphCalendarService

**Purpose**: Microsoft Graph API integration

**Responsibilities**:
- Create calendar events
- Update existing events
- Delete events
- Search for events
- Handle recurring events and exceptions

**Key Methods**:
- `createEvent(ExtractedEntities, String userId)`: Create new event
- `updateEvent(String eventId, ExtractedEntities, String userId)`: Update event
- `deleteEvent(String eventId, String userId)`: Delete event
- `findEvents(...)`: Search for events

**Event Creation**:
- Maps ExtractedEntities to Microsoft Graph Event model
- Handles attendees (email addresses)
- Supports recurrence patterns
- Applies exception rules

### 5. Configuration Classes

#### AzureConfig
- Creates `TokenCredential` bean for Azure AD authentication
- Uses client secret credentials

#### GraphApiConfig
- Configures `GraphServiceClient` for Microsoft Graph API
- Sets up authentication and endpoint

## Data Flow

### Booking a Meeting Flow

```
1. User enters text: "Book Mary for 2 PM tomorrow"
   ↓
2. Frontend sends POST /api/scheduleMeeting
   ↓
3. ScheduleController receives request
   ↓
4. ScheduleService.processScheduleRequest()
   ↓
5. OpenAIService.extractIntentAndEntities()
   - Calls OpenAI API with structured prompt
   - Receives JSON response with intent and entities
   - Parses response into ExtractedEntities object
   ↓
6. ScheduleService routes based on intent (BookMeeting)
   ↓
7. GraphCalendarService.createEvent()
   - Maps ExtractedEntities to Graph Event model
   - Calls Microsoft Graph API
   - Returns event ID
   ↓
8. ScheduleService formats response
   ↓
9. ScheduleController returns HTTP 200 with response
   ↓
10. Frontend displays confirmation
```

### Canceling a Meeting Flow

```
1. User enters text: "Cancel my meeting with Alex on Jan 15 at 10 AM"
   ↓
2. OpenAIService extracts intent: CancelMeeting
   - Extracts: attendees=["Alex"], startDateTime="2024-01-15T10:00"
   ↓
3. ScheduleService.handleCancelMeeting()
   ↓
4. GraphCalendarService.findEvents()
   - Searches for events matching criteria
   - Returns list of matching events
   ↓
5. GraphCalendarService.deleteEvent()
   - Deletes the found event
   ↓
6. Response returned to user
```

## API Design

### REST Endpoints

#### POST /api/scheduleMeeting

**Description**: Process natural language scheduling request

**Request Headers**:
- `Content-Type: application/json`
- `X-User-Id: <user-id>` (REQUIRED - user email or object ID)

**Request Body**:
```json
{
  "text": "Book Mary for 2 PM tomorrow"
}
```

**Response (Success)**:
```json
{
  "status": "success",
  "message": "Meeting scheduled successfully",
  "eventId": "AAMkAGI2...",
  "eventSubject": "Meeting with Mary",
  "startTime": "2025-12-10T14:00:00",
  "endTime": "2025-12-10T15:00:00",
  "attendees": ["Mary"],
  "recurrencePattern": null,
  "exceptions": null
}
```

**Response (Error)**:
```json
{
  "status": "error",
  "message": "Error description"
}
```

**Status Codes**:
- `200 OK`: Request processed (success or error)
- `400 Bad Request`: Invalid request format
- `500 Internal Server Error`: Server error

#### GET /api/health

**Description**: Health check endpoint

**Response**:
```
Service is running
```

**Status Code**: `200 OK`

## Security Architecture

### Authentication

**Azure AD OAuth 2.0 Client Credentials Flow**:
1. Application authenticates using client ID and secret
2. Azure AD returns access token
3. Token used for Microsoft Graph API calls

**Configuration**:
- Tenant ID: Azure AD directory
- Client ID: Application registration ID
- Client Secret: Application secret

### Credential Management

**Current Approach**: Configuration in `application.yml`
- Simple for development and testing
- All credentials in one place
- No external dependencies

**Production Recommendations**:
- Use environment variables
- Consider Azure Key Vault for production
- Rotate secrets regularly
- Never commit secrets to version control

### API Security

- **OpenAI API**: Secured with API key (Bearer token)
- **Microsoft Graph API**: Secured with OAuth 2.0 access token
- **No user authentication required**: Application-level permissions

## Configuration Management

### application.yml Structure

```yaml
server:
  port: 8080

spring:
  application:
    name: schedule-hub

azure:
  activedirectory:
    tenant-id: <tenant-id>
    client-id: <client-id>
    client-secret: <client-secret>
    authority: https://login.microsoftonline.com/<tenant-id>
  application-insights:
    instrumentation-key: <optional>
    connection-string: <optional>

openai:
  api-key: <api-key>
  model: gpt-4o-mini

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
    endpoint: https://graph.microsoft.com/v1.0

logging:
  level:
    com.bestbuy.schedulehub: INFO
```

### Configuration Properties

| Property | Required | Description |
|----------|----------|-------------|
| `azure.activedirectory.tenant-id` | Yes | Azure AD tenant ID |
| `azure.activedirectory.client-id` | Yes | Azure AD app registration client ID |
| `azure.activedirectory.client-secret` | Yes | Azure AD client secret |
| `openai.api-key` | Yes | OpenAI API key |
| `openai.model` | No | OpenAI model (default: gpt-4o-mini) |
| `azure.application-insights.*` | No | Application Insights configuration |

### Environment Variables

All configuration can be overridden with environment variables:
- `AZURE_TENANT_ID`
- `AZURE_CLIENT_ID`
- `AZURE_CLIENT_SECRET`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`

## Error Handling

### Error Categories

1. **OpenAI API Errors**
   - 401 Unauthorized: Invalid API key
   - Rate limit errors
   - Network errors

2. **Graph API Errors**
   - 401 Unauthorized: Invalid credentials
   - 403 Forbidden: Missing permissions
   - 404 Not Found: Event not found

3. **Application Errors**
   - Invalid intent extraction
   - Missing required entities
   - Date/time parsing errors

### Error Response Format

```json
{
  "status": "error",
  "message": "Human-readable error message"
}
```

### Logging

All errors are logged with:
- Error level (ERROR, WARN, INFO)
- Context information
- Stack traces for exceptions
- Request/response details

## Logging & Monitoring

### Logging Levels

- **INFO**: Normal operations, request processing
- **DEBUG**: Detailed flow information
- **WARN**: Recoverable issues
- **ERROR**: Failures requiring attention

### Log Format

```
[timestamp] [level] [thread] [class] : [message]
```

### Key Log Points

1. **Request Received**: Controller entry
2. **Intent Extraction**: OpenAI service calls
3. **Entity Details**: Extracted entities
4. **Graph API Calls**: Calendar operations
5. **Success/Error**: Final outcomes

### Application Insights Integration

Optional integration for:
- Request telemetry
- Performance metrics
- Error tracking
- Dependency tracking

## Testing Strategy

### Test Types

1. **Unit Tests**
   - Service-level testing
   - Mock external dependencies
   - Test business logic

2. **Integration Tests**
   - Full request/response cycle
   - Mock external APIs
   - Test end-to-end flows

3. **Test Coverage**
   - All services tested
   - All intents covered
   - Error scenarios included

### Test Scenarios

1. Simple booking: "Book Mary for 2 PM tomorrow"
2. Recurring events: "Book myTeam 3 PM every weekday"
3. Cancellation: "Cancel my meeting with Alex"
4. Rescheduling: "Reschedule meeting from 2 PM to 3 PM"
5. Error handling: Invalid requests, API failures

## Performance Considerations

### OpenAI API

- **Latency**: ~200-500ms per request
- **Cost**: ~$0.0001 per request (gpt-4o-mini)
- **Rate Limits**: Based on subscription tier

### Microsoft Graph API

- **Latency**: ~100-300ms per request
- **Rate Limits**: 10,000 requests per 10 minutes
- **Caching**: Not currently implemented

### Optimization Opportunities

1. **Caching**: Cache OpenAI responses for similar requests
2. **Batch Operations**: Batch multiple calendar operations
3. **Async Processing**: Process requests asynchronously
4. **Connection Pooling**: Reuse HTTP connections

## Future Enhancements

1. **Multi-language Support**: Support multiple languages
2. **Voice Input**: Add Azure Speech SDK for voice input
3. **Conflict Detection**: Check for scheduling conflicts
4. **Meeting Room Booking**: Integrate room availability
5. **Notifications**: Email/SMS notifications
6. **User Preferences**: Store user defaults
7. **Key Vault Integration**: Secure credential management
8. **Caching Layer**: Redis for response caching

## Deployment Architecture

### Local Development
- Spring Boot embedded Tomcat
- Port 8080
- In-memory configuration

### Production (Recommended)
- Azure App Service
- Environment variables for configuration
- Application Insights enabled
- HTTPS required
- Auto-scaling configured

## Conclusion

Schedule Hub is designed with simplicity, reliability, and extensibility in mind. The service-based architecture allows for easy modifications and additions. The use of OpenAI for NLP eliminates the need for training and deployment of custom models, making the system easier to maintain and improve.

