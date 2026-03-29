# Requirements Document

## Introduction

This document specifies the requirements for the Step Functions Serverless Workflow package — a complete serverless application on AWS that orchestrates item processing via AWS Step Functions, API Gateway, and Lambda functions. The system exposes two REST endpoints: one to trigger a state machine execution and one to check execution status. Processing is delegated to a Python Lambda that supports three modes (whole, loop, parallel). A shared Java library provides formula parsing (ANTLR4-based) and DuckDB query utilities. A separate DuckDB Lambda and Java DataHandler enable ad-hoc SQL queries over Parquet files stored in S3.

## Glossary

- **API_Gateway**: The AWS API Gateway REST API that exposes the `/trigger`, `/check`, and `/data` endpoints.
- **TriggerHandler**: The Java 17 Lambda function that validates incoming requests and starts Step Functions executions.
- **CheckHandler**: The Java 17 Lambda function that retrieves execution status and history from Step Functions.
- **DataHandler**: The Java 17 Lambda function that executes DuckDB SQL queries against S3 Parquet files.
- **ProcessingLambda**: The Python 3.11 Lambda function that processes individual items within the state machine.
- **DuckDbLambda**: The Python 3.11 Lambda function (standalone stack) that executes DuckDB queries against S3 Parquet files.
- **StateMachine**: The AWS Step Functions state machine named `ProcessAndReportJob` that orchestrates item processing.
- **Execution**: A single run of the StateMachine, identified by a UUID-based execution name.
- **processType**: A required request field that controls the processing mode; one of `whole`, `loop`, or `parallel`.
- **items**: An array of non-empty strings to be processed in a single Execution.
- **item**: A convenience field for submitting a single string instead of an `items` array.
- **maxConcurrency**: An optional integer controlling the maximum concurrent Map iterations for `parallel` mode.
- **FormulaParser**: The ANTLR4-based parser in LinTangJavaLib that converts formula strings into an AST.
- **FormulaAST**: The abstract syntax tree produced by the FormulaParser.
- **FormulaStringBuilder**: The visitor that serialises a FormulaAST back into a canonical formula string.
- **DuckDBUtils**: The shared Java utility class that executes DuckDB SQL queries against S3 Parquet files.
- **CDK**: AWS Cloud Development Kit (TypeScript) used to define and deploy all infrastructure.
- **StepStack**: The CDK stack that defines the StateMachine and ProcessingLambda.
- **ApiStack**: The CDK stack that defines the API_Gateway, TriggerHandler, CheckHandler, and DataHandler.
- **DuckDbStack**: The CDK stack that defines the standalone DuckDbLambda.

---

## Requirements

### Requirement 1: Trigger State Machine Execution

**User Story:** As an API consumer, I want to start a Step Functions execution via a POST request, so that I can asynchronously process a list of items.

#### Acceptance Criteria

1. WHEN a POST request is received at `/trigger` with a valid JSON body, THE TriggerHandler SHALL start a new StateMachine Execution and return HTTP 200 with a JSON body containing `message` and `executionId` fields.
2. WHEN the request body contains a `processType` field with value `parallel`, `loop`, or `whole` (case-insensitive), THE TriggerHandler SHALL normalise the value to lowercase before passing it to the StateMachine.
3. WHEN the request body contains an `items` field that is a non-empty array of non-empty strings, THE TriggerHandler SHALL include all items in the StateMachine input payload.
4. WHEN the request body contains an `item` field (single non-empty string) and no `items` field, THE TriggerHandler SHALL treat it as a single-element items array.
5. WHEN the request body contains a `maxConcurrency` field with a positive integer, THE TriggerHandler SHALL pass that value to the StateMachine input; otherwise THE TriggerHandler SHALL default `maxConcurrency` to the length of the items array.
6. THE TriggerHandler SHALL generate a unique execution name in the format `execution-{UUID}` for each Execution.
7. IF the request body is absent or empty, THEN THE TriggerHandler SHALL return HTTP 400 with `{"message": "Request body is required"}`.
8. IF the `processType` field is absent, THEN THE TriggerHandler SHALL return HTTP 400 with `{"message": "processType is required (parallel|loop|whole)"}`.
9. IF the `processType` field is not one of `parallel`, `loop`, or `whole`, THEN THE TriggerHandler SHALL return HTTP 400 with `{"message": "processType must be one of: parallel, loop, whole"}`.
10. IF the `items` field is present but is an empty array, THEN THE TriggerHandler SHALL return HTTP 400 with `{"message": "items must be a non-empty array of strings"}`.
11. IF the `items` array contains any empty or blank string, THEN THE TriggerHandler SHALL return HTTP 400 with `{"message": "items must contain non-empty strings"}`.
12. IF neither `items` nor `item` is provided, THEN THE TriggerHandler SHALL return HTTP 400 with `{"message": "Provide either 'items' (array of strings) or 'item' (single string)"}`.
13. IF the `STATE_MACHINE_ARN` environment variable is absent or empty, THEN THE TriggerHandler SHALL return HTTP 500 with `{"message": "STATE_MACHINE_ARN not configured"}`.
14. IF the Step Functions `StartExecution` call fails, THEN THE TriggerHandler SHALL return HTTP 500 with `{"message": "Failed to start state machine execution", "error": "<error detail>"}`.
15. THE TriggerHandler SHALL include CORS headers (`Access-Control-Allow-Origin: *`) on all responses.

---

### Requirement 2: Check Execution Status

**User Story:** As an API consumer, I want to retrieve the status and output of a running or completed execution, so that I can poll for results without coupling to the processing logic.

#### Acceptance Criteria

1. WHEN a GET request is received at `/check` with a non-empty `executionId` query parameter, THE CheckHandler SHALL call `DescribeExecution` and `GetExecutionHistory` and return HTTP 200 with a JSON body containing `executionId`, `status`, `output`, `startDate`, `stopDate`, `cause`, `error`, and `events` fields.
2. WHEN the Execution status is `SUCCEEDED`, THE CheckHandler SHALL include the parsed JSON output in the `output` field.
3. WHEN the Execution status is `RUNNING`, THE CheckHandler SHALL return `output: null` and `stopDate: null`.
4. WHEN the Execution status is `FAILED`, THE CheckHandler SHALL populate the `cause` and `error` fields from the Step Functions response.
5. IF the `executionId` query parameter is absent or empty, THEN THE CheckHandler SHALL return HTTP 400 with `{"message": "Execution ID is required", "error": "Missing executionId parameter"}`.
6. IF the Step Functions service returns `ExecutionDoesNotExist`, THEN THE CheckHandler SHALL return HTTP 404 with `{"message": "Execution not found", "error": "<error detail>"}`.
7. IF any other Step Functions service error occurs, THEN THE CheckHandler SHALL return HTTP 500 with `{"message": "Failed to check execution status", "error": "<error detail>"}`.
8. THE CheckHandler SHALL construct the execution ARN from the `AWS_REGION`, `AWS_ACCOUNT_ID` environment variables, the fixed state machine name `ProcessAndReportJob`, and the provided `executionId`.
9. THE CheckHandler SHALL include CORS headers (`Access-Control-Allow-Origin: *`) on all responses.

---

### Requirement 3: State Machine Processing Modes

**User Story:** As a system operator, I want the state machine to support three processing modes, so that I can choose the right concurrency model for each workload.

#### Acceptance Criteria

1. WHEN the StateMachine receives input with `processType` equal to `whole`, THE StateMachine SHALL invoke the ProcessingLambda once with the full input payload and store the result in `$.results`.
2. WHEN the StateMachine receives input with `processType` equal to `loop`, THE StateMachine SHALL iterate over `$.items` using a Map state with `maxConcurrency` of 1, invoking the ProcessingLambda once per item sequentially, and store the array of results in `$.results`.
3. WHEN the StateMachine receives input with `processType` equal to `parallel`, THE StateMachine SHALL iterate over `$.items` using a Map state with `maxConcurrency` read from `$.maxConcurrency`, invoking the ProcessingLambda concurrently, and store the array of results in `$.results`.
4. WHEN the StateMachine receives input with an unrecognised `processType`, THE StateMachine SHALL transition immediately to the `ProcessingFailed` Fail state.
5. WHILE processing any mode, THE StateMachine SHALL execute a 1-second Wait state before and after the processing step for operational visibility.
6. WHEN any processing step (ProcessJob, ParallelProcess Map, or LoopProcess Map) raises an error, THE StateMachine SHALL catch the error and transition to the `ProcessingFailed` Fail state.
7. WHEN all processing steps complete successfully, THE StateMachine SHALL pass through a `FinalState` Pass state that assembles `originalInput`, `results`, `count`, `processedAt`, and `status` fields before transitioning to the `Succeed` state.
8. THE StateMachine SHALL enforce a maximum execution duration of 30 minutes.

---

### Requirement 4: Item Processing Lambda

**User Story:** As a developer, I want the ProcessingLambda to transform each item and return a structured result, so that downstream consumers receive consistent output regardless of processing mode.

#### Acceptance Criteria

1. WHEN the ProcessingLambda receives an event with an `items` array, THE ProcessingLambda SHALL process each element and return a list of result objects.
2. WHEN the ProcessingLambda receives an event with a single `item` field, THE ProcessingLambda SHALL process that item and return a single result object.
3. WHEN the ProcessingLambda processes an item, THE ProcessingLambda SHALL return a result object containing `item`, `processedItem` (uppercased value), `customData`, `processedAt` (ISO 8601 UTC timestamp), `status` (`"processed"`), and `message` fields.
4. WHEN the ProcessingLambda receives an item with value `"FAIL!"`, THE ProcessingLambda SHALL raise a `ValueError` to signal a processing failure to the StateMachine.
5. THE ProcessingLambda SHALL wait 10 seconds before processing each item to simulate work.
6. THE ProcessingLambda SHALL preserve the `customData` field from the input event in each result object.

---

### Requirement 5: DuckDB Data Query Endpoint

**User Story:** As a data analyst, I want to query Parquet files stored in S3 using SQL, so that I can retrieve structured data without managing a database.

#### Acceptance Criteria

1. WHEN a POST request is received at `/data` with a JSON body containing `s3_path` and `query` fields, THE DataHandler SHALL execute the SQL query against the specified S3 Parquet file using DuckDB and return HTTP 200 with `message`, `s3_path`, `row_count`, `columns`, and `data` fields.
2. WHEN the `query` string contains the identifier `parquet_data`, THE DataHandler SHALL create a DuckDB view named `parquet_data` over the S3 Parquet file before executing the query.
3. IF the request body is missing `s3_path` or `query`, THEN THE DataHandler SHALL return HTTP 400 with `{"message": "Missing s3_path or query in request"}`.
4. IF the DuckDB query execution fails, THEN THE DataHandler SHALL return HTTP 500 with `{"message": "Error executing query: <error detail>"}`.
5. THE DataHandler SHALL load the DuckDB `httpfs` extension to enable S3 access.
6. THE DataHandler SHALL use `/tmp` as the DuckDB home directory to comply with Lambda's read-only filesystem constraint.
7. THE DuckDbLambda SHALL support the same `s3_path` and `query` interface as the DataHandler, using the native Python DuckDB client.
8. THE DuckDbLambda SHALL configure the S3 region from the `AWS_REGION` environment variable.

---

### Requirement 6: Formula Parsing Library

**User Story:** As a developer, I want to parse spreadsheet-style formula strings into an AST and print them back, so that I can programmatically inspect and transform formulas.

#### Acceptance Criteria

1. WHEN a valid formula string is provided to the FormulaParser, THE FormulaParser SHALL parse it into a FormulaAST without errors.
2. WHEN an invalid formula string is provided to the FormulaParser, THE FormulaParser SHALL throw an exception with a descriptive error message.
3. THE FormulaParser SHALL support arithmetic operators `+`, `-`, `*`, `/`, and `^` (power) with correct operator precedence.
4. THE FormulaParser SHALL support unary `+` and `-` operators.
5. THE FormulaParser SHALL support parenthesised sub-expressions.
6. THE FormulaParser SHALL support numeric literals (integer and floating-point).
7. THE FormulaParser SHALL support string literals enclosed in double quotes.
8. THE FormulaParser SHALL support boolean literals `TRUE` and `FALSE`.
9. THE FormulaParser SHALL support cell references in the form `[A-Z]+[0-9]+` (e.g., `A1`, `BC42`).
10. THE FormulaParser SHALL support cell range references in the form `CellRef:CellRef` (e.g., `A1:B10`).
11. THE FormulaParser SHALL support function calls in the form `IDENTIFIER(argList?)`.
12. THE FormulaStringBuilder SHALL serialise any FormulaAST back into a valid formula string.
13. FOR ALL valid formula strings `f`, parsing `f` then printing the resulting FormulaAST with FormulaStringBuilder then parsing the result SHALL produce an equivalent FormulaAST (round-trip property).

---

### Requirement 7: Infrastructure as Code

**User Story:** As a DevOps engineer, I want all AWS resources defined in CDK stacks, so that the entire application can be deployed and torn down repeatably.

#### Acceptance Criteria

1. THE CDK SHALL define a `StepStack` that provisions the StateMachine and ProcessingLambda, and exports the `StateMachineArn` as a CloudFormation output.
2. THE CDK SHALL define an `ApiStack` that provisions the API_Gateway, TriggerHandler, CheckHandler, and DataHandler, and exports the `ApiEndpoint` as a CloudFormation output.
3. THE CDK SHALL define a `DuckDbStack` that provisions the DuckDbLambda with the public DuckDB Python layer, and exports `DuckDbLambdaArn` and `DuckDbLambdaName` as CloudFormation outputs.
4. THE TriggerHandler SHALL be granted only `states:StartExecution` on the specific StateMachine ARN.
5. THE CheckHandler SHALL be granted only `states:DescribeExecution` and `states:GetExecutionHistory` on execution ARNs matching the pattern `arn:aws:states:<region>:<account>:execution:ProcessAndReportJob:*`.
6. THE DataHandler SHALL be granted only `s3:GetObject` on the `arn:aws:s3:::lintang-test1/*` resource.
7. THE DuckDbLambda SHALL be granted `s3:GetObject` and `s3:ListBucket` on S3 resources.
8. THE API_Gateway SHALL enable CORS for all origins and methods on all resources.
9. WHEN `cdk deploy --all` is executed, THE CDK SHALL deploy all three stacks to the configured AWS account and region without requiring manual approval when `--require-approval never` is specified.
