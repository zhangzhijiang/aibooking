# Azure CLU (Conversational Language Understanding) Configuration Guide

This document provides detailed instructions for configuring Azure CLU for the Schedule Hub.

## Overview

CLU (Conversational Language Understanding) is used to extract intents and entities from natural language speech input. The assistant supports three main intents:

1. **BookMeeting** - Create new calendar events
2. **CancelMeeting** - Delete existing calendar events
3. **RescheduleMeeting** - Update existing calendar events

## Prerequisites

- An Azure subscription
- Access to [Language Studio](https://language.cognitive.azure.com/)

## Step 1: Create Azure Language Resource

1. Go to [Azure Portal](https://portal.azure.com/)
2. Search for "Language service" or "Cognitive Services"
3. Click **Create**
4. Fill in the details:
   - **Subscription**: Your subscription
   - **Resource Group**: Create new or use existing
   - **Region**: Choose a region (e.g., East US, West US 2)
   - **Name**: `ScheduleHub-CLU` (or your preferred name)
   - **Pricing Tier**: Choose Free (F0) for testing or Standard (S) for production
5. Click **Review + create**, then **Create**
6. Once deployed, go to the resource and note:
   - **Endpoint**: `https://<resource-name>.cognitiveservices.azure.com`
   - **Keys**: Go to **Keys and Endpoint** and copy **Key 1**

## Step 2: Create CLU Project in Language Studio

1. Go to [Language Studio](https://language.cognitive.azure.com/)
2. Sign in with your Azure account
3. Select your Language resource
4. Click **Conversational language understanding** in the left menu
5. Click **Create new project**
6. Enter project details:
   - **Name**: `ScheduleHub`
   - **Description**: `Speech-driven calendar Schedule assistant`
   - **Language**: `English`
7. Click **Create project**

## Step 3: Create Intents

### BookMeeting Intent

1. Go to **Intents** > **Add intent**
2. Name: `BookMeeting`
3. Click **Add**
4. Add example utterances:
   - `Book Mary for 2 PM tomorrow`
   - `Schedule a meeting with John at 3 PM next Friday`
   - `Create an appointment with Sarah on January 20 at 10 AM`
   - `Book myTeam 3 PM to 4 PM every weekday`
   - `Schedule team sync 9 AM every weekday from January to July`
   - `Set up a meeting with the marketing team tomorrow at 2 PM`

### CancelMeeting Intent

1. Go to **Intents** > **Add intent**
2. Name: `CancelMeeting`
3. Click **Add**
4. Add example utterances:
   - `Cancel my meeting with Alex on Jan 15 at 10 AM`
   - `Delete the appointment with Sarah tomorrow`
   - `Remove the meeting scheduled for next Friday`
   - `Cancel the team meeting on January 20`

### RescheduleMeeting Intent

1. Go to **Intents** > **Add intent**
2. Name: `RescheduleMeeting`
3. Click **Add**
4. Add example utterances:
   - `Reschedule my meeting with Sarah from 2 PM to 3 PM next Friday`
   - `Move the appointment with John to tomorrow at 4 PM`
   - `Change the meeting time from 10 AM to 2 PM`
   - `Update the team sync to next Monday at 9 AM`

## Step 4: Create Entities

### PersonName Entity

1. Go to **Entities** > **Add entity**
2. Select **Learned** entity type
3. Name: `PersonName`
4. Click **Add**
5. Label entities in example utterances:
   - In "Book **Mary** for 2 PM", label "Mary" as `PersonName`
   - In "Cancel meeting with **Alex**", label "Alex" as `PersonName`
   - In "Schedule with **myTeam**", label "myTeam" as `PersonName`

### DateTime Entity

1. Go to **Entities** > **Add entity**
2. Select **Prebuilt** entity type
3. Choose **DateTime** from the list
4. Click **Add**
5. This entity will automatically recognize dates and times in utterances

### Recurrence Entity

1. Go to **Entities** > **Add entity**
2. Select **Learned** entity type
3. Name: `Recurrence`
4. Click **Add**
5. Label in example utterances:
   - In "every **weekday**", label "weekday" as `Recurrence`
   - In "**daily** at 9 AM", label "daily" as `Recurrence`
   - In "**every Monday**", label "every Monday" as `Recurrence`
   - In "**weekly** on Fridays", label "weekly" as `Recurrence`

### Exception Entity

1. Go to **Entities** > **Add entity**
2. Select **Learned** entity type
3. Name: `Exception`
4. Click **Add**
5. Label in example utterances:
   - In "except **every second Tuesday**", label "every second Tuesday" as `Exception`
   - In "skip **Monday in the first week**", label "Monday in the first week" as `Exception`

### Subject/Title Entity (Optional)

1. Go to **Entities** > **Add entity**
2. Select **Learned** entity type
3. Name: `Subject`
4. Click **Add**
5. Label meeting subjects in utterances when mentioned

### Location Entity (Optional)

1. Go to **Entities** > **Add entity**
2. Select **Learned** entity type
3. Name: `Location`
4. Click **Add**
5. Label locations in utterances

## Step 5: Train the Model

1. Click **Train** in the top menu
2. Select **Train a new model**
3. Wait for training to complete (usually a few minutes)
4. Review any training warnings or errors

## Step 6: Deploy the Model

1. Go to **Deployments** > **Add deployment**
2. Enter deployment name: `production` (or your preferred name)
3. Select your trained model
4. Click **Deploy**
5. Wait for deployment to complete

## Step 7: Test Your Model

1. Go to **Test model** in the left menu
2. Enter a test utterance like "Book Mary for 2 PM tomorrow"
3. Review the predicted intent and extracted entities
4. Test with various utterances to ensure accuracy

## Step 8: Configure Application

Update your `application.yml` configuration file with the following CLU settings:

```yaml
azure:
  clu:
    endpoint: https://<your-resource-name>.cognitiveservices.azure.com
    key: <your-key>
    project-name: ScheduleHub
    deployment-name: production
    api-version: 2022-05-01
```

Replace:

- `<your-resource-name>` - Your Language Service resource name
- `<your-key>` - Your Language Service key (Key 1)

## Example CLU Response

```json
{
  "kind": "ConversationResult",
  "result": {
    "query": "Book Mary for 2 PM tomorrow",
    "prediction": {
      "topIntent": "BookMeeting",
      "intents": {
        "BookMeeting": { "score": 0.95 },
        "CancelMeeting": { "score": 0.02 },
        "RescheduleMeeting": { "score": 0.01 }
      },
      "entities": [
        {
          "category": "PersonName",
          "text": "Mary",
          "offset": 5,
          "length": 4,
          "confidenceScore": 0.98
        },
        {
          "category": "DateTime",
          "text": "2 PM tomorrow",
          "offset": 13,
          "length": 14,
          "confidenceScore": 0.95,
          "extraInformation": {
            "values": [
              {
                "timex": "2024-01-16T14:00",
                "type": "datetime"
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

1. **Training Data**: Provide at least 5-10 example utterances per intent for better accuracy
2. **Entity Labeling**: Consistently label entities in all training examples
3. **Testing**: Regularly test with new utterances to identify gaps
4. **Retraining**: Retrain and redeploy after adding new examples or entities
5. **Monitor performance**: Use Language Studio analytics to identify improvement opportunities

## Troubleshooting

### Low Intent Confidence Scores

- Add more training examples for the intent
- Ensure examples are diverse and cover different phrasings
- Review entity labeling consistency

### Missing Entities

- Verify entities are properly labeled in training examples
- Add more examples containing the entity
- Check that entity names match exactly (case-sensitive)

### API Errors

- Verify endpoint URL format: `https://<resource-name>.cognitiveservices.azure.com`
- Check that API key is correct
- Ensure deployment is active in Language Studio
- Verify project name and deployment name match exactly

## Migration from LUIS

If you're migrating from LUIS:

1. Export your LUIS app (if possible)
2. Create a new CLU project in Language Studio
3. Recreate intents and entities in CLU
4. Import or manually add training examples
5. Train and deploy the CLU model
6. Update application configuration as shown above

## Additional Resources

- [Azure CLU Documentation](https://learn.microsoft.com/azure/ai-services/language-service/conversational-language-understanding/overview)
- [Language Studio](https://language.cognitive.azure.com/)
- [CLU REST API Reference](https://learn.microsoft.com/azure/ai-services/language-service/conversational-language-understanding/how-to/call-api)
