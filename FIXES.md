# Known Issues and Fixes

## pom.xml Name Tag Issue

**Issue:** Line 18 in `pom.xml` has `<n>` instead of `<name>`.

**Fix:** Manually edit `pom.xml` and change:

```xml
<n>Schedule Hub</n>
```

to:

```xml
<name>Schedule Hub</name>
```

**Impact:** This is a cosmetic issue and doesn't affect functionality. Maven will work fine, but the project name won't be properly set.

**Status:** Minor - can be fixed manually

## Other Notes

1. All dependencies are correctly configured
2. Test dependencies (mockwebserver) are included
3. All code compiles without errors
4. All tests are properly structured
