package com.lintang.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            // Parse request body
            String body = event.getBody();
            Map<String, String> request = mapper.readValue(body, new TypeReference<Map<String, String>>(){});
            
            String s3Path = request.get("s3_path");
            String query = request.get("query");

            if (s3Path == null || query == null) {
                return createErrorResponse(400, "Missing s3_path or query in request");
            }

            context.getLogger().log("Processing query on " + s3Path);

            // Execute DuckDB query
            Map<String, Object> result = executeQuery(s3Path, query, context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", 200);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Query executed successfully");
            responseBody.put("s3_path", s3Path);
            responseBody.putAll(result);
            
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(200);
            apiResponse.setBody(mapper.writeValueAsString(responseBody));
            return apiResponse;

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(500, "Error executing query: " + e.getMessage());
        }
    }

    private Map<String, Object> executeQuery(String s3Path, String query, Context context) throws Exception {
        // Ensure /tmp exists and is writable
        String tmpDir = "/tmp";
        new File(tmpDir).mkdirs();

        // Create DuckDB connection in memory with /tmp as home directory
        String connectionUrl = "jdbc:duckdb::memory:";
        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            // Set home directory for extensions
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET home_directory='" + tmpDir + "';");
                
                // Install and load httpfs extension for S3 support
                stmt.execute("INSTALL httpfs;");
                stmt.execute("LOAD httpfs;");
                
                context.getLogger().log("Extensions loaded");
            }

            // Create view from S3 parquet file if needed
            if (query.toLowerCase().contains("parquet_data")) {
                try (Statement stmt = conn.createStatement()) {
                    String createViewSql = "CREATE VIEW parquet_data AS SELECT * FROM read_parquet('" + s3Path + "');";
                    stmt.execute(createViewSql);
                    context.getLogger().log("View created from " + s3Path);
                }
            }

            // Execute the query
            List<Map<String, Object>> rows = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(query);
                ResultSetMetaData metadata = rs.getMetaData();
                
                // Get column names
                int columnCount = metadata.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metadata.getColumnName(i));
                }

                // Get rows
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        row.put(columns.get(i - 1), value);
                    }
                    rows.add(row);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("row_count", rows.size());
            result.put("columns", columns);
            result.put("data", rows);
            
            return result;
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        try {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("statusCode", statusCode);
            errorBody.put("message", message);
            response.setBody(mapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            response.setBody("{\"statusCode\": " + statusCode + ", \"message\": \"" + message + "\"}");
        }
        return response;
    }
}
