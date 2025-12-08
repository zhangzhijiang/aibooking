# Complete Azure Setup Guide

This guide provides step-by-step instructions for setting up all Azure resources required for the Speech Calendar Assistant.

## Prerequisites

- Azure subscription (free tier works for testing)
- Microsoft 365 account with Outlook Calendar access
- Azure CLI installed (optional, for command-line setup)

## 1. Azure AD App Registration

### Portal Setup

1. Navigate to [Azure Portal](https://portal.azure.com)
2. Go to **Azure Active Directory** > **App registrations**
3. Click **New registration**

**Configuration:**
- **Name**: `SpeechCalendarAssistant`
- **Supported account types**: 
  - For single tenant: `Accounts in this organizational directory only`
  - For multi-tenant: `Accounts in any organizational directory`
- **Redirect URI**: 
  - Type: `Web`
  - URI: `http://localhost:8080` (for local development)
  - Add another: `https://<your-app-service>.azurewebsites.net` (for production)

4. Click **Register**

**Note down:**
- **Application (client) ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- **Directory (tenant) ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

### Create Client Secret

1. In your app registration, go to **Certificates & secrets**
2. Click **New client secret**
3. **Description**: `Speech Calendar Assistant Secret`
4. **Expires**: Choose 12 or 24 months
5. Click **Add**
6. **IMPORTANT**: Copy the **Value** immediately (it won't be shown again)

### Configure API Permissions

1. Go to **API permissions**
2. Click **Add a permission**
3. Select **Microsoft Graph**
4. Choose **Application permissions** (not Delegated)
5. Add the following permissions:
   - `Calendars.ReadWrite` - Read and write calendars
   - `User.Read.All` - Read all users' profiles
6. Click **Add permissions**
7. **IMPORTANT**: Click **Grant admin consent for [your organization]**
8. Verify both permissions show "Granted for [your org]"

### Azure CLI Alternative

```bash
# Login
az login

# Create app registration
az ad app create \
  --display-name "SpeechCalendarAssistant" \
  --web-redirect-uris "http://localhost:8080" \
  --required-resource-accesses @manifest.json

# Create service principal
az ad sp create --id <app-id>

# Create client secret
az ad app credential reset --id <app-id> --append

# Grant permissions (requires admin)
az ad app permission admin-consent --id <app-id>
```

## 2. Azure Speech Service

### Portal Setup

1. Go to **Create a resource**
2. Search for **Speech**
3. Click **Create**

**Configuration:**
- **Subscription**: Your subscription
- **Resource group**: 
  - Create new: `speech-calendar-rg`
  - Or use existing
- **Region**: Choose closest (e.g., `eastus`, `westus2`)
- **Name**: `speech-calendar-assistant` (must be globally unique)
- **Pricing tier**: 
  - `F0` - Free (5 hours/month, limited features)
  - `S0` - Standard (pay-as-you-go)

4. Click **Review + create** > **Create**

### Get Credentials

1. Go to your Speech resource
2. Navigate to **Keys and Endpoint**
3. Note:
   - **Key 1**: `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - **Region**: `eastus`

### Azure CLI Alternative

```bash
az cognitiveservices account create \
  --name speech-calendar-assistant \
  --resource-group speech-calendar-rg \
  --kind SpeechServices \
  --sku F0 \
  --location eastus

# Get keys
az cognitiveservices account keys list \
  --name speech-calendar-assistant \
  --resource-group speech-calendar-rg
```

## 3. Azure LUIS

### Portal Setup

1. Go to [LUIS Portal](https://www.luis.ai)
2. Sign in with your Azure account
3. Click **Create new app**
4. Configure:
   - **Name**: `SpeechCalendarAssistant`
   - **Culture**: `English (en-us)`
5. Follow the detailed configuration in `LUIS_CONFIGURATION.md`

### Create Azure Resource

1. In LUIS Portal, go to **Manage** > **Azure Resources**
2. Click **Add resource** > **Create new resource**
3. Configure:
   - **Resource name**: `SpeechCalendarAssistant-LUIS`
   - **Subscription**: Your subscription
   - **Resource group**: `speech-calendar-rg`
   - **Pricing tier**: `F0` (Free) or `S0` (Standard)
   - **Location**: Same as Speech Service
4. Click **Done**

### Get Credentials

1. Go to **Manage** > **Keys and Endpoint**
2. Note:
   - **App ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
   - **Primary Key**: `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - **Region**: `eastus`
   - **Endpoint**: `https://eastus.api.cognitive.microsoft.com`

## 4. Azure Key Vault

### Portal Setup

1. Go to **Create a resource**
2. Search for **Key Vault**
3. Click **Create**

**Configuration:**
- **Subscription**: Your subscription
- **Resource group**: `speech-calendar-rg`
- **Key vault name**: `speech-calendar-kv` (must be globally unique)
- **Region**: Same as other resources
- **Pricing tier**: `Standard`

4. Click **Review + create** > **Create**

### Configure Access Policy

1. Go to your Key Vault
2. Navigate to **Access policies**
3. Click **Add Access Policy**
4. **Configure from template**: Leave blank
5. **Secret permissions**: Select `Get`, `List`, `Set`
6. **Select principal**: Search for your app registration name
7. Click **Add**
8. Click **Save**

### Add Secrets

1. Go to **Secrets**
2. Click **Generate/Import** for each:

**azure-speech-key**
- Name: `azure-speech-key`
- Value: Your Speech Service Key 1
- Click **Create**

**azure-luis-key**
- Name: `azure-luis-key`
- Value: Your LUIS Primary Key
- Click **Create**

**azure-client-secret**
- Name: `azure-client-secret`
- Value: Your Azure AD client secret
- Click **Create**

### Azure CLI Alternative

```bash
# Create Key Vault
az keyvault create \
  --name speech-calendar-kv \
  --resource-group speech-calendar-rg \
  --location eastus

# Set access policy
az keyvault set-policy \
  --name speech-calendar-kv \
  --spn <app-client-id> \
  --secret-permissions get list set

# Add secrets
az keyvault secret set \
  --vault-name speech-calendar-kv \
  --name azure-speech-key \
  --value "<speech-key>"

az keyvault secret set \
  --vault-name speech-calendar-kv \
  --name azure-luis-key \
  --value "<luis-key>"

az keyvault secret set \
  --vault-name speech-calendar-kv \
  --name azure-client-secret \
  --value "<client-secret>"
```

## 5. Application Insights

### Portal Setup

1. Go to **Create a resource**
2. Search for **Application Insights**
3. Click **Create**

**Configuration:**
- **Subscription**: Your subscription
- **Resource group**: `speech-calendar-rg`
- **Name**: `speech-calendar-insights`
- **Region**: Same as other resources
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

```bash
az monitor app-insights component create \
  --app speech-calendar-insights \
  --location eastus \
  --resource-group speech-calendar-rg \
  --application-type java

# Get instrumentation key
az monitor app-insights component show \
  --app speech-calendar-insights \
  --resource-group speech-calendar-rg \
  --query instrumentationKey
```

## 6. Azure App Service (Optional - for Production)

### Portal Setup

1. Go to **Create a resource**
2. Search for **Web App**
3. Click **Create**

**Configuration:**
- **Subscription**: Your subscription
- **Resource group**: `speech-calendar-rg`
- **Name**: `speech-calendar-app` (must be globally unique)
- **Publish**: `Code`
- **Runtime stack**: `Java 17`
- **Operating System**: `Linux`
- **Region**: Same as other resources
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
AZURE_KEY_VAULT_URI = https://speech-calendar-kv.vault.azure.net/
AZURE_SPEECH_KEY = <your-speech-key>
AZURE_SPEECH_REGION = eastus
AZURE_LUIS_APP_ID = <your-luis-app-id>
AZURE_LUIS_KEY = <your-luis-key>
AZURE_LUIS_REGION = eastus
APPLICATIONINSIGHTS_INSTRUMENTATION_KEY = <your-instrumentation-key>
APPLICATIONINSIGHTS_CONNECTION_STRING = <your-connection-string>
```

4. Click **Save**

### Enable Managed Identity (Recommended)

1. Go to **Identity**
2. **System assigned** tab
3. Set **Status** to **On**
4. Click **Save**
5. Go to Key Vault > **Access policies**
6. Add access policy for the App Service managed identity

### Azure CLI Alternative

```bash
# Create App Service Plan
az appservice plan create \
  --name speech-calendar-plan \
  --resource-group speech-calendar-rg \
  --sku F1 \
  --is-linux

# Create Web App
az webapp create \
  --name speech-calendar-app \
  --resource-group speech-calendar-rg \
  --plan speech-calendar-plan \
  --runtime "JAVA:17-java17"

# Configure app settings
az webapp config appsettings set \
  --name speech-calendar-app \
  --resource-group speech-calendar-rg \
  --settings \
    AZURE_TENANT_ID="<tenant-id>" \
    AZURE_CLIENT_ID="<client-id>" \
    AZURE_CLIENT_SECRET="<client-secret>" \
    AZURE_KEY_VAULT_URI="https://speech-calendar-kv.vault.azure.net/" \
    AZURE_SPEECH_KEY="<speech-key>" \
    AZURE_SPEECH_REGION="eastus" \
    AZURE_LUIS_APP_ID="<luis-app-id>" \
    AZURE_LUIS_KEY="<luis-key>" \
    AZURE_LUIS_REGION="eastus"
```

## 7. Environment Variables Summary

Create a `.env` file or set these in your deployment:

```bash
# Azure AD
AZURE_TENANT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AZURE_CLIENT_SECRET=your-client-secret-value

# Azure Key Vault
AZURE_KEY_VAULT_URI=https://speech-calendar-kv.vault.azure.net/

# Azure Speech Service
AZURE_SPEECH_KEY=your-speech-key
AZURE_SPEECH_REGION=eastus

# Azure LUIS
AZURE_LUIS_APP_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AZURE_LUIS_KEY=your-luis-key
AZURE_LUIS_REGION=eastus

# Application Insights (Optional)
APPLICATIONINSIGHTS_INSTRUMENTATION_KEY=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=xxx;IngestionEndpoint=xxx
```

## 8. Verification Checklist

- [ ] Azure AD app registration created with correct permissions
- [ ] Client secret created and saved
- [ ] Admin consent granted for Graph API permissions
- [ ] Speech Service created and key obtained
- [ ] LUIS app created, trained, and published
- [ ] LUIS Azure resource linked and key obtained
- [ ] Key Vault created with access policy configured
- [ ] Secrets added to Key Vault
- [ ] Application Insights created (optional)
- [ ] App Service created and configured (if deploying)
- [ ] All environment variables set correctly
- [ ] Application starts without errors
- [ ] Health endpoint returns success
- [ ] Speech recognition works in browser
- [ ] LUIS extracts intents and entities correctly
- [ ] Calendar events can be created via Graph API

## 9. Cost Estimation

**Free Tier (F0) Resources:**
- Speech Service: 5 hours/month free
- LUIS: 10,000 transactions/month free
- App Service: Limited free tier available

**Estimated Monthly Cost (Standard Tier):**
- Speech Service (S0): ~$1 per hour of audio
- LUIS (S0): ~$0.50 per 1,000 transactions
- App Service (B1): ~$13/month
- Key Vault: ~$0.03 per 10,000 operations
- Application Insights: First 5GB free, then ~$2.30/GB

**Total (light usage)**: ~$15-20/month
**Total (heavy usage)**: ~$50-100/month

## 10. Troubleshooting

### Authentication Errors

- Verify tenant ID, client ID, and secret are correct
- Check that admin consent is granted
- Ensure client secret hasn't expired
- Verify app registration has correct permissions

### Key Vault Access Denied

- Check access policy includes your app/service principal
- Verify managed identity is enabled (if using)
- Ensure Key Vault firewall allows your IP (if configured)

### Speech Recognition Issues

- Verify Speech Service key and region
- Check browser microphone permissions
- Ensure HTTPS is used in production
- Review browser console for errors

### LUIS Not Working

- Verify app is published to Production slot
- Check endpoint URL is correct
- Ensure key and region match
- Review LUIS portal for errors

### Graph API Errors

- Verify permissions are granted and consented
- Check that user has calendar access
- Ensure correct user ID format
- Review Graph API error messages

## Next Steps

1. Complete LUIS configuration (see `LUIS_CONFIGURATION.md`)
2. Deploy the application locally
3. Test all functionality
4. Deploy to Azure App Service
5. Monitor using Application Insights
6. Set up alerts for errors

