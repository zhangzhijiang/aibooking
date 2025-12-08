# Azure LUIS Configuration Guide

This document provides detailed instructions for configuring Azure LUIS for the Speech Calendar Assistant.

## Overview

LUIS (Language Understanding Intelligent Service) is used to extract intents and entities from natural language speech input. The assistant supports three main intents:

1. **BookMeeting** - Create new calendar events
2. **CancelMeeting** - Delete existing calendar events
3. **RescheduleMeeting** - Update existing calendar events

## Step 1: Create LUIS Application

1. Go to [LUIS Portal](https://www.luis.ai)
2. Sign in with your Azure account
3. Click **Create new app**
4. Enter:
   - **Name**: `SpeechCalendarAssistant`
   - **Culture**: `English (en-us)`
   - **Description**: `Speech-driven calendar booking assistant`
5. Click **Done**

## Step 2: Create Intents

### BookMeeting Intent

1. Go to **Intents** > **Add**
2. Name: `BookMeeting`
3. Click **Done**
4. Add example utterances:
   - `Book Mary for 2 PM tomorrow`
   - `Schedule a meeting with John at 3 PM next Friday`
   - `Create an appointment with Sarah on January 20 at 10 AM`
   - `Book myTeam 3 PM to 4 PM every weekday`
   - `Schedule team sync 9 AM every weekday from January to July`
   - `Set up a meeting with the marketing team tomorrow at 2 PM`

### CancelMeeting Intent

1. Go to **Intents** > **Add**
2. Name: `CancelMeeting`
3. Click **Done**
4. Add example utterances:
   - `Cancel my meeting with Alex on Jan 15 at 10 AM`
   - `Delete the appointment with Sarah tomorrow`
   - `Remove the meeting scheduled for next Friday`
   - `Cancel the team meeting on January 20`

### RescheduleMeeting Intent

1. Go to **Intents** > **Add**
2. Name: `RescheduleMeeting`
3. Click **Done**
4. Add example utterances:
   - `Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday`
   - `Move the appointment with John to tomorrow at 4 PM`
   - `Change the meeting time from 10 AM to 2 PM`
   - `Update the team sync to next Monday at 9 AM`

## Step 3: Create Entities

### personName (Simple Entity)

1. Go to **Entities** > **Add**
2. Select **Simple**
3. Name: `personName`
4. Click **Done**
5. Label in example utterances:
   - In "Book **Mary** for 2 PM", label "Mary" as `personName`
   - In "Cancel meeting with **Alex**", label "Alex" as `personName`
   - In "Schedule with **myTeam**", label "myTeam" as `personName`

### datetime (Prebuilt Entity)

1. Go to **Entities** > **Add prebuilt entity**
2. Select **datetimeV2**
3. Click **Done**
4. This entity will automatically recognize dates and times in utterances

### recurrence (Simple Entity)

1. Go to **Entities** > **Add**
2. Select **Simple**
3. Name: `recurrence`
4. Click **Done**
5. Label in example utterances:
   - In "every **weekday**", label "weekday" as `recurrence`
   - In "**daily** at 9 AM", label "daily" as `recurrence`
   - In "**every Monday**", label "every Monday" as `recurrence`
   - In "**weekly** on Fridays", label "weekly" as `recurrence`

### exception (Simple Entity)

1. Go to **Entities** > **Add**
2. Select **Simple**
3. Name: `exception`
4. Click **Done**
5. Label in example utterances:
   - In "except **every second Tuesday**", label "every second Tuesday" as `exception`
   - In "skip **Monday in the first week**", label "Monday in the first week" as `exception`
   - In "but not **Tuesday/Thursday in the second week**", label "Tuesday/Thursday in the second week" as `exception`

### subject (Simple Entity)

1. Go to **Entities** > **Add**
2. Select **Simple**
3. Name: `subject`
4. Click **Done**
5. Label in example utterances:
   - In "Schedule **team sync** 9 AM", label "team sync" as `subject`
   - In "Book **standup meeting**", label "standup meeting" as `subject`

### location (Simple Entity)

1. Go to **Entities** > **Add**
2. Select **Simple**
3. Name: `location`
4. Click **Done**
5. Label in example utterances:
   - In "meeting in **Conference Room A**", label "Conference Room A" as `location`
   - In "at **Microsoft Building 1**", label "Microsoft Building 1" as `location`

## Step 4: Train the Model

1. Go to **Train** in the top menu
2. Click **Train**
3. Wait for training to complete (usually 1-2 minutes)
4. Review any warnings or errors

## Step 5: Test the Model

1. Go to **Test** in the top menu
2. Enter test utterances:
   - `Book Mary for 2 PM tomorrow`
   - `Cancel my meeting with Alex on Jan 15 at 10 AM`
   - `Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday`
3. Verify that:
   - Correct intent is predicted
   - Entities are extracted correctly
   - Confidence scores are above 0.7

## Step 6: Publish the Application

1. Go to **Publish** in the top menu
2. Select **Production** slot
3. Click **Publish**
4. Note the endpoint URL (you'll need the App ID and Key)

## Step 7: Get Application Credentials

1. Go to **Manage** in the top menu
2. Note the **Application ID**
3. Go to **Azure Resources**
4. Create a new resource or link existing:
   - **Resource name**: `SpeechCalendarAssistant-LUIS`
   - **Resource group**: Your resource group
   - **Pricing tier**: F0 (Free) or S0 (Standard)
5. Note the **Primary Key** and **Region**

## Step 8: Configure in Application

Update your `application.properties` or environment variables:

```properties
azure.luis.app-id=<your-app-id>
azure.luis.key=<your-primary-key>
azure.luis.region=<your-region>
azure.luis.endpoint=https://<your-region>.api.cognitive.microsoft.com
```

## Example LUIS Response

For the utterance: "Book Mary for 2 PM tomorrow"

```json
{
  "query": "Book Mary for 2 PM tomorrow",
  "prediction": {
    "topIntent": "BookMeeting",
    "intents": {
      "BookMeeting": {
        "score": 0.95
      }
    },
    "entities": {
      "personName": [
        {
          "text": "Mary",
          "category": "personName",
          "offset": 5,
          "length": 4,
          "score": 0.9
        }
      ],
      "datetimeV2": [
        {
          "text": "2 PM tomorrow",
          "category": "datetimeV2",
          "offset": 14,
          "length": 13,
          "score": 0.95,
          "resolution": {
            "values": [
              {
                "timex": "2024-01-16T14:00",
                "type": "datetime",
                "value": "2024-01-16 14:00:00"
              }
            ]
          }
        }
      ]
    }
  }
}
```

## Best Practices

1. **Add diverse examples**: Include variations in phrasing, word order, and vocabulary
2. **Label consistently**: Ensure entities are labeled the same way across all utterances
3. **Review predictions**: Regularly test and refine based on real-world usage
4. **Handle edge cases**: Add examples for common errors or ambiguous inputs
5. **Monitor performance**: Use LUIS analytics to identify improvement opportunities

## Troubleshooting

### Low Intent Confidence

- Add more example utterances
- Ensure examples are diverse
- Check for spelling errors in examples

### Missing Entities

- Verify entities are properly labeled in training data
- Add more examples with the entity
- Check entity definitions match the use case

### Incorrect Intent Prediction

- Review similar intents for overlap
- Add more distinguishing examples
- Consider using phrase lists for domain-specific terms

## Advanced Configuration

### Phrase Lists

Create phrase lists for common terms:
- **Attendee names**: Add common names or team names
- **Meeting types**: "standup", "sync", "review", "planning"
- **Time expressions**: "tomorrow", "next week", "this Friday"

### Patterns

Use patterns for common structures:
- `Book {personName} for {datetime}`
- `Cancel meeting with {personName} on {datetime}`
- `Schedule {subject} {recurrence}`

## Next Steps

After configuring LUIS:
1. Test with real speech input
2. Monitor LUIS analytics for accuracy
3. Continuously improve with additional training data
4. Consider active learning for automatic improvement

