package com.lintang.duckdb;

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

/**
 * Utility class for DuckDB operations.
 */
public class DuckDBUtils {

    /**
     * Execute a DuckDB query and return results.
     *
     * @param s3Path S3 path to the Parquet file
     * @param query SQL query to execute
     * @param logger Optional logger for debug output (can be null)
     * @return Map containing row_count, columns, and data
     * @throws Exception if query execution fails
     */
    public static Map<String, Object> executeQuery(String s3Path, String query, Object logger) throws Exception {
        // Ensure /tmp exists and is writable
        String tmpDir = "/tmp";
        new File(tmpDir).mkdirs();

        // Create DuckDB connection in memory
        String connectionUrl = "jdbc:duckdb::memory:";
        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            // Set home directory and load extensions
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET home_directory='" + tmpDir + "';");
                stmt.execute("INSTALL httpfs;");
                stmt.execute("LOAD httpfs;");

                if (logger != null) {
                    logMessage(logger, "DuckDB extensions loaded");
                }
            }

            // Create view from S3 parquet file if needed
            if (query.toLowerCase().contains("parquet_data")) {
                try (Statement stmt = conn.createStatement()) {
                    String createViewSql = "CREATE VIEW parquet_data AS SELECT * FROM read_parquet('" + s3Path + "');";
                    stmt.execute(createViewSql);
                    if (logger != null) {
                        logMessage(logger, "View created from " + s3Path);
                    }
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

    /**
     * Log a message using the provided logger.
     *
     * @param logger Logger object (can be com.amazonaws.services.lambda.runtime.Context)
     * @param message Message to log
     */
    private static void logMessage(Object logger, String message) {
        try {
            // Try to use AWS Lambda Context logger
            if (logger != null && logger.getClass().getName().contains("Context")) {
                java.lang.reflect.Method getLogger = logger.getClass().getMethod("getLogger");
                Object loggerObj = getLogger.invoke(logger);
                java.lang.reflect.Method log = loggerObj.getClass().getMethod("log", String.class);
                log.invoke(loggerObj, message);
            }
        } catch (Exception e) {
            // Silently fail if logging doesn't work
        }
    }
}
