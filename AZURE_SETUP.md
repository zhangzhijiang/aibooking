# Complete Azure Setup Guide

This guide provides step-by-step instructions for setting up all Azure resources required for the Schedule Hub.

## Prerequisites

- Azure subscription (free tier works for testing)
- Microsoft 365 account with Outlook Calendar access
- OpenAI API account ([Sign up here](https://platform.openai.com/signup))
- Azure CLI installed (optional, for command-line setup)

## 1. Azure AD App Registration

### Portal Setup

1. Navigate to [Azure Portal](https://portal.azure.com)
2. Go to **Azure Active Directory** > **App registrations**
3. Click **New registration**

**Configuration:**

- **Name**: `ScheduleHub`
- **Supported account types**:
  - For single tenant: `Accounts in this organizational directory only`
  - For multi-tenant: `Accounts in any organizational directory`
- **Redirect URI**: Not required for application authentication (only needed for delegated authentication)
  - You can skip this step or leave it empty
  - If you add one, use: `http://localhost:8080` (for local development)

4. Click **Register**

**Note down:**

- **Application (client) ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- **Directory (tenant) ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

### Create Client Secret

1. In your app registration, go to **Certificates & secrets**
2. Click **New client secret**
3. **Description**: `Schedule Hub Secret`
4. **Expires**: Choose 12 or 24 months
5. Click **Add**
6. **IMPORTANT**: Copy the **Value** immediately (it won't be shown again)

### Configure API Permissions

**This application uses Application Permissions (Service-Driven Architecture)**

The application acts as a service principal and can manage calendars for all users in your tenant. No user login is required.

1. Go to **API permissions**
2. Click **Add a permission**
3. Select **Microsoft Graph**
4. Choose **Application permissions** (NOT Delegated)
5. Add the following permissions:
   - `Calendars.ReadWrite` - Read and write calendars for all users
   - `User.Read.All` - Read all users' profiles (required to resolve user IDs)
6. Click **Add permissions**
7. **IMPORTANT**: Click **Grant admin consent for [your organization]**
8. Verify both permissions show "Granted for [your org]" with a green checkmark

**Why Application Permissions?**

- **Service-Driven**: Runs as a background service, no user interaction required
- **Multi-User**: Can manage calendars for any user in the tenant
- **Automated**: Perfect for automated scheduling systems
- **Secure**: Uses client credentials (client ID + secret) for authentication

**Configuration in application.yml:**

```yaml
azure:
  activedirectory:
    authentication-mode: application
    client-secret: your-client-secret-value

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
```

**Important Notes:**

- The application requires the `X-User-Id` header in API requests to specify which user's calendar to manage
- User ID can be an email address (e.g., `user@example.com`) or Azure AD object ID
- The application must have admin consent granted for the permissions

### Azure CLI Alternative

In PowerShell (recommended for Azure CLI on Windows):

```powershell
# Login
az login

# Create app registration
az ad app create `
  --display-name "ScheduleHub" `
  --web-redirect-uris "http://localhost:8080"

# Get app ID
$appId = az ad app list --display-name "ScheduleHub" --query [0].appId -o tsv

# Create service principal
az ad sp create --id $appId

# Create client secret
az ad app credential reset --id $appId --append

# Grant permissions (requires admin)
az ad app permission admin-consent --id $appId
```

**Note:** Use backtick `` ` `` for line continuation in PowerShell.

## 2. OpenAI API Setup

### Get API Key

1. Go to [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Click **Create new secret key**
4. **Name**: `Schedule Hub` (optional)
5. Click **Create secret key**
6. **IMPORTANT**: Copy the API key immediately (starts with `sk-`)

### Add Payment Method

1. Go to [OpenAI Billing](https://platform.openai.com/account/billing)
2. Click **Add payment method**
3. Add your credit card or other payment method
4. Set up usage limits if desired

**Note:** OpenAI requires a payment method to use the API, even for free tier usage.

### Choose Model

The application uses `gpt-4o-mini` by default (most cost-effective). You can change this in `application.yml`:

- **gpt-4o-mini** (recommended): Cheapest, fast, good quality
- **gpt-4o**: Better quality, more expensive
- **gpt-3.5-turbo**: Older alternative

### Cost Information

- **gpt-4o-mini**: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- Typical scheduling request: ~100-200 tokens (~$0.0001 per request)
- Very affordable for most use cases

## 3. Application Insights (Optional)

### Portal Setup

1. Go to **Create a resource**
2. Search for **Application Insights**
3. Click **Create**

**Configuration:**

- **Subscription**: Your subscription
- **Resource group**: Create new or use existing
- **Name**: `schedule-hub-insights`
- **Region**: Choose closest region
- **Application type**: `Java`
- **Workspace**: Create new or use existing

4. Click **Review + create** > **Create**

### Get Credentials

1. Go to your Application Insights resource
2. Navigate to **Overview**
3. Note:
   - **Instrumentation Key**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
4. Go to **Connection String**
5. Note the **Connection String**

### Azure CLI Alternative

In PowerShell:

```powershell
az monitor app-insights component create `
  --app schedule-hub-insights `
  --location eastus `
  --resource-group <your-resource-group> `
  --application-type java

# Get instrumentation key
az monitor app-insights component show `
  --app schedule-hub-insights `
  --resource-group <your-resource-group> `
  --query instrumentationKey
```

## 4. Azure App Service (Optional - for Production)

### Portal Setup

1. Go to **Create a resource**
2. Search for **Web App**
3. Click **Create**

**Configuration:**

- **Subscription**: Your subscription
- **Resource group**: Create new or use existing
- **Name**: `schedule-hub-app` (must be globally unique)
- **Publish**: `Code`
- **Runtime stack**: `Java 17`
- **Operating System**: `Linux`
- **Region**: Choose closest region
- **App Service Plan**: Create new or use existing
  - **Sku and size**: `F1` (Free) for testing, `B1` (Basic) for production

4. Click **Review + create** > **Create**

### Configure Application Settings

1. Go to your App Service
2. Navigate to **Configuration** > **Application settings**
3. Click **New application setting** for each:

```
AZURE_TENANT_ID = <your-tenant-id>
AZURE_CLIENT_ID = <your-client-id>
AZURE_CLIENT_SECRET = <your-client-secret>
OPENAI_API_KEY = <your-openai-api-key>
OPENAI_MODEL = gpt-4o-mini
APPLICATIONINSIGHTS_INSTRUMENTATION_KEY = <your-instrumentation-key> (optional)
APPLICATIONINSIGHTS_CONNECTION_STRING = <your-connection-string> (optional)
```

4. Click **Save**

### Azure CLI Alternative

In PowerShell:

```powershell
# Create App Service Plan
az appservice plan create `
  --name schedule-hub-plan `
  --resource-group <your-resource-group> `
  --sku F1 `
  --is-linux

# Create Web App
az webapp create `
  --name schedule-hub-app `
  --resource-group <your-resource-group> `
  --plan schedule-hub-plan `
  --runtime "JAVA:17-java17"

# Configure app settings
az webapp config appsettings set `
  --name schedule-hub-app `
  --resource-group <your-resource-group> `
  --settings `
    AZURE_TENANT_ID="<tenant-id>" `
    AZURE_CLIENT_ID="<client-id>" `
    AZURE_CLIENT_SECRET="<client-secret>" `
    OPENAI_API_KEY="<openai-key>" `
    OPENAI_MODEL="gpt-4o-mini"
```

## 5. Configuration Summary

Configure these values in `application.yml` (see `QUICK_START.md` for details):

### Required Configuration

- **Azure AD**: `tenant-id`, `client-id`, `client-secret`
- **OpenAI**: `api-key`, `model` (optional, defaults to gpt-4o-mini)

### Optional Configuration

- **Application Insights**: `instrumentation-key`, `connection-string`

### Example application.yml

```yaml
azure:
  activedirectory:
    tenant-id: your-tenant-id
    client-id: your-client-id
    client-secret: your-client-secret
    authority: https://login.microsoftonline.com/your-tenant-id
  application-insights:
    instrumentation-key: # Optional
    connection-string: # Optional

openai:
  api-key: sk-your-openai-api-key-here
  model: gpt-4o-mini

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
    endpoint: https://graph.microsoft.com/v1.0
```

## 6. Verification Checklist

- [ ] Azure AD app registration created with correct permissions
- [ ] Client secret created and saved
- [ ] Admin consent granted for Graph API permissions
- [ ] OpenAI API key obtained
- [ ] Payment method added to OpenAI account
- [ ] Application Insights created (optional)
- [ ] App Service created and configured (if deploying)
- [ ] All configuration values in `application.yml` are set correctly
- [ ] Application starts without errors
- [ ] Health endpoint returns success
- [ ] Text input works in browser
- [ ] OpenAI extracts intents and entities correctly
- [ ] Calendar events can be created via Graph API

## 7. Cost Estimation

**Free Tier Resources:**

- App Service: Limited free tier available (F1)
- Application Insights: First 5GB free

**Estimated Monthly Cost:**

- **OpenAI API** (gpt-4o-mini):
  - ~$0.0001 per request (very affordable)
  - ~$1-5/month for light usage (100-1000 requests/day)
  - ~$10-50/month for heavy usage (10,000+ requests/day)
- **App Service** (B1 Basic): ~$13/month
- **Application Insights**: First 5GB free, then ~$2.30/GB

**Total (light usage)**: ~$15-20/month
**Total (heavy usage)**: ~$50-100/month

**Note:** OpenAI costs are very low with gpt-4o-mini. Most of the cost comes from Azure App Service if deployed.

## 8. Troubleshooting

### Authentication Errors

- Verify tenant ID, client ID, and secret are correct in `application.yml`
- Check that admin consent is granted for Graph API permissions
- Ensure client secret hasn't expired
- Verify app registration has correct permissions

### OpenAI API Errors

**401 Unauthorized:**

- Verify OpenAI API key is correct in `application.yml`
- Check that API key hasn't been revoked
- Ensure you have credits in your OpenAI account

**Rate Limit Errors:**

- OpenAI has rate limits based on your subscription tier
- Consider using `gpt-4o-mini` for higher rate limits
- Check your usage in OpenAI dashboard

**Invalid API Key:**

- Ensure API key starts with `sk-`
- Verify key is copied completely (no spaces)
- Check OpenAI dashboard to ensure key is active

### Graph API Errors

- Verify permissions are granted and consented
- Check that user has calendar access
- Ensure correct user ID format
- Review Graph API error messages in logs

### Application Startup Errors

- Verify all required configuration values are set in `application.yml`
- Check that OpenAI API key is valid
- Ensure Azure AD credentials are correct
- Review application logs for detailed error messages

## 9. Next Steps

1. Complete local setup (see `QUICK_START.md`)
2. Test the application locally
3. Deploy to Azure App Service (optional)
4. Monitor using Application Insights (optional)
5. Set up alerts for errors (optional)

## 10. Security Best Practices

### For Production

1. **Never commit secrets to Git**

   - Use environment variables or secure configuration management
   - Consider using Azure Key Vault for production (future enhancement)

2. **Rotate credentials regularly**

   - Rotate Azure AD client secrets every 90 days
   - Rotate OpenAI API keys if compromised

3. **Use environment variables**

   - Set secrets as environment variables instead of `application.yml` in production
   - Use Azure App Service Application Settings for deployment

4. **Monitor usage**
   - Set up OpenAI usage alerts
   - Monitor API costs regularly
   - Review Application Insights for anomalies

## 11. Service-Driven Architecture

**This application uses Application Permissions exclusively (Service-Driven Architecture)**

The application only supports application authentication. It acts as a service principal and can manage calendars for all users in your tenant.

### Application Authentication (Client Credentials Flow)

- **Mode**: `application`
- **Authentication**: Uses client ID + client secret
- **User Interaction**: None (background service)
- **Acts As**: Service principal (the application itself)
- **Permissions**: Application permissions (requires admin consent)
- **Scope**: `https://graph.microsoft.com/.default`
- **Use Case**: Background services, daemon apps, automated scheduling

**Configuration:**

```yaml
azure:
  activedirectory:
    authentication-mode: application
    client-secret: your-client-secret-value

microsoft:
  graph:
    scope: https://graph.microsoft.com/.default
```

**Important Notes:**

1. **Application Permissions Only**: This application only supports application permissions (service-driven architecture)
2. **Client Secret Required**: Client secret is mandatory for application authentication
3. **Admin Consent Required**: Application permissions require admin consent to be granted
4. **User ID Required**: All API requests must include the `X-User-Id` header to specify which user's calendar to manage
5. **No User Login**: The application runs as a background service with no user interaction required

## Support

For detailed setup instructions, see:

- `QUICK_START.md` - Quick start guide
- `README.md` - Full project documentation

For issues:

- Check application logs
- Review OpenAI API dashboard
- Verify all configuration values in `application.yml`
