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
3. Azure LUIS Application
4. Azure Key Vault
5. Application Insights (optional)

**Quick Checklist:**
- [ ] App registration created with Graph API permissions
- [ ] Client secret created and saved
- [ ] Speech Service key obtained
- [ ] LUIS app created, trained, and published
- [ ] LUIS key obtained
- [ ] Key Vault created with secrets

## Step 2: Configure LUIS (10-15 minutes)

Follow `LUIS_CONFIGURATION.md` to:

1. Create intents: BookMeeting, CancelMeeting, RescheduleMeeting
2. Create entities: personName, datetime, recurrence, exception, subject, location
3. Add example utterances
4. Train and publish the model

## Step 3: Local Setup (5 minutes)

### 1. Set Environment Variables

Create a `.env` file or export variables:

```bash
export AZURE_TENANT_ID="your-tenant-id"
export AZURE_CLIENT_ID="your-client-id"
export AZURE_CLIENT_SECRET="your-client-secret"
export AZURE_KEY_VAULT_URI="https://your-keyvault.vault.azure.net/"
export AZURE_SPEECH_KEY="your-speech-key"
export AZURE_SPEECH_REGION="eastus"
export AZURE_LUIS_APP_ID="your-luis-app-id"
export AZURE_LUIS_KEY="your-luis-key"
export AZURE_LUIS_REGION="eastus"
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
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
5. Check the booking response

### 4. Test API Directly

```bash
curl -X POST http://localhost:8080/api/bookMeeting \
  -H "Content-Type: application/json" \
  -d '{"text": "Book Mary for 2 PM tomorrow"}'
```

## Step 5: Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run integration tests
mvn test -Dtest=BookingIntegrationTest
```

## Common Issues

### Issue: Speech recognition not working

**Solution:**
- Verify Speech Service key and region are correct
- Check browser microphone permissions
- Ensure you're using HTTPS in production (required for mic access)

### Issue: LUIS not extracting entities

**Solution:**
- Verify LUIS app is published to Production slot
- Check that example utterances are properly labeled
- Review LUIS response in application logs

### Issue: Graph API authentication errors

**Solution:**
- Verify app registration has correct permissions
- Ensure admin consent is granted
- Check that client secret hasn't expired

### Issue: Maven build fails

**Solution:**
- Ensure Java 17 is installed: `java -version`
- Clear Maven cache: `mvn clean`
- Check internet connection for dependency downloads

## Next Steps

1. **Deploy to Azure App Service** - See `AZURE_SETUP.md` section 6
2. **Configure HTTPS** - Required for microphone access in production
3. **Set up monitoring** - Enable Application Insights alerts
4. **Improve LUIS model** - Add more training examples based on usage
5. **Add features** - SMS/email notifications, multi-language support, etc.

## Example Test Cases

The application includes automated tests for:

1. ✅ Simple booking: "Book Mary for 2 PM tomorrow"
2. ✅ Recurring booking: "Book myTeam 3 PM to 4 PM every weekday, except every second Tuesday"
3. ✅ Multiple exclusions: "Schedule team sync 9 AM every weekday, but skip Monday in the first week"
4. ✅ Cancel meeting: "Cancel my meeting with Alex on Jan 15 at 10 AM"
5. ✅ Reschedule meeting: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Support

For detailed documentation, see:
- `README.md` - Full project documentation
- `AZURE_SETUP.md` - Complete Azure resource setup
- `LUIS_CONFIGURATION.md` - LUIS model configuration

For issues:
- Check application logs
- Review Azure portal for service status
- Verify all environment variables are set correctly

