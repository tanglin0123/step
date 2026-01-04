package com.lintang.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling API Gateway proxy requests and responses.
 */
public class ApiGatewayUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parse JSON request body into a Map.
     *
     * @param event API Gateway proxy request event
     * @return Map containing parsed JSON
     * @throws Exception if JSON parsing fails
     */
    public static Map<String, Object> parseRequestBody(APIGatewayProxyRequestEvent event) throws Exception {
        String body = event.getBody();
        if (body == null || body.isEmpty()) {
            return new HashMap<>();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) mapper.readValue(body, Map.class);
        return result;
    }

    /**
     * Create a successful API Gateway response.
     *
     * @param statusCode HTTP status code
     * @param body Response body object
     * @return API Gateway proxy response event
     */
    public static APIGatewayProxyResponseEvent createSuccessResponse(int statusCode, Object body) {
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(statusCode);
            response.setBody(mapper.writeValueAsString(body));
            return response;
        } catch (Exception e) {
            return createErrorResponse(500, "Failed to serialize response: " + e.getMessage());
        }
    }

    /**
     * Create an error API Gateway response.
     *
     * @param statusCode HTTP status code
     * @param message Error message
     * @return API Gateway proxy response event
     */
    public static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
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

    /**
     * Get ObjectMapper instance for JSON operations.
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return mapper;
    }
}
