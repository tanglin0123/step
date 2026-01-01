# LinTangJavaLambda - DuckDB Data Handler

This Lambda function provides a Java-based handler for querying Parquet files stored in S3 using DuckDB, exposed through an API Gateway endpoint.

## Overview

The `DataHandler` class handles HTTP POST requests to the `/data` API endpoint and executes DuckDB queries against Parquet files in S3. It returns query results in JSON format.

## Request Format

Send a POST request to the `/data` endpoint with the following JSON body:

```json
{
  "s3_path": "s3://bucket-name/path/to/file.parquet",
  "query": "SELECT * FROM parquet_data LIMIT 5"
}
```

### Parameters:
- **s3_path** (required): Full S3 path to the Parquet file to query
- **query** (required): SQL query to execute against the data

## Response Format

Successful responses return HTTP 200 with JSON body:

```json
{
  "message": "Query executed successfully",
  "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
  "row_count": 5,
  "columns": ["time", "gold", "bitcoin"],
  "data": [
    {"time": 1730154000000, "gold": 0.96, "bitcoin": 0.97},
    ...
  ]
}
```

Error responses return appropriate HTTP status codes (400, 500) with error messages.

## Example Queries

### 1. Get First 5 Rows

```bash
curl -X POST https://{api-endpoint}/prod/data \
  -H "Content-Type: application/json" \
  -d '{
    "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
    "query": "SELECT * FROM parquet_data LIMIT 5"
  }'
```

Response:
```json
{
  "message": "Query executed successfully",
  "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
  "row_count": 5,
  "columns": ["time", "gold", "bitcoin"],
  "data": [
    {"gold": 0.96, "time": 1730154000000, "bitcoin": 0.97},
    {"gold": 0.9792, "time": 1730154001000, "bitcoin": 0.9312},
    {"gold": 1.02816, "time": 1730154002000, "bitcoin": 0.97776},
    {"gold": 1.0487232, "time": 1730154003000, "bitcoin": 0.9875376},
    {"gold": 1.017261504, "time": 1730154004000, "bitcoin": 1.03691448}
  ]
}
```

### 2. Aggregate Statistics

```bash
curl -X POST https://{api-endpoint}/prod/data \
  -H "Content-Type: application/json" \
  -d '{
    "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
    "query": "SELECT COUNT(*) as total_rows, AVG(gold) as avg_gold, AVG(bitcoin) as avg_bitcoin FROM parquet_data"
  }'
```

Response:
```json
{
  "message": "Query executed successfully",
  "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
  "row_count": 1,
  "columns": ["total_rows", "avg_gold", "avg_bitcoin"],
  "data": [
    {
      "avg_gold": 0.720622689438218,
      "total_rows": 517,
      "avg_bitcoin": 0.8822185605003504
    }
  ]
}
```

### 3. Filter with WHERE Clause

```bash
curl -X POST https://{api-endpoint}/prod/data \
  -H "Content-Type: application/json" \
  -d '{
    "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
    "query": "SELECT * FROM parquet_data WHERE gold > 1.0 LIMIT 3"
  }'
```

Response:
```json
{
  "message": "Query executed successfully",
  "s3_path": "s3://lintang-test1/data_files/gold_vs_bitcoin.parquet",
  "row_count": 3,
  "columns": ["time", "gold", "bitcoin"],
  "data": [
    {"gold": 1.02816, "time": 1730154002000, "bitcoin": 0.97776},
    {"gold": 1.0487232, "time": 1730154003000, "bitcoin": 0.9875376},
    {"gold": 1.017261504, "time": 1730154004000, "bitcoin": 1.03691448}
  ]
}
```

## Architecture

### Components

1. **DataHandler.java**: RequestHandler implementation
   - Accepts APIGatewayProxyRequestEvent
   - Parses JSON request body
   - Executes DuckDB queries
   - Returns structured JSON responses

2. **DuckDB JDBC Integration**
   - Version: 1.1.3 (matches Python Lambda)
   - Extensions: httpfs (for S3 support)
   - Memory: In-process JDBC connection

3. **AWS Integration**
   - Lambda Runtime: Java 17
   - Ephemeral Storage: 512 MB (for DuckDB extensions)
   - Memory: 1024 MB
   - S3 Permissions: Read access to target bucket
   - Environment: /tmp directory for extension installation

### File Structure

```
LinTangJavaLambda/
├── src/main/java/com/lintang/lambda/
│   ├── TriggerHandler.java       # Step Functions trigger
│   ├── CheckHandler.java         # Execution status checker
│   └── DataHandler.java          # DuckDB query handler (NEW)
├── pom.xml                       # Maven configuration
├── target/function.jar           # Built JAR (deployed to Lambda)
└── README.md                      # This file
```

## Building and Deployment

### Build

```bash
cd LinTangJavaLambda
mvn clean package -DskipTests
```

This creates `target/function.jar` which is deployed to Lambda.

### Deploy

```bash
cd /Users/lintang/workspace/step
cdk deploy --all --require-approval never
```

## Key Implementation Details

### DuckDB Home Directory

The handler explicitly sets `home_directory=/tmp` before loading extensions:
```java
stmt.execute("SET home_directory='/tmp';");
stmt.execute("INSTALL httpfs;");
stmt.execute("LOAD httpfs;");
```

This is critical because Lambda containers have `/tmp` as the only writable directory.

### Conditional View Creation

The handler creates the `parquet_data` view only if the query references it:
```java
if (query.toLowerCase().contains("parquet_data")) {
    String createViewSql = "CREATE VIEW parquet_data AS SELECT * FROM read_parquet('" + s3Path + "');";
    stmt.execute(createViewSql);
}
```

### S3 Access

- IAM role grants `s3:GetObject` permissions on the target bucket
- DuckDB's httpfs extension uses Lambda execution role credentials automatically
- No explicit AWS credentials needed in code

## Performance Characteristics

- **Initialization Time**: ~10-15 seconds (first execution, includes extension installation)
- **Subsequent Queries**: ~2-5 seconds (depends on data size)
- **Data Size**: Limited by Lambda memory (1024 MB) and /tmp storage (512 MB)

## Error Handling

The handler returns:
- **400**: Missing required parameters (s3_path, query)
- **500**: Query execution errors, S3 access issues, or other runtime errors

## Comparison with Python Lambda

Both Python and Java DuckDB Lambdas:
- Use DuckDB 1.1.3
- Accept identical request format: `{"s3_path": "...", "query": "..."}`
- Return identical response structure
- Use httpfs extension for S3 access
- Set home_directory=/tmp for Lambda environment

Differences:
- Python: Uses prebuilt Lambda layer, faster cold starts
- Java: Includes DuckDB JDBC driver in JAR, slightly larger deployment package
- Java: Can handle larger ephemeral storage (512 MB vs 200 MB default)

## Dependencies

See `pom.xml` for complete dependency list:
- AWS Lambda Core/Events APIs
- Jackson (JSON processing)
- DuckDB JDBC Driver 1.1.3
- AWS SDK v1 (S3, StepFunctions)

## Notes

- All queries execute in memory with a local JDBC connection
- Parquet files are streamed from S3, not downloaded to local storage
- Results are limited by Lambda memory constraints
- Concurrent requests create separate DuckDB connections
