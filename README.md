# Speech-Driven Outlook Calendar Booking Assistant

A complete Java Spring Boot application that enables users to book meetings in Microsoft Outlook Calendar using speech input. The system integrates Azure Speech Services, LUIS (Language Understanding), and Microsoft Graph API.

## Features

- 🎤 **Speech Input**: Web-based UI with microphone button using Azure Speech SDK
- 🧠 **NLP Processing**: Azure LUIS for intent and entity extraction
- 📅 **Calendar Integration**: Microsoft Graph API for creating, updating, and deleting calendar events
- 🔄 **Recurring Events**: Support for complex recurrence patterns with exception handling
- 🔐 **Security**: Azure AD OAuth 2.0 authentication with Key Vault for secrets
- 📊 **Monitoring**: Azure Application Insights integration
- ✅ **Automated Tests**: Comprehensive unit and integration tests

## Architecture

```
┌─────────────┐
│   Browser   │
│  (HTML/JS)  │
└──────┬──────┘
       │ Speech SDK
       ▼
┌─────────────────┐
│  Spring Boot    │
│  REST API       │
└──────┬──────────┘
       │
       ├──► Azure LUIS (NLP)
       ├──► Microsoft Graph API (Calendar)
       ├──► Azure Key Vault (Secrets)
       └──► Application Insights (Monitoring)
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Azure subscription
- Microsoft 365 account with Outlook Calendar access

## Azure Resources Setup

### 1. Azure AD App Registration

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Click **New registration**
4. Configure:
   - **Name**: `SpeechCalendarAssistant`
   - **Supported account types**: Accounts in this organizational directory only
   - **Redirect URI**: `http://localhost:8080` (for local testing)
5. Click **Register**
6. Note the **Application (client) ID** and **Directory (tenant) ID**
7. Go to **Certificates & secrets** > **New client secret**
8. Create a secret and note the **Value** (you'll need it later)
9. Go to **API permissions** > **Add a permission** > **Microsoft Graph**
10. Add the following **Application permissions**:
    - `Calendars.ReadWrite`
    - `User.Read.All`
11. Click **Grant admin consent**

### 2. Azure Speech Service

1. Go to **Create a resource** > **Speech**
2. Configure:
   - **Subscription**: Your subscription
   - **Resource group**: Create new or use existing
   - **Region**: Choose closest region (e.g., `eastus`)
   - **Name**: `speech-calendar-assistant`
   - **Pricing tier**: F0 (Free) or S0 (Standard)
3. Click **Create**
4. Go to the resource > **Keys and Endpoint**
5. Note the **Key 1** and **Region**

### 3. Azure LUIS

1. Go to [LUIS Portal](https://www.luis.ai)
2. Sign in with your Azure account
3. Create a new app: **SpeechCalendarAssistant**
4. Configure intents:
   - `BookMeeting`
   - `CancelMeeting`
   - `RescheduleMeeting`
5. Configure entities:
   - `personName` (Simple entity)
   - `datetime` (Prebuilt: `datetimeV2`)
   - `recurrence` (Simple entity)
   - `exception` (Simple entity)
   - `subject` (Simple entity)
   - `location` (Simple entity)
6. Train the model with example utterances (see `LUIS_CONFIGURATION.md`)
7. Publish the app to **Production** slot
8. Go to **Manage** > **Azure Resources**
9. Create a new resource or link existing
10. Note the **App ID**, **Key**, and **Region**

### 4. Azure Key Vault

1. Go to **Create a resource** > **Key Vault**
2. Configure:
   - **Name**: `speech-calendar-kv`
   - **Resource group**: Same as above
   - **Region**: Same as above
   - **Pricing tier**: Standard
3. Click **Create**
4. Go to **Access policies** > **Add Access Policy**
5. Select your app registration's service principal
6. Grant **Secret permissions**: Get, List, Set
7. Add secrets:
   - `azure-speech-key`
   - `azure-luis-key`
   - `azure-client-secret`

### 5. Application Insights

1. Go to **Create a resource** > **Application Insights**
2. Configure:
   - **Name**: `speech-calendar-insights`
   - **Resource group**: Same as above
   - **Application type**: Java
3. Click **Create**
4. Note the **Instrumentation Key** and **Connection String**

### 6. Azure App Service (Optional - for deployment)

1. Go to **Create a resource** > **Web App**
2. Configure:
   - **Name**: `speech-calendar-app`
   - **Runtime stack**: Java 17
   - **Operating System**: Linux
3. Click **Create**
4. Configure **Configuration** > **Application settings** with all environment variables

## Local Setup

### 1. Clone and Build

```bash
git clone <repository-url>
cd aibooking
mvn clean install
```

### 2. Configure Environment Variables

Create a `.env` file or set environment variables:

```bash
# Azure AD
export AZURE_TENANT_ID="your-tenant-id"
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"

# Azure Key Vault
export AZURE_KEY_VAULT_URI="https://speech-calendar-kv.vault.azure.net/"

# Azure Speech Service
export AZURE_SPEECH_KEY="your-speech-key"
export AZURE_SPEECH_REGION="eastus"

# Azure LUIS
export AZURE_LUIS_APP_ID="your-luis-app-id"
export AZURE_LUIS_KEY="your-luis-key"
export AZURE_LUIS_REGION="eastus"

# Application Insights (optional)
export APPLICATIONINSIGHTS_INSTRUMENTATION_KEY="your-instrumentation-key"
export APPLICATIONINSIGHTS_CONNECTION_STRING="your-connection-string"
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the Web UI

Open your browser and navigate to `http://localhost:8080`

1. Enter your Azure Speech Service key and region
2. Click the microphone button
3. Speak your booking request (e.g., "Book Mary for 2 PM tomorrow")

## API Endpoints

### POST /api/bookMeeting

Book, cancel, or reschedule a meeting based on natural language input.

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

## Testing

### Run All Tests

```bash
mvn test
```

### Test Scenarios

The project includes comprehensive tests for:

1. **Simple booking**: "Book Mary for 2 PM tomorrow"
2. **Recurring booking with exception**: "Book myTeam 3 PM to 4 PM every weekday, except every second Tuesday"
3. **Multiple exclusions**: "Schedule team sync 9 AM every weekday, but skip Monday in the first week and skip Tuesday/Thursday in the second week"
4. **Cancel meeting**: "Cancel my meeting with Alex on Jan 15 at 10 AM"
5. **Reschedule meeting**: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Deployment to Azure App Service

### 1. Build the Application

```bash
mvn clean package -DskipTests
```

### 2. Deploy via Azure CLI

```bash
az webapp deploy \
  --resource-group <resource-group> \
  --name speech-calendar-app \
  --type jar \
  --src-path target/speech-calendar-assistant-1.0.0.jar
```

### 3. Configure App Settings

```bash
az webapp config appsettings set \
  --resource-group <resource-group> \
  --name speech-calendar-app \
  --settings \
    AZURE_TENANT_ID="<tenant-id>" \
    AZURE_CLIENT_ID="<client-id>" \
    AZURE_CLIENT_SECRET="<client-secret>" \
    AZURE_KEY_VAULT_URI="<key-vault-uri>" \
    AZURE_SPEECH_KEY="<speech-key>" \
    AZURE_SPEECH_REGION="eastus" \
    AZURE_LUIS_APP_ID="<luis-app-id>" \
    AZURE_LUIS_KEY="<luis-key>" \
    AZURE_LUIS_REGION="eastus"
```

## Project Structure

```
aibooking/
├── src/
│   ├── main/
│   │   ├── java/com/aibooking/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── service/         # Business logic services
│   │   │   └── SpeechCalendarAssistantApplication.java
│   │   └── resources/
│   │       ├── static/          # Frontend HTML/JS
│   │       └── application.properties
│   └── test/                    # Unit and integration tests
├── pom.xml
└── README.md
```

## Troubleshooting

### Speech Recognition Not Working

- Verify Azure Speech Service key and region are correct
- Check browser microphone permissions
- Ensure HTTPS is used in production (required for microphone access)

### LUIS Not Extracting Entities

- Verify LUIS app is published to Production slot
- Check that example utterances are properly labeled
- Review LUIS response in application logs

### Graph API Authentication Errors

- Verify app registration has correct permissions
- Ensure admin consent is granted
- Check that client secret hasn't expired

### Key Vault Access Denied

- Verify access policy includes your app registration
- Check that managed identity is configured (if using)

## License

This project is provided as-is for demonstration purposes.

## Support

For issues and questions, please refer to:

- [Azure Speech Service Documentation](https://docs.microsoft.com/azure/cognitive-services/speech-service/)
- [Microsoft Graph API Documentation](https://docs.microsoft.com/graph/)
- [Azure LUIS Documentation](https://docs.microsoft.com/azure/cognitive-services/luis/)
