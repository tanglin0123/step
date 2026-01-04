# LinTangJavaLib

A shared Java library providing utilities for AWS Lambda functions used in the LinTang project.

## Overview

This library contains reusable utilities for:
- API Gateway proxy request/response handling
- DuckDB database operations
- Common Lambda patterns

## Modules

### ApiGatewayUtils

Utilities for handling API Gateway proxy requests and responses:

```java
import com.lintang.util.ApiGatewayUtils;

// Parse request body
Map<String, Object> data = ApiGatewayUtils.parseRequestBody(event);

// Create success response
APIGatewayProxyResponseEvent response = ApiGatewayUtils.createSuccessResponse(200, resultMap);

// Create error response
APIGatewayProxyResponseEvent errorResponse = ApiGatewayUtils.createErrorResponse(400, "Invalid request");
```

### DuckDBUtils

Utilities for executing DuckDB queries against S3 Parquet files:

```java
import com.lintang.duckdb.DuckDBUtils;

// Execute a query
Map<String, Object> result = DuckDBUtils.executeQuery(
    "s3://bucket/path/file.parquet",
    "SELECT * FROM parquet_data LIMIT 5",
    context  // Optional Context for logging
);

// Result contains: row_count, columns, data
```

## Building

```bash
mvn clean install
```

This installs the library to your local Maven repository, making it available to other projects.

## Usage in Other Projects

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.lintang</groupId>
    <artifactId>LinTangJavaLib</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Project Structure

```
LinTangJavaLib/
├── src/main/java/com/lintang/
│   ├── util/
│   │   └── ApiGatewayUtils.java
│   └── duckdb/
│       └── DuckDBUtils.java
├── pom.xml
└── README.md
```

## Dependencies

- AWS Lambda Java Core (1.2.2)
- AWS Lambda Java Events (3.11.3)
- Jackson Databind (2.17.2)

## Version

1.0.0 - Initial release

## Related Projects

- **LinTangJavaLambda**: Uses this library for Lambda handlers
- **DuckDbLambda**: Python-based DuckDB Lambda (similar functionality)
