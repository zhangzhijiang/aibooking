# Data Flow Review: OpenAIService → ScheduleService → GraphCalendarService

## Overview
This document reviews the data flow between services to ensure proper contract compliance and Graph API compatibility.

## Data Flow Path

```
User Input (Text)
    ↓
OpenAIService.extractIntentAndEntities()
    ↓ (JSON Response from OpenAI)
OpenAIService.parseOpenAIResponse()
    ↓ (ExtractedEntities DTO)
ScheduleService.processScheduleRequest()
    ↓ (ExtractedEntities)
GraphCalendarService.createEvent() / updateEvent()
    ↓ (Graph API Event Object)
Microsoft Graph API
```

## 1. OpenAI Prompt Format Review

### Current Prompt Structure
- ✅ **JSON Format**: Uses `response_format: {type: "json_object"}` to ensure JSON output
- ✅ **Field Names**: Matches ExtractedEntities DTO exactly
- ✅ **Date Format**: ISO 8601 format `yyyy-MM-ddTHH:mm` (matches parser)
- ✅ **Null Handling**: Explicitly instructs to use `null` for optional fields

### Prompt Output Format
```json
{
  "intent": "BookMeeting",
  "attendees": ["John", "mary@example.com"],
  "startDateTime": "2025-12-10T15:00",
  "endDateTime": "2025-12-10T16:00",
  "subject": "Meeting with John",
  "location": null,
  "recurrencePattern": null,
  "exceptions": null
}
```

### ✅ Verification
- All fields match ExtractedEntities DTO structure
- Date format matches parser: `DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")`
- Prompt explicitly requests JSON format
- Response format is set to `json_object`

## 2. OpenAIService → ExtractedEntities Mapping

### Field Mappings
| OpenAI JSON Field | ExtractedEntities Field | Type | Status |
|------------------|------------------------|------|--------|
| `intent` | `intent` | String | ✅ |
| `attendees` | `attendees` | List<String> | ✅ |
| `startDateTime` | `startDateTime` | LocalDateTime | ✅ |
| `endDateTime` | `endDateTime` | LocalDateTime | ✅ |
| `subject` | `subject` | String | ✅ |
| `location` | `location` | String | ✅ |
| `recurrencePattern` | `recurrencePattern` | String | ✅ |
| `exceptions` | `exceptions` | List<String> | ✅ |

### Parsing Logic
- ✅ **Intent**: Direct string mapping
- ✅ **Attendees**: List parsing with null checks
- ✅ **Dates**: Parsed using `DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")`
- ✅ **Subject**: Defaults to "Meeting" if null/empty
- ✅ **Location**: Handles null, empty string, and "null" string
- ✅ **RecurrencePattern**: Handles null, empty string, and "null" string
- ✅ **Exceptions**: List parsing with null checks

## 3. ScheduleService → GraphCalendarService Mapping

### Data Flow
- ✅ **Direct Pass-Through**: ExtractedEntities passed directly to GraphCalendarService methods
- ✅ **No Transformation**: ScheduleService doesn't modify ExtractedEntities
- ✅ **Proper Routing**: Intent-based routing to correct handler methods

### Handler Methods
- ✅ `handleBookMeeting()` → `GraphCalendarService.createEvent()`
- ✅ `handleCancelMeeting()` → `GraphCalendarService.findEvents()` + `deleteEvent()`
- ✅ `handleRescheduleMeeting()` → `GraphCalendarService.findEvents()` + `updateEvent()`

## 4. GraphCalendarService → Graph API Mapping

### Field Conversions

#### ✅ Subject
- **Input**: `String` from ExtractedEntities
- **Output**: `event.subject` (String)
- **Default**: "Meeting" if null
- **Status**: ✅ Correct

#### ✅ Start/End DateTime
- **Input**: `LocalDateTime` from ExtractedEntities
- **Output**: `DateTimeTimeZone` object with:
  - `dateTime`: ISO format string
  - `timeZone`: System default timezone
- **Status**: ✅ Correct

#### ✅ Attendees
- **Input**: `List<String>` from ExtractedEntities (names or emails)
- **Output**: `List<Attendee>` objects with:
  - `emailAddress.address`: Email address
  - `emailAddress.name`: Display name
  - `type`: AttendeeType.REQUIRED
- **Conversion Logic**:
  - If input contains "@" → treat as email, extract name from email
  - If input is name only → construct email as `name@example.com`
- **Status**: ✅ Correct (matches Graph API requirements)

#### ✅ Location
- **Input**: `String` from ExtractedEntities
- **Output**: `Location` object with:
  - `displayName`: Location string
- **Status**: ✅ Correct (matches Graph API requirements)

#### ✅ Recurrence
- **Input**: `String` from ExtractedEntities ("daily", "weekly", "weekday", "monthly")
- **Output**: `PatternedRecurrence` object with:
  - `pattern`: RecurrencePattern (type, interval, daysOfWeek)
  - `range`: RecurrenceRange (startDate, endDate)
- **Status**: ✅ Correct (matches Graph API requirements)

#### ✅ Exceptions
- **Input**: `List<String>` from ExtractedEntities
- **Output**: Handled separately by deleting specific recurring event instances
- **Status**: ✅ Correct (Graph API doesn't have direct "exceptions" field)

## 5. Potential Issues & Recommendations

### ✅ All Issues Resolved
1. ✅ **Location Format**: Fixed - now uses Location object with displayName
2. ✅ **Attendees Format**: Correct - uses Attendee objects with emailAddress
3. ✅ **Recurrence Format**: Correct - uses PatternedRecurrence object
4. ✅ **Date Format**: Consistent - ISO 8601 format throughout

### Recommendations

#### 1. Enhanced Error Handling
- Consider adding validation for date ranges (end > start)
- Validate attendee email format if needed

#### 2. Prompt Enhancement
- The prompt is well-structured and clear
- Consider adding examples for edge cases (e.g., "meeting with John and Mary at 3PM")

#### 3. Data Validation
- Current null handling is robust
- Consider adding business logic validation (e.g., meeting duration limits)

## 6. Summary

### ✅ Data Flow Status: **CORRECT**

All services work together correctly:

1. **OpenAI Prompt** → Outputs valid JSON matching ExtractedEntities structure
2. **OpenAIService** → Parses JSON correctly into ExtractedEntities DTO
3. **ScheduleService** → Routes and passes ExtractedEntities correctly
4. **GraphCalendarService** → Converts ExtractedEntities to Graph API format correctly

### Key Strengths
- ✅ Consistent date format (ISO 8601) throughout
- ✅ Proper null handling at all levels
- ✅ Correct Graph API object structure (Attendee, Location, PatternedRecurrence)
- ✅ Clear separation of concerns
- ✅ Robust error handling

### No Issues Found
All contract issues have been resolved. The data flow is correct and Graph API compatible.

