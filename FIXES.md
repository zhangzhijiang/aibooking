# Known Issues and Fixes

## Resolved Issues

### pom.xml Name Tag Issue

**Issue:** Line 18 in `pom.xml` had `<n>` instead of `<name>`.

**Status:** ✅ Fixed - This has been corrected in the current version.

**Impact:** This was a cosmetic issue and didn't affect functionality. Maven worked fine, but the project name wasn't properly set.

## Current Status

1. ✅ All dependencies are correctly configured
2. ✅ Test dependencies (mockwebserver) are included
3. ✅ All code compiles without errors
4. ✅ All tests are properly structured
5. ✅ CLU service removed (replaced with OpenAI)
6. ✅ KeyVault dependency removed (using application.yml)
7. ✅ All documentation updated

## Configuration Notes

### application.yml

All configuration is now centralized in `application.yml`. No external services like Key Vault are required for basic operation.

**Required Configuration:**
- Azure AD credentials (tenant-id, client-id, client-secret)
- OpenAI API key

**Optional Configuration:**
- OpenAI model selection (defaults to gpt-4o-mini)
- Application Insights (for monitoring)

### Environment Variables

For production deployments, consider using environment variables instead of hardcoding values in `application.yml`:

```bash
AZURE_TENANT_ID=your-tenant-id
AZURE_CLIENT_ID=your-client-id
AZURE_CLIENT_SECRET=your-client-secret
OPENAI_API_KEY=your-openai-key
```

## Migration Notes

### From CLU to OpenAI

The application has been migrated from Azure CLU to OpenAI API:

- ✅ `CluService` removed
- ✅ `CluIntent` DTO removed
- ✅ All tests updated to use `OpenAIService`
- ✅ Configuration simplified (no CLU endpoint/key needed)

### From KeyVault to application.yml

Configuration management simplified:

- ✅ `KeyVaultService` removed
- ✅ All secrets now in `application.yml`
- ✅ No Azure Key Vault dependency required

## Troubleshooting

### Common Issues

1. **OpenAI API 401 Error**
   - Verify API key is correct
   - Check that payment method is added
   - Ensure API key hasn't expired

2. **Graph API Authentication Error**
   - Verify Azure AD credentials
   - Check admin consent is granted
   - Ensure client secret hasn't expired

3. **Intent Not Recognized**
   - Check application logs for OpenAI response
   - Try rephrasing the request
   - Verify OpenAI API is working

## Future Improvements

Potential enhancements for future versions:

1. **Key Vault Integration** - Add Azure Key Vault for production secret management
2. **Caching** - Add response caching for OpenAI API
3. **Retry Logic** - Add automatic retry for transient failures
4. **Rate Limiting** - Add client-side rate limiting
5. **Validation** - Enhanced input validation
6. **Multi-language Support** - Support multiple languages

## Testing

All test scenarios are passing:

- ✅ Simple booking
- ✅ Recurring events
- ✅ Cancellation
- ✅ Rescheduling
- ✅ Integration tests

Run tests with:
```cmd
mvn test
```
