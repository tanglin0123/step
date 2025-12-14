# API Gateway Sample Requests

This document provides sample requests to test your deployed Step Functions API.

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
Use `POST` to trigger the state machine.

### Request Body
The request body should be valid JSON that will be passed to the state machine and processed by the Lambda function.

---

## Sample Request 1: Basic Request (Minimal)

This is the simplest valid request:

```json
{
  "message": "Hello World"
}
```

### Using curl:
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello World"
  }'
```

### Expected Response:
```json
{
  "message": "State Machine execution started successfully",
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Note:** The `executionId` is automatically generated as a UUID by the system.

---

## Sample Request 2: Request with Custom Data

This request includes additional custom data that will be processed:

```json
{
  "message": "Processing customer order",
  "customData": {
    "orderId": "ORD-12345",
    "customerId": "CUST-67890",
    "amount": 150.50,
    "items": ["item1", "item2", "item3"]
  }
}
```

### Using curl:
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Processing customer order",
    "customData": {
      "orderId": "ORD-12345",
      "customerId": "CUST-67890",
      "amount": 150.50,
      "items": ["item1", "item2", "item3"]
    }
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

## Sample Request 3: Data Processing Request

This request demonstrates various data types for processing:

```json
{
  "message": "Processing customer data batch",
  "customData": {
    "batchId": "BATCH-2025-001",
    "timestamp": "2025-12-13T10:30:00Z",
    "records": [
      {
        "id": 1,
        "name": "Alice",
        "email": "alice@example.com",
        "status": "active"
      },
      {
        "id": 2,
        "name": "Bob",
        "email": "bob@example.com",
        "status": "inactive"
      }
    ],
    "totalRecords": 2,
    "metadata": {
      "source": "API",
      "priority": "high"
    }
  }
}
```

### Using curl:
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Processing customer data batch",
    "customData": {
      "batchId": "BATCH-2025-001",
      "timestamp": "2025-12-13T10:30:00Z",
      "records": [
        {
          "id": 1,
          "name": "Alice",
          "email": "alice@example.com",
          "status": "active"
        },
        {
          "id": 2,
          "name": "Bob",
          "email": "bob@example.com",
          "status": "inactive"
        }
      ],
      "totalRecords": 2,
      "metadata": {
        "source": "API",
        "priority": "high"
      }
    }
  }'
```

---

## Sample Request 4: Default Values (Minimal Fields)

If you send just an empty object, the Lambda will use default values:

```json
{}
```

### Using curl:
```bash
curl -X POST https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Expected Response:
The execution will use:
- `executionName`: Auto-generated UUID (execution-{uuid})
- `message`: "Hello from Lambda" (default)
- `customData`: {} (empty)

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

### Basic Python Script

```python
import json
import requests

# Replace with your actual API endpoint
API_ENDPOINT = "https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger"

# Sample request payload
payload = {
    "message": "Triggered from Python",
    "customData": {
        "language": "python",
        "timestamp": "2025-12-13T10:30:00Z"
    }
}

# Make the request
response = requests.post(
    API_ENDPOINT,
    json=payload,
    headers={"Content-Type": "application/json"}
)

# Print the response
print("Status Code:", response.status_code)
print("Response:", json.dumps(response.json(), indent=2))
```

---

## Using JavaScript/Node.js

### Basic JavaScript Script

```javascript
const apiEndpoint = "https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/trigger";

const payload = {
  message: "Triggered from JavaScript",
  customData: {
    language: "javascript",
    timestamp: new Date().toISOString()
  }
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
  console.log("Status:", data.statusCode);
  console.log("Response:", JSON.stringify(data, null, 2));
})
.catch(error => console.error("Error:", error));
```

---

## Data Flow Through the System

### 1. API Gateway receives the request
- Endpoint: `/trigger`
- Method: `POST`
- Body: Your JSON payload

### 2. Trigger Lambda processes the request
- Receives: Event with your JSON in the `body` field
- Validates: Ensures body is not empty
- Parses: Converts string to JSON if needed
- Generates: Creates a unique UUID for the execution name
- Starts: Calls state machine with your JSON as input

### 3. Step Functions State Machine executes
- **ProcessJob**: Invokes the processing Lambda with your input
- **Wait**: Waits 5 seconds
- **FinalState**: Adds completion metadata
- **Succeed**: Marks execution as successful

### 4. Processing Lambda executes
- Input: Your JSON payload from the state machine
- Processing: Extracts fields, creates response object
- Output: Returns processed data with metadata

### 5. State Machine completes
- Captures Lambda output in `$.lambdaResult`
- Adds final state metadata
- Marks execution as successful

---

## Response Structure

### Success Response (HTTP 200)

```json
{
  "message": "State Machine execution started successfully",
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Note:** The `executionId` is a UUID automatically generated by the system for each execution. Use this ID to track your execution in the AWS Step Functions console.

### Error Response: Missing Body (HTTP 400)

```json
{
  "message": "Request body is required",
  "error": "No JSON body provided"
}
```

### Error Response: Lambda Failure (HTTP 500)

```json
{
  "message": "Failed to start state machine execution",
  "error": "Error message details"
}
```

---

## Checking Execution Status

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
    "originalInput": {...},
    "message": "...",
    "customData": {...},
    "processedAt": "...",
    "status": "processed",
    "responseData": {...},
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

### Using curl:
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
    headers={'Content-Type': 'application/json'}
)

if response.status_code == 200:
    data = response.json()
    print(f"Status: {data['status']}")
    print(f"Output: {json.dumps(data['output'], indent=2)}")
    print(f"Started: {data['startDate']}")
    print(f"Completed: {data['stopDate']}")
else:
    print(f"Error: {response.status_code}")
    print(response.text)
```

### Expected Responses:

#### Success - Execution Completed (HTTP 200):
```json
{
  "executionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SUCCEEDED",
  "output": {
    "originalInput": {"message": "Hello World"},
    "message": "Hello World",
    "customData": {},
    "processedAt": "2025-12-13T10:30:00.000Z",
    "status": "processed",
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

## Monitoring the Execution

After triggering the execution, you can monitor it in the AWS Console:

### Option 1: AWS Console
1. Go to Step Functions
2. Find "ProcessAndReportJob" state machine
3. Look for your execution by name
4. View execution details, including:
   - Input
   - Lambda output
   - State machine state
   - Final result

### Option 2: AWS CLI

```bash
# List recent executions
aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-west-2:685915392751:stateMachine:ProcessAndReportJob \
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
