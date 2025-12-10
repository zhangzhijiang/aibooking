# Quick Start Guide

This guide will help you get the Schedule Hub up and running quickly.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Azure subscription (for Azure AD)
- OpenAI API key ([Get one here](https://platform.openai.com/api-keys))
- Microsoft 365 account

## Step 1: Azure AD Setup (10 minutes)

### 1. Create App Registration

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Click **New registration**
4. Configure:
   - **Name**: `ScheduleHub`
   - **Supported account types**: Accounts in this organizational directory only
5. Click **Register**
6. Note the **Application (client) ID** and **Directory (tenant) ID**

### 2. Create Client Secret

1. In your app registration, go to **Certificates & secrets**
2. Click **New client secret**
3. **Description**: `Schedule Hub Secret`
4. **Expires**: Choose 12 or 24 months
5. Click **Add**
6. **IMPORTANT**: Copy the **Value** immediately

### 3. Configure API Permissions

1. Go to **API permissions**
2. Click **Add a permission** > **Microsoft Graph**
3. Choose **Application permissions**
4. Add:
   - `Calendars.ReadWrite`
   - `User.Read.All`
5. Click **Grant admin consent for [your organization]**

**Quick Checklist:**

- [ ] App registration created
- [ ] Client secret created and saved
- [ ] API permissions granted with admin consent

## Step 2: Get OpenAI API Key (2 minutes)

1. Go to [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Click **Create new secret key**
4. Copy the API key (starts with `sk-`)

**Note:** You'll need to add a payment method to your OpenAI account to use the API.

## Step 3: Configure application.yml (5 minutes)

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: schedule-hub

azure:
  activedirectory:
    tenant-id: your-tenant-id # From Azure AD App Registration
    client-id: your-client-id # From Azure AD App Registration
    client-secret: your-client-secret # From Azure AD App Registration
    authority: https://login.microsoftonline.com/your-tenant-id
  application-insights:
    instrumentation-key: # Optional
    connection-string: # Optional

# OpenAI Configuration
openai:
  api-key: sk-your-openai-api-key-here # From OpenAI Platform
  model: gpt-4o-mini # Options: gpt-4o-mini (cheapest), gpt-4o, gpt-3.5-turbo

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
    endpoint: https://graph.microsoft.com/v1.0

logging:
  level:
    com.bestbuy.schedulehub: INFO
```

Replace:

- `your-tenant-id` - From Azure AD App Registration
- `your-client-id` - From Azure AD App Registration
- `your-client-secret` - From Azure AD App Registration (client secret value)
- `sk-your-openai-api-key-here` - Your OpenAI API key

## Step 4: Build and Run (2 minutes)

Open Command Prompt or PowerShell:

```cmd
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Step 5: Test the Application

### 1. Open the Web UI

Navigate to `http://localhost:8080` in your browser

### 2. Test Text Input

1. Type: "Book Mary for 2 PM tomorrow"
2. Click Submit or press Enter
3. Verify the booking confirmation appears

### 3. Test API Directly

**Important:** Include the `X-User-Id` header with a user email or object ID.

Using PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/scheduleMeeting" -Method Post -ContentType "application/json" -Headers @{"X-User-Id"="user@example.com"} -Body '{"text": "Book Mary for 2 PM tomorrow"}'
```

Or using curl:

```cmd
curl -X POST http://localhost:8080/api/scheduleMeeting -H "Content-Type: application/json" -H "X-User-Id: user@example.com" -d "{\"text\": \"Book Mary for 2 PM tomorrow\"}"
```

Replace `user@example.com` with an actual user email from your tenant.

## Step 6: Run Tests

```cmd
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ScheduleServiceTest
```

## Common Issues

### Issue: OpenAI API authentication failed

**Solution:**

- Verify OpenAI API key is correct in `application.yml`
- Check that you have credits in your OpenAI account
- Ensure the API key starts with `sk-`

### Issue: Graph API authentication errors

**Solution:**

- Verify app registration has correct permissions
- Ensure admin consent is granted
- Check that client secret hasn't expired
- Verify all Azure AD values in `application.yml` are correct

### Issue: Intent not recognized

**Solution:**

- Check application logs for detailed OpenAI responses
- Try rephrasing your request
- Verify OpenAI API key is working (check OpenAI dashboard)

### Issue: Maven build fails

**Solution:**

- Ensure Java 17 is installed: `java -version`
- Clear Maven cache: `mvn clean`
- Check internet connection for dependency downloads

## Example Test Cases

Try these requests in the web UI:

1. ✅ **Simple Schedule**: "Book Mary for 2 PM tomorrow"
2. ✅ **With Date**: "Schedule a meeting with John on January 20 at 3 PM"
3. ✅ **Recurring**: "Book myTeam 3 PM to 4 PM every weekday"
4. ✅ **Cancel**: "Cancel my meeting with Alex on Jan 15 at 10 AM"
5. ✅ **Reschedule**: "Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday"

## Next Steps

1. **Review Logs** - Check application logs for detailed booking information
2. **Test Different Requests** - Try various scheduling scenarios
3. **Deploy to Production** - See deployment section in README.md
4. **Monitor Costs** - Check OpenAI usage dashboard for API costs

## Support

For detailed documentation, see:

- `README.md` - Full project documentation
- `DESIGN.md` - Architecture and design documentation
- `AZURE_SETUP.md` - Complete Azure AD setup guide

For issues:

- Check application logs
- Review OpenAI API dashboard
- Verify all configuration values in `application.yml`
- See `DESIGN.md` for troubleshooting and architecture details
