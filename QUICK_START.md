# Quick Start Guide

This guide will help you get the Speech Calendar Assistant up and running quickly.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Azure subscription
- Microsoft 365 account

## Step 1: Azure Setup (15-20 minutes)

Follow the detailed instructions in `AZURE_SETUP.md` to create:

1. Azure AD App Registration
2. Azure Speech Service
3. Azure CLU (Conversational Language Understanding) Project
4. Azure Key Vault
5. Application Insights (optional)

**Quick Checklist:**

- [ ] App registration created with Graph API permissions
- [ ] Client secret created and saved
- [ ] Speech Service key obtained
- [ ] CLU project created, trained, and deployed
- [ ] CLU key and endpoint obtained
- [ ] Key Vault created with secrets

## Step 2: Configure CLU (10-15 minutes)

Follow `CLU_CONFIGURATION.md` to:

1. Create intents: BookMeeting, CancelMeeting, RescheduleMeeting
2. Create entities: PersonName, DateTime, Recurrence, Exception, Subject, Location
3. Add example utterances
4. Train and deploy the model

## Step 3: Local Setup (5 minutes)

### 1. Configure application.yml

Edit `src/main/resources/application.yml` and replace the placeholder values with your actual Azure credentials:

```yaml
server:
  port: 8080

spring:
  application:
    name: speech-calendar-assistant

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
    project-name: SpeechCalendarAssistant
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

Replace the following values:

- `your-tenant-id` - Your Azure AD tenant ID
- `your-client-id` - Your Azure AD app registration client ID
- `your-client-secret` - Your Azure AD client secret
- `your-keyvault.vault.azure.net` - Your Key Vault URI
- `your-speech-key` - Your Azure Speech Service key
- `eastus` - Your Speech Service region (or your preferred region)
- `your-resource-name` - Your CLU Language Service resource name
- `your-clu-key` - Your CLU Language Service key

### 2. Build the Project

Open Command Prompt or PowerShell and run:

```cmd
mvn clean install
```

### 3. Run the Application

```cmd
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Step 4: Test the Application

### 1. Open the Web UI

Navigate to `http://localhost:8080` in your browser

### 2. Configure Speech Service

- Enter your Azure Speech Service key
- Enter your Azure Speech Service region (e.g., `eastus`)

### 3. Test Speech Recognition

1. Click the microphone button
2. Allow microphone access when prompted
3. Say: "Book Mary for 2 PM tomorrow"
4. Verify the transcription appears
5. Check the Schedule response

### 4. Test API Directly

Using PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/scheduleMeeting" -Method Post -ContentType "application/json" -Body '{"text": "Book Mary for 2 PM tomorrow"}'
```

Or using curl (if installed on Windows 10/11):

```cmd
curl -X POST http://localhost:8080/api/scheduleMeeting -H "Content-Type: application/json" -d "{\"text\": \"Book Mary for 2 PM tomorrow\"}"
```

## Step 5: Run Tests

Open Command Prompt or PowerShell and run:

```cmd
REM Run all tests
mvn test

REM Run specific test class
mvn test -Dtest=ScheduleServiceTest

REM Run integration tests
mvn test -Dtest=ScheduleIntegrationTest
```

**Note:** Use `REM` for comments in Command Prompt, or `#` in PowerShell.

## Common Issues

### Issue: Speech recognition not working

**Solution:**

- Verify Speech Service key and region are correct
- Check browser microphone permissions
- Ensure you're using HTTPS in production (required for mic access)

### Issue: CLU not extracting entities

**Solution:**

- Verify CLU project is deployed to production deployment
- Check that example utterances are properly labeled
- Review CLU response in application logs
- Ensure project name and deployment name match configuration

### Issue: Graph API authentication errors

**Solution:**

- Verify app registration has correct permissions
- Ensure admin consent is granted
- Check that client secret hasn't expired

### Issue: Maven build fails

**Solution:**

- Ensure Java 17 is installed: `java -version` (works in both Command Prompt and PowerShell)
- Clear Maven cache: `mvn clean`
- Check internet connection for dependency downloads

## Next Steps

1. **Deploy to Azure App Service** - See `AZURE_SETUP.md` section 6
2. **Configure HTTPS** - Required for microphone access in production
3. **Set up monitoring** - Enable Application Insights alerts
4. **Improve CLU model** - Add more training examples based on usage
5. **Add features** - SMS/email notifications, multi-language support, etc.

## Example Test Cases

The application includes automated tests for:

1. ✅ Simple Schedule: "Book Mary for 2 PM tomorrow"
2. ✅ Recurring Schedule: "Book myTeam 3 PM to 4 PM every weekday, except every second Tuesday"
3. ✅ Multiple exclusions: "Schedule team sync 9 AM every weekday, but skip Monday in the first week"
4. ✅ Cancel meeting: "Cancel my meeting with Alex on Jan 15 at 10 AM"
5. ✅ Reschedule meeting: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Support

For detailed documentation, see:

- `README.md` - Full project documentation
- `AZURE_SETUP.md` - Complete Azure resource setup
- `CLU_CONFIGURATION.md` - CLU model configuration

For issues:

- Check application logs
- Review Azure portal for service status
- Verify all configuration values in `application.yml` are correct
