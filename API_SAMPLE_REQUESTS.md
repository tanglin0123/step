# API Gateway Sample Requests

This document provides sample requests to test your deployed Step Functions API.

- Processing Lambda: Python (`LinTangPythonLambda/index.py`, runtime `python3.11`)
- Trigger and Check Lambdas: Java (`LinTangJavaLambda/target/function.jar`, runtime `java17`)

## API Endpoint

After deploying with `cdk deploy`, you'll receive an API Gateway endpoint URL. It will look similar to:
```
https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger
```

The endpoint is also exported as a CloudFormation output: `StepFunctionApiEndpoint`

## Request Format

### Content-Type
All requests must include the `Content-Type: application/json` header.

### HTTP Method
Use `POST` to trigger the state machine via the Java Lambda.

### Request Body
The request body must be valid JSON with the following fields:
- **processType**: `"parallel"`, `"loop"`, or `"whole"` (determines execution mode) — required
- **items**: An array of non-empty strings to process — optional if `item` provided
- **item**: A single non-empty string — optional convenience when not sending `items`

---

## Sample Request 1: Parallel Processing

Process multiple items concurrently (maxConcurrency = items.length):

```json
{
  "processType": "parallel",
  "items": ["item1", "item2", "item3", "item4"]
}
```

### Using curl (Trigger via Java Lambda):
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "processType": "parallel",
    "items": ["item1", "item2", "item3", "item4"]
  }'
```

### Expected Response (from Java Trigger Lambda):
```json
{
  "message": "State Machine execution started successfully",
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Note:** The `executionId` is automatically generated as a UUID by the system.

---

## Sample Request 2: Loop Processing

Process items sequentially one at a time (maxConcurrency = 1):

```json
{
  "processType": "loop",
  "items": ["order-123", "order-456", "order-789"]
}
```

### Using curl:
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "processType": "loop",
    "items": ["order-123", "order-456", "order-789"]
  }'
```

### Expected Response:
```json
{
  "message": "State Machine execution started successfully",
  "executionId": "execution-b2c3d4e5-f6a7-8901-bcde-f12345678901"
}
```

---

## Sample Request 3: Whole Processing

Process entire payload as a single item (no Map iteration):

```json
{
  "processType": "whole",
  "items": ["single-batch"]
}
```

### Using curl:
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "processType": "whole",
    "items": ["single-batch-1", "single-batch-2"]
  }'

### Single-item convenience (Whole processing)
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "processType": "whole",
    "item": "single-item"
  }'
```
```

---

## Error Responses

### Missing processType (HTTP 400):
```json
{
  "message": "processType is required (parallel|loop|whole)"
}
```

### Invalid processType (HTTP 400):
```json
{
  "message": "processType must be one of: parallel, loop, whole"
}
```

### Missing items and item (HTTP 400):
```json
{
  "message": "Provide either 'items' (array of strings) or 'item' (single string)"
}
```

### Items with empty strings (HTTP 400):
```json
{
  "message": "items must contain non-empty strings"
}

---

## Using Postman

### Step 1: Create a new POST request
- Method: `POST`
- URL: `https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger`

### Step 2: Set Headers
- Key: `Content-Type`
- Value: `application/json`

### Step 3: Set Body
- Select `raw` and `JSON`
- Paste one of the sample requests above

### Step 4: Send
Click "Send" to execute the request

---

## Using Python

### Basic Python Script (Loop)

```python
import json
import requests

API_ENDPOINT = "https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger"

payload = {
  "processType": "loop",
  "items": ["task-1", "task-2", "task-3"]
}

response = requests.post(
  API_ENDPOINT,
  json=payload,
  headers={"Content-Type": "application/json"}
)

print("Status Code:", response.status_code)
print("Response:", json.dumps(response.json(), indent=2))
```

---

## Using JavaScript/Node.js

### Basic JavaScript Script

```javascript
const apiEndpoint = "https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger";

const payload = {
  processType: "parallel",
  items: ["item1", "item2", "item3"]
};

fetch(apiEndpoint, {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify(payload)
})
.then(response => response.json())
.then(data => {
  console.log("Response:", JSON.stringify(data, null, 2));
})
.catch(error => console.error("Error:", error));
```
```
### Error Response: Missing Body (HTTP 400)

```json
{
  "message": "Request body is required"
}
```

### Error Response: Invalid processType (HTTP 400)

```json
{
  "message": "processType must be one of: parallel, loop, whole"
}
```

### Error Response: Invalid items (HTTP 400)

```json
{
  "message": "items must be a non-empty array of strings"
}
```

### Error Response: Lambda Failure (HTTP 500)

```json
{
  "message": "Failed to start state machine execution",
  "error": "Error message details"
}
```
```

---

## Checking Execution Status (Java Lambda)

After triggering a state machine execution, you can check its status and retrieve the results using the `/check` endpoint.

### Endpoint

```
GET https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/check?executionId={executionId}
```

### Query Parameters

- `executionId` (required): The execution ID returned from the trigger response

### Response Format

```json
{
  "executionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SUCCEEDED",
  "output": {
    "parallelResults": [
      { "item": "item1", "status": "processed" },
      { "item": "item2", "status": "processed" }
    ],
    "loopResults": [
      { "item": "order-123", "status": "processed" }
    ],
    "finalResult": {
      "message": "State machine execution completed",
      "timestamp": "..."
    }
  },
  "startDate": "2025-12-13T10:30:00.000Z",
  "stopDate": "2025-12-13T10:30:10.000Z",
  "cause": null,
  "error": null
}
```

---

## Sample Request 5: Check Execution Status

### Using curl (Check via Java Lambda):
```bash
curl -X GET "https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/check?executionId=a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "Content-Type: application/json"
```

### Using JavaScript/Node.js:
```javascript
const executionId = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
const apiUrl = 'https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod';

fetch(`${apiUrl}/check?executionId=${executionId}`)
  .then(response => response.json())
  .then(data => {
    console.log('Execution Status:', data.status);
    console.log('Output:', data.output);
    console.log('Start Date:', data.startDate);
    console.log('Stop Date:', data.stopDate);
  })
  .catch(error => console.error('Error:', error));
```

### Using Python:
```python
import requests
import json

execution_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'
api_url = 'https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod'

response = requests.get(
    f'{api_url}/check',
    params={'executionId': execution_id},
#### Success - Execution Completed (HTTP 200):
```json
{
  "executionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SUCCEEDED",
  "output": {
    "parallelResults": [
      {
        "originalInput": {"item": "item1"},
        "item": "item1",
        "customData": {},
        "processedAt": "2025-12-15T10:30:00.000Z",
        "status": "processed",
        "responseData": {
          "greeting": "Processed item: item1 at 2025-12-15T10:30:00.000Z",
          "inputFieldCount": 1,
          "receivedFields": ["item"]
        }
      },
      {
        "originalInput": {"item": "item2"},
        "item": "item2",
        "customData": {},
        "processedAt": "2025-12-15T10:30:00.100Z",
        "status": "processed",
        "responseData": {
          "greeting": "Processed item: item2 at 2025-12-15T10:30:00.100Z",
          "inputFieldCount": 1,
          "receivedFields": ["item"]
        }
      }
    ],
    "finalResult": {
      "message": "Parallel execution completed",
      "timestamp": "2025-12-15T10:30:05.000Z"
    }
  },
  "startDate": "2025-12-15T10:30:00.000Z",
  "stopDate": "2025-12-15T10:30:10.000Z",
  "cause": null,
  "error": null
}
``` "status": "processed",
    "responseData": {
      "greeting": "Hello World - Processed at 2025-12-13T10:30:00.000Z",
      "inputFieldCount": 1,
      "receivedFields": ["message"]
    },
    "finalResult": {
      "message": "State machine execution completed",
      "timestamp": "2025-12-13T10:30:05.000Z"
    }
  },
  "startDate": "2025-12-13T10:30:00.000Z",
  "stopDate": "2025-12-13T10:30:10.000Z",
  "cause": null,
  "error": null
}
```

#### Still Running - Execution In Progress (HTTP 200):
```json
{
  "executionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "RUNNING",
  "output": null,
  "startDate": "2025-12-13T10:30:00.000Z",
  "stopDate": null,
  "cause": null,
  "error": null
}
```

#### Failed Execution (HTTP 200):
```json
{
  "executionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "FAILED",
  "output": null,
  "startDate": "2025-12-13T10:30:00.000Z",
  "stopDate": "2025-12-13T10:30:05.000Z",
  "cause": "An error occurred during execution",
  "error": "States.TaskFailed"
}
```

#### Not Found - Invalid Execution ID (HTTP 404):
```json
{
  "message": "Execution not found",
  "error": "Execution does not exist",
  "executionId": "invalid-id"
}
```

#### Missing Parameter (HTTP 400):
```json
{
  "message": "Execution ID is required",
  "error": "Missing executionId parameter"
}
```

---

## Execution Status Values

- **RUNNING**: The execution is currently in progress
- **SUCCEEDED**: The execution completed successfully
- **FAILED**: The execution failed with an error
- **TIMED_OUT**: The execution exceeded the timeout (5 minutes)
- **ABORTED**: The execution was manually aborted
## Lambda Output Structure

When the processing Lambda completes, the state machine receives output with this structure:

```json
{
  "originalInput": {
    "item": "item1"
  },
  "item": "item1",
  "customData": {},
  "processedAt": "2025-12-15T10:30:00.000Z",
  "status": "processed",
  "responseData": {
    "greeting": "Processed item: item1 at 2025-12-15T10:30:00.000Z",
    "inputFieldCount": 1,
    "receivedFields": ["item"]
  }
}
```

## Processing Modes

### Parallel Processing
- **processType**: `"parallel"`
- **Behavior**: All items processed concurrently
- **maxConcurrency**: Set to the number of items
- **Use Case**: Fast processing of independent items
- **Output Path**: `$.parallelResults`

### Loop Processing
- **processType**: `"loop"`
- **Behavior**: Items processed sequentially one at a time
- **maxConcurrency**: 1
- **Use Case**: Order-dependent processing or rate-limited operations
- **Output Path**: `$.loopResults`

### Whole Processing
- **processType**: `"whole"`
- **Behavior**: Single Lambda invocation with entire payload
- **Use Case**: Single-item or batch processing as one unit
- **Output Path**: Direct Lambda output-state-machine-arn arn:aws:states:us-west-2:685915392751:stateMachine:ProcessAndReportJob \
  --region us-west-2

# Get execution details
aws stepfunctions describe-execution \
  --execution-arn "arn:aws:states:us-west-2:685915392751:execution:ProcessAndReportJob:my-execution" \
  --region us-west-2

# Get execution history
aws stepfunctions get-execution-history \
  --execution-arn "arn:aws:states:us-west-2:685915392751:execution:ProcessAndReportJob:my-execution" \
  --region us-west-2
```

---

## Lambda Output Structure

When the processing Lambda completes, the state machine receives output with this structure:

```json
{
  "originalInput": {
    "message": "Hello World",
    "customData": {}
  },
  "message": "Hello World",
  "customData": {},
  "processedAt": "2025-12-13T10:30:00.000Z",
  "status": "processed",
  "responseData": {
    "greeting": "Hello World - Processed at 2025-12-13T10:30:00.000Z",
    "inputFieldCount": 2,
    "receivedFields": ["message", "customData"]
  }
}
```

---

## Notes

- The state machine will wait 5 seconds after Lambda processing before completing
- Executions timeout after 5 minutes
- All timestamps are in ISO 8601 format
- Custom data is preserved and passed through the entire workflow
- Each execution is assigned a unique UUID automatically (format: `execution-{uuid}`)
- The execution name is returned in the API response for tracking purposes
