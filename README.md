# Speech-Driven Outlook Calendar Schedule Assistant

A complete Java Spring Boot application that enables users to schedule meetings in Microsoft Outlook Calendar using speech input. Schedule Hub provides a unified interface for managing calendar events through natural language. The system integrates Azure Speech Services, CLU (Conversational Language Understanding), and Microsoft Graph API.

## Features

- 🎤 **Speech Input**: Web-based UI with microphone button using Azure Speech SDK
- 🧠 **NLP Processing**: Azure CLU for intent and entity extraction
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
       ├──► Azure CLU (NLP)
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
   - **Name**: `ScheduleHub`
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
   - **Name**: `schedule-hub`
   - **Pricing tier**: F0 (Free) or S0 (Standard)
3. Click **Create**
4. Go to the resource > **Keys and Endpoint**
5. Note the **Key 1** and **Region**

### 3. Azure CLU (Conversational Language Understanding)

1. Go to [Language Studio](https://language.cognitive.azure.com/)
2. Create a new **Language** resource in Azure Portal if you haven't already
3. Sign in to Language Studio with your Azure account
4. Create a new CLU project: **ScheduleHub**
5. Configure intents:
   - `BookMeeting`
   - `CancelMeeting`
   - `RescheduleMeeting`
6. Configure entities:
   - `PersonName` (Learned entity)
   - `DateTime` (Prebuilt entity)
   - `Recurrence` (Learned entity)
   - `Exception` (Learned entity)
   - `Subject` (Learned entity)
   - `Location` (Learned entity)
7. Train the model with example utterances (see `CLU_CONFIGURATION.md`)
8. Deploy the model to **production** (or your preferred deployment name)
9. Note the **Endpoint**, **Key**, **Project Name**, and **Deployment Name**

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
   - `azure-clu-key`
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
4. Configure **Configuration** > **Application settings** (if needed for Azure App Service)

## Local Setup

### 1. Clone and Build

Open Command Prompt or PowerShell and run:

```cmd
git clone <repository-url>
cd schedulehub
mvn clean install
```

### 2. Configure application.yml

Edit `src/main/resources/application.yml` and replace the placeholder values with your actual Azure credentials:

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
  keyvault:
    uri: https://your-keyvault.vault.azure.net/
    enabled: true
  speech:
    key: your-speech-key
    region: eastus
  clu:
    endpoint: https://your-resource-name.cognitiveservices.azure.com
    key: your-clu-key
    project-name: ScheduleHub
    deployment-name: production
    api-version: 2022-05-01
  application-insights:
    instrumentation-key: # Optional: your-instrumentation-key
    connection-string: # Optional: your-connection-string

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
    endpoint: https://graph.microsoft.com/v1.0

logging:
  level:
    com.bestbuy.schedulehub: INFO
    com.microsoft.graph: DEBUG
    com.azure: DEBUG
```

### 3. Run the Application

```cmd
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the Web UI

Open your browser and navigate to `http://localhost:8080`

1. Enter your Azure Speech Service key and region
2. Click the microphone button
3. Speak your schedule request (e.g., "Book Mary for 2 PM tomorrow")

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

```cmd
mvn test
```

### Test Scenarios

The project includes comprehensive tests for:

1. **Simple Schedule**: "Book Mary for 2 PM tomorrow" (natural language example)
2. **Recurring Schedule with exception**: "Book myTeam 3 PM to 4 PM every weekday, except every second Tuesday" (natural language example)
3. **Multiple exclusions**: "Schedule team sync 9 AM every weekday, but skip Monday in the first week and skip Tuesday/Thursday in the second week"
4. **Cancel meeting**: "Cancel my meeting with Alex on Jan 15 at 10 AM"
5. **Reschedule meeting**: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Deployment to Azure App Service

### 1. Build the Application

```cmd
mvn clean package -DskipTests
```

### 2. Deploy via Azure CLI

In PowerShell (Azure CLI commands work the same on Windows):

```powershell
az webapp deploy `
  --resource-group <resource-group> `
  --name speech-calendar-app `
  --type jar `
  --src-path target/schedule-hub-1.0.0.jar
```

**Note:** Use backtick `` ` `` for line continuation in PowerShell. In Command Prompt, you can use `^` but PowerShell is recommended for Azure CLI.

### 3. Configure App Settings

```powershell
az webapp config appsettings set `
  --resource-group <resource-group> `
  --name speech-calendar-app `
  --settings `
    AZURE_TENANT_ID="<tenant-id>" `
    AZURE_CLIENT_ID="<client-id>" `
    AZURE_CLIENT_SECRET="<client-secret>" `
    AZURE_KEY_VAULT_URI="<key-vault-uri>" `
    AZURE_SPEECH_KEY="<speech-key>" `
    AZURE_SPEECH_REGION="eastus" `
    AZURE_CLU_RESOURCE_NAME="<resource-name>" `
    AZURE_CLU_KEY="<clu-key>" `
    AZURE_CLU_PROJECT_NAME="ScheduleHub" `
    AZURE_CLU_DEPLOYMENT_NAME="production" `
    AZURE_CLU_API_VERSION="2022-05-01"
```

## Project Structure

```
schedulehub/
├── src/
│   ├── main/
│   │   ├── java/com/bestbuy/schedulehub/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── service/         # Business logic services
│   │   │   └── ScheduleHubApplication.java
│   │   └── resources/
│   │       ├── static/          # Frontend HTML/JS
│   │       └── application.yml
│   └── test/                    # Unit and integration tests
├── pom.xml
└── README.md
```

## Troubleshooting

### Speech Recognition Not Working

- Verify Azure Speech Service key and region are correct
- Check browser microphone permissions
- Ensure HTTPS is used in production (required for microphone access)

### CLU Not Extracting Entities

- Verify CLU project is deployed to production deployment
- Check that example utterances are properly labeled
- Review CLU response in application logs
- Ensure project name and deployment name match configuration

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
- [Azure CLU Documentation](https://learn.microsoft.com/azure/ai-services/language-service/conversational-language-understanding/overview)
