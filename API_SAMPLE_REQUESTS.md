# API Gateway Sample Requests

This document provides sample requests to test your deployed Step Functions API.

## Architecture

- **Processing Lambda**: Python 3.11 (`LinTangPythonLambda/index.py`) — processes items within Step Functions
- **Trigger Lambda**: Java 17 (`TriggerHandler`) — starts state machine executions
- **Check Lambda**: Java 17 (`CheckHandler`) — retrieves execution status and results

## API Endpoint

After deploying with `cdk deploy --all`, you'll receive an API Gateway endpoint URL from the **ApiStack** output:
```
https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/
```

Two endpoints are available:
- **POST** `/trigger` — Start a new execution (via Trigger Lambda)
- **GET** `/check?executionId=...` — Check execution status (via Check Lambda)

## Request Format

### Content-Type
All requests must include the `Content-Type: application/json` header.

### HTTP Method
Use `POST` to trigger the state machine via the Java Lambda.

### Request Body
The request body must be valid JSON with the following fields:
- **processType**: (required) One of `"parallel"`, `"loop"`, or `"whole"`
  - `"whole"`: Single Lambda invocation with entire payload
  - `"loop"`: Sequential iteration (Map with maxConcurrency=1)
  - `"parallel"`: Concurrent iteration (respects maxConcurrency)
- **items**: (optional) Array of non-empty strings to process
- **item**: (optional) Single non-empty string (convenience alternative to **items**)
- **maxConcurrency**: (optional) Max concurrent iterations for parallel/loop modes; defaults to item count for parallel, 1 for loop

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

### Invalid processType (HTTP 400)
If `processType` is not one of `parallel`, `loop`, or `whole`, the state machine immediately fails:
```json
{
  "message": "Invalid processType 'invalid'. Must be one of: parallel, loop, whole"
}
```

### Missing processType (HTTP 400):
```json
{
  "message": "processType is required (parallel|loop|whole)"
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
```

### Processing Failure (HTTP 200 with FAILED status)
When a Lambda raises an error (e.g., item equals `"FAIL!"`), the execution fails:
```json
{
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "FAILED",
  "output": null,
  "startDate": 1766810000000,
  "stopDate": 1766810010000,
  "cause": "An error occurred during item processing",
  "error": "ProcessingError"
}
```

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

After triggering a state machine execution, use the `/check` endpoint to retrieve status and results.

### Endpoint

```
GET https://xxxxxxx.execute-api.us-west-2.amazonaws.com/prod/check?executionId={executionId}
```

### Query Parameters

- `executionId` (required): The execution ID returned from the trigger response

### Unified Response Format

All three processing modes (`whole`, `loop`, `parallel`) return a unified output structure:

```json
{
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SUCCEEDED",
  "output": {
    "originalInput": {
      "processType": "parallel",
      "items": ["item1", "item2", "item3"],
      "maxConcurrency": 3
    },
    "results": [
      {
        "item": "item1",
        "processedItem": "ITEM1",
        "customData": {},
        "processedAt": "2025-12-27T04:36:24.817743+00:00Z",
        "status": "processed",
        "message": "Processed item: item1 at 2025-12-27T04:36:24.817743+00:00Z"
      },
      {
        "item": "item2",
        "processedItem": "ITEM2",
        "customData": {},
        "processedAt": "2025-12-27T04:36:24.749961+00:00Z",
        "status": "processed",
        "message": "Processed item: item2 at 2025-12-27T04:36:24.749961+00:00Z"
      }
    ],
    "count": 3,
    "processedAt": "2025-12-27T04:36:26.107Z",
    "status": "processed"
  },
  "startDate": 1766810183591,
  "stopDate": 1766810186141,
  "cause": null,
  "error": null,
  "events": [...]
}
```

The `events` array contains the full Step Functions execution history.

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
    headers={'Content-Type': 'application/json'}
)
print("Status Code:", response.status_code)
print("Response:", json.dumps(response.json(), indent=2))
```

---

## Check Response Examples

#### Success - Execution Completed (HTTP 200, Unified Output):
```json
{
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "SUCCEEDED",
  "output": {
    "originalInput": {
      "processType": "parallel",
      "items": ["item1", "item2", "item3"],
      "maxConcurrency": 3
    },
    "results": [
      {
        "item": "item1",
        "processedItem": "ITEM1",
        "customData": {},
        "processedAt": "2025-12-27T04:36:24.817743+00:00Z",
        "status": "processed",
        "message": "Processed item: item1 at 2025-12-27T04:36:24.817743+00:00Z"
      },
      {
        "item": "item2",
        "processedItem": "ITEM2",
        "customData": {},
        "processedAt": "2025-12-27T04:36:24.749961+00:00Z",
        "status": "processed",
        "message": "Processed item: item2 at 2025-12-27T04:36:24.749961+00:00Z"
      },
      {
        "item": "item3",
        "processedItem": "ITEM3",
        "customData": {},
        "processedAt": "2025-12-27T04:36:24.778027+00:00Z",
        "status": "processed",
        "message": "Processed item: item3 at 2025-12-27T04:36:24.778027+00:00Z"
      }
    ],
    "count": 3,
    "processedAt": "2025-12-27T04:36:26.107Z",
    "status": "processed"
  },
  "startDate": 1766810183591,
  "stopDate": 1766810186141,
  "cause": null,
  "error": null,
  "events": [...]

#### Still Running - Execution In Progress (HTTP 200):
```json
{
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "RUNNING",
  "output": null,
  "startDate": 1766810000000,
  "stopDate": null,
  "cause": null,
  "error": null
}
```

#### Failed Execution (HTTP 200):
```json
{
  "executionId": "execution-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "FAILED",
  "output": null,
  "startDate": 1766810000000,
  "stopDate": 1766810005000,
  "cause": "An error occurred during item processing",
  "error": "ProcessingError"
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
- **FAILED**: The execution failed with an error (invalid processType, item error, or Lambda exception)
- **TIMED_OUT**: The execution exceeded the timeout (5 minutes)
- **ABORTED**: The execution was manually aborted

---

## Lambda Output Structure

When the processing Lambda completes for a single item, it returns:

```json
{
  "item": "item1",
  "processedItem": "ITEM1",
  "customData": {},
  "processedAt": "2025-12-27T04:36:24.817743+00:00Z",
  "status": "processed",
  "message": "Processed item: item1 at 2025-12-27T04:36:24.817743+00:00Z"
}
```

For `whole` mode: A single dict is returned.  
For `loop`/`parallel` modes: A list of such dicts is returned (one per item).

The Step Functions state machine then aggregates these into the unified output format with `originalInput`, `results`, `count`, `processedAt`, and `status` fields.
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

All modes return a **unified output** with `originalInput`, `results`, `count`, `processedAt`, and `status`.

### Parallel Processing
- **processType**: `"parallel"`
- **Behavior**: All items processed concurrently in a Map state
- **maxConcurrency**: Configurable; defaults to length of items array
- **Use Case**: Fast parallel processing of independent items
- **State Machine Path**: `Choice → WaitBeforeParallel → ParallelProcess (Map) → WaitAfterParallel → FinalState → Succeed`

### Loop Processing
- **processType**: `"loop"`
- **Behavior**: Items processed sequentially one at a time in a Map state
- **maxConcurrency**: Always 1
- **Use Case**: Order-dependent or rate-limited processing
- **State Machine Path**: `Choice → WaitBeforeLoop → LoopProcess (Map) → WaitAfterLoop → FinalState → Succeed`

### Whole Processing
- **processType**: `"whole"`
- **Behavior**: Single Lambda invocation with entire payload
- **maxConcurrency**: N/A (no Map iteration)
- **Use Case**: Single-item or batch processing as one atomic unit
- **State Machine Path**: `Choice → WaitBeforeWhole → ProcessJob (Task) → WaitAfterWhole → FinalState → Succeed`-state-machine-arn arn:aws:states:us-west-2:685915392751:stateMachine:ProcessAndReportJob \
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

- **Output Unification**: All three processing modes return an identical output structure, making it easy to switch modes without changing downstream code
- **Wait States**: The state machine includes 1-second wait states before and after processing for operational visibility
- **Failure Handling**: Invalid `processType` values cause immediate failure; item errors propagate to fail state
- **Timestamps**: All dates are UNIX timestamps (milliseconds since epoch); processedAt timestamps may include timezone info from Lambda
- **maxConcurrency**: Optional parameter; defaults to item array length for parallel, 1 for loop, ignored for whole
- **Execution IDs**: Generated as UUID format `execution-{uuid}` for unique tracking
- **IAM Scoping**: Check Lambda permissions are scoped to execution ARNs of the specific state machine, preventing cross-execution access
- **Custom Data**: Preserved and passed through the entire workflow (visible in output under `originalInput`)
