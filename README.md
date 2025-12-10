# Schedule Hub - Outlook Calendar Assistant

A Java Spring Boot application that enables users to schedule meetings in Microsoft Outlook Calendar using natural language text input. Schedule Hub provides a simple web interface for managing calendar events through natural language processing powered by OpenAI.

## Features

- 📝 **Text Input**: Simple web-based UI for entering scheduling requests
- 🤖 **AI-Powered NLP**: OpenAI GPT for intent and entity extraction
- 📅 **Calendar Integration**: Microsoft Graph API for creating, updating, and deleting calendar events
- 🔄 **Recurring Events**: Support for complex recurrence patterns with exception handling
- 🔐 **Security**: Azure AD OAuth 2.0 authentication
- 📊 **Monitoring**: Azure Application Insights integration (optional)
- ✅ **Automated Tests**: Comprehensive unit and integration tests

## Architecture

```
┌─────────────┐
│   Browser   │
│  (HTML/JS)  │
└──────┬──────┘
       │ HTTP POST
       ▼
┌─────────────────┐
│  Spring Boot    │
│  REST API       │
└──────┬──────────┘
       │
       ├──► OpenAI API (NLP/Intent Extraction)
       ├──► Microsoft Graph API (Calendar)
       └──► Application Insights (Monitoring - Optional)
```

For detailed architecture and design documentation, see [DESIGN.md](DESIGN.md).

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Azure subscription (for Azure AD and Microsoft Graph)
- OpenAI API key ([Get one here](https://platform.openai.com/api-keys))
- Microsoft 365 account with Outlook Calendar access

## Quick Start

### 1. Configure application.yml

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: schedule-hub

azure:
  activedirectory:
    tenant-id: your-tenant-id
    client-id: your-client-id
    client-secret: your-client-secret
    authority: https://login.microsoftonline.com/your-tenant-id
  application-insights:
    instrumentation-key: # Optional
    connection-string: # Optional

# OpenAI Configuration
openai:
  api-key: your-openai-api-key-here # Get from https://platform.openai.com/api-keys
  model: gpt-4o-mini # Options: gpt-4o-mini (cheapest), gpt-4o, gpt-3.5-turbo

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
    endpoint: https://graph.microsoft.com/v1.0

logging:
  level:
    com.bestbuy.schedulehub: INFO
```

### 2. Azure AD App Registration (Application Permissions)

This application uses **Application Permissions** (service-driven architecture). It acts as a service principal and can manage calendars for all users in your tenant.

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Click **New registration**
4. Configure:
   - **Name**: `ScheduleHub`
   - **Supported account types**: Accounts in this organizational directory only
5. Click **Register**
6. Note the **Application (client) ID** and **Directory (tenant) ID**
7. Go to **Certificates & secrets** > **New client secret**
8. Create a secret and note the **Value** (copy immediately - it won't be shown again)
9. Go to **API permissions** > **Add a permission** > **Microsoft Graph**
10. Choose **Application permissions** (NOT Delegated)
11. Add the following permissions:
    - `Calendars.ReadWrite` - Read and write calendars for all users
    - `User.Read.All` - Read all users' profiles
12. Click **Add permissions**
13. **IMPORTANT**: Click **Grant admin consent for [your organization]**
14. Verify both permissions show "Granted for [your org]" with admin consent

**Note:** When making API requests, include the `X-User-Id` header with the user's email or object ID to specify which user's calendar to manage.

### 3. Get OpenAI API Key

1. Go to [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Click **Create new secret key**
4. Copy the API key (starts with `sk-`)
5. Add it to `application.yml` under `openai.api-key`

### 4. Build and Run

```cmd
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 5. Access the Web UI

Open your browser and navigate to `http://localhost:8080`

1. Type your scheduling request (e.g., "Book Mary for 2 PM tomorrow")
2. Click Submit or press Enter
3. View the booking confirmation

## API Endpoints

### POST /api/scheduleMeeting

Schedule, cancel, or reschedule a meeting based on natural language input.

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

### GET /api/health

Health check endpoint.

## Example Requests

### Book a Meeting

```
"Book Mary for 2 PM tomorrow"
"Schedule a meeting with John at 3 PM next Friday"
"Create an appointment with Sarah on January 20 at 10 AM"
```

### Cancel a Meeting

```
"Cancel my meeting with Alex on Jan 15 at 10 AM"
"Delete the appointment with Sarah tomorrow"
```

### Reschedule a Meeting

```
"Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"
"Move the appointment with John to tomorrow at 4 PM"
```

### Recurring Meetings

```
"Book myTeam 3 PM to 4 PM every weekday"
"Schedule team sync 9 AM every weekday from January to July"
```

## Project Structure

```
schedulehub/
├── src/
│   ├── main/
│   │   ├── java/com/bestbuy/schedulehub/
│   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── AzureConfig.java       # Azure AD authentication
│   │   │   │   └── GraphApiConfig.java    # Microsoft Graph API config
│   │   │   ├── controller/      # REST controllers
│   │   │   │   └── ScheduleController.java
│   │   │   ├── dto/             # Data transfer objects
│   │   │   │   ├── ScheduleRequest.java
│   │   │   │   ├── ScheduleResponse.java
│   │   │   │   └── ExtractedEntities.java
│   │   │   ├── service/         # Business logic services
│   │   │   │   ├── ScheduleService.java    # Main orchestration
│   │   │   │   ├── OpenAIService.java      # OpenAI NLP integration
│   │   │   │   └── GraphCalendarService.java # Graph API integration
│   │   │   └── ScheduleHubApplication.java
│   │   └── resources/
│   │       ├── static/          # Frontend HTML/JS
│   │       │   ├── index.html
│   │       │   └── app.js
│   │       └── application.yml
│   └── test/                    # Unit and integration tests
├── pom.xml
└── README.md
```

## Configuration

All configuration is done through `application.yml`. No external services like Key Vault are required.

### Required Configuration

- `azure.activedirectory.*` - Azure AD credentials for Microsoft Graph authentication
- `openai.api-key` - Your OpenAI API key
- `microsoft.graph.*` - Microsoft Graph API settings

### Optional Configuration

- `openai.model` - OpenAI model to use (default: `gpt-4o-mini`)
- `azure.application-insights.*` - Application Insights for monitoring
- `logging.level.*` - Logging configuration

## Testing

### Run All Tests

```cmd
mvn test
```

### Test Scenarios

The project includes comprehensive tests for:

1. **Simple Schedule**: "Book Mary for 2 PM tomorrow"
2. **Recurring Schedule**: "Book myTeam 3 PM to 4 PM every weekday"
3. **Cancel meeting**: "Cancel my meeting with Alex on Jan 15 at 10 AM"
4. **Reschedule meeting**: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Troubleshooting

### OpenAI API Errors

**401 Unauthorized:**

- Verify your OpenAI API key is correct in `application.yml`
- Check that your API key hasn't expired
- Ensure you have credits in your OpenAI account

**Rate Limit Errors:**

- OpenAI has rate limits based on your subscription tier
- Consider using `gpt-4o-mini` for lower costs and higher rate limits
- Add retry logic if needed

### Graph API Authentication Errors

- Verify app registration has correct permissions
- Ensure admin consent is granted
- Check that client secret hasn't expired
- Verify tenant-id, client-id, and client-secret in `application.yml`

### Intent Extraction Issues

- Check application logs for detailed OpenAI responses
- Verify the prompt in `OpenAIService.java` matches your needs
- Try rephrasing your request if intent is not recognized

## Deployment

### Build for Production

```cmd
mvn clean package -DskipTests
```

### Environment Variables (Alternative to application.yml)

You can also use environment variables:

```cmd
set AZURE_TENANT_ID=your-tenant-id
set AZURE_CLIENT_ID=your-client-id
set AZURE_CLIENT_SECRET=your-client-secret
set OPENAI_API_KEY=your-openai-key
```

## Cost Considerations

### OpenAI API Costs

- **gpt-4o-mini**: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens (very affordable)
- **gpt-4o**: ~$2.50 per 1M input tokens, ~$10 per 1M output tokens
- **gpt-3.5-turbo**: ~$0.50 per 1M input tokens, ~$1.50 per 1M output tokens

For typical scheduling requests, costs are minimal (fractions of a cent per request).

## License

This project is provided as-is for demonstration purposes.

## Documentation

- **[README.md](README.md)** - This file, main project documentation
- **[DESIGN.md](DESIGN.md)** - Complete architecture and design documentation
- **[QUICK_START.md](QUICK_START.md)** - Quick setup guide
- **[AZURE_SETUP.md](AZURE_SETUP.md)** - Detailed Azure resource setup
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - Project overview and summary

## Support

For issues and questions:

- Check application logs for detailed error messages
- Review [OpenAI API Documentation](https://platform.openai.com/docs)
- Review [Microsoft Graph API Documentation](https://docs.microsoft.com/graph/)
- Verify all configuration values in `application.yml`
- See [DESIGN.md](DESIGN.md) for architecture details
- See [AZURE_SETUP.md](AZURE_SETUP.md) for Azure configuration help
