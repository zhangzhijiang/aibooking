# Azure AD Configuration Checklist for 404 Errors

If you're getting a 404 error when calling:
```
https://graph.microsoft.com/v1.0/users/{userId}/calendar/events
```

Follow this checklist to verify your Azure AD configuration is correct.

## Step 1: Verify User Exists in Azure AD

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **Users**
3. Search for the user: `zhangzhijiang@idatagear.com`
4. Verify:
   - ✅ User exists and is active
   - ✅ **User principal name** matches exactly: `zhangzhijiang@idatagear.com`
   - ✅ User belongs to tenant: `4c626fb1-0203-4c28-bd18-e451f44d0048`
   - ✅ User account is not disabled or deleted

**Alternative:** Try using the **Object ID** (GUID) instead of email:
- Find it in: Azure AD → Users → [User] → **Object ID** field
- Use format: `12345678-1234-1234-1234-123456789abc`

## Step 2: Verify Application Permissions

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**
3. Find your app: `a8445943-d967-4c1f-9701-4e167c999eb4`
4. Click on the app name
5. Go to **API permissions**

**Required Permissions:**

| Permission | Type | Status |
|------------|------|--------|
| `Calendars.ReadWrite` | **Application** (NOT Delegated) | ✅ Granted for [your organization] |
| `User.Read.All` | **Application** (NOT Delegated) | ✅ Granted for [your organization] |

**Important:**
- ❌ **NOT** Delegated permissions
- ✅ **MUST** be Application permissions
- ✅ **MUST** show "Granted for [your organization]" with green checkmark

## Step 3: Grant Admin Consent (If Needed)

If permissions show "Not granted" or "Admin consent required":

1. In **API permissions** page
2. Click **Grant admin consent for [your organization]**
3. Click **Yes** to confirm
4. Wait a few seconds
5. Refresh the page
6. Verify both permissions now show: ✅ **Granted for [your organization]**

## Step 4: Verify Client Secret

1. In your app registration, go to **Certificates & secrets**
2. Check:
   - ✅ Client secret exists
   - ✅ Secret has NOT expired (check "Expires" column)
   - ✅ Secret value in `application.yml` matches the "Value" column (NOT Secret ID)

## Step 5: Verify Tenant ID

Check that your `application.yml` has the correct tenant ID:
```yaml
azure:
  activedirectory:
    tenant-id: 4c626fb1-0203-4c28-bd18-e451f44d0048
```

Verify this matches:
- Azure Portal → Azure Active Directory → **Overview** → **Tenant ID**

## Step 6: Test the Configuration

### Option A: Test with Email
```
X-User-Id: zhangzhijiang@idatagear.com
```

### Option B: Test with Object ID
1. Get Object ID from: Azure AD → Users → [User] → Object ID
2. Use format:
```
X-User-Id: 12345678-1234-1234-1234-123456789abc
```

## Common Issues and Solutions

### Issue: 404 Error - User Not Found

**Possible Causes:**
1. User doesn't exist in Azure AD
   - **Solution:** Verify user exists in Azure Portal → Azure AD → Users

2. Wrong email format
   - **Solution:** Use exact User Principal Name from Azure AD

3. User in different tenant
   - **Solution:** Verify tenant ID matches

### Issue: 404 Error - Permission Denied

**Possible Causes:**
1. Application permissions not granted
   - **Solution:** Grant admin consent (Step 3)

2. Wrong permission type (Delegated instead of Application)
   - **Solution:** Remove delegated permissions, add Application permissions

3. Permissions not configured
   - **Solution:** Add `Calendars.ReadWrite` and `User.Read.All` as Application permissions

### Issue: Authentication Errors

**Possible Causes:**
1. Client secret expired
   - **Solution:** Create new client secret and update `application.yml`

2. Wrong tenant ID
   - **Solution:** Verify tenant ID in `application.yml` matches Azure AD

## Quick Verification Commands

### Check Application Permissions (Azure CLI)
```powershell
az ad app permission list --id a8445943-d967-4c1f-9701-4e167c999eb4
```

### Check User Exists (Azure CLI)
```powershell
az ad user show --id zhangzhijiang@idatagear.com
```

### Grant Admin Consent (Azure CLI)
```powershell
az ad app permission admin-consent --id a8445943-d967-4c1f-9701-4e167c999eb4
```

## Still Getting 404?

1. **Check Application Logs:**
   - Look for detailed error messages
   - Check the exact URL being called
   - Verify user ID format

2. **Try Object ID Instead:**
   - Sometimes Object ID works when email doesn't
   - Get it from: Azure AD → Users → [User] → Object ID

3. **Verify Application Has Access:**
   - Application permissions allow access to ALL users in tenant
   - But user must exist and be active

4. **Check Tenant ID:**
   - Ensure user belongs to the tenant specified in `application.yml`

## Support

If issues persist:
- Review application logs for detailed error messages
- Check Azure AD audit logs for permission issues
- Verify all configuration values in `application.yml`

