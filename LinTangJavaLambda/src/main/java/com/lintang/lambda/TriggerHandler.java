package com.lintang.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TriggerHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, String> corsHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        h.put("Access-Control-Allow-Headers", "Content-Type");
        h.put("Access-Control-Allow-Methods", "OPTIONS,POST");
        h.put("Content-Type", "application/json");
        return h;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(corsHeaders());

        try {
            if (event == null || event.getBody() == null || event.getBody().isEmpty()) {
                return badRequest(response, "Request body is required");
            }

            JsonNode bodyJson = MAPPER.readTree(event.getBody());

            // Validate processType
            String rawProcessType = bodyJson.path("processType").asText(null);
            if (rawProcessType == null) {
                return badRequest(response, "processType is required (parallel|loop|whole)");
            }

            String processType = rawProcessType.toLowerCase();
            if (!processType.equals("parallel") && !processType.equals("loop") && !processType.equals("whole")) {
                return badRequest(response, "processType must be one of: parallel, loop, whole");
            }

            // Validate items; allow single 'item' for convenience
            List<String> items = new ArrayList<>();
            JsonNode itemsNode = bodyJson.get("items");
            if (itemsNode != null && itemsNode.isArray()) {
                if (itemsNode.size() == 0) {
                    return badRequest(response, "items must be a non-empty array of strings");
                }
                for (JsonNode n : itemsNode) {
                    if (!n.isTextual() || n.asText().isBlank()) {
                        return badRequest(response, "items must contain non-empty strings");
                    }
                    items.add(n.asText());
                }
            } else {
                JsonNode singleItemNode = bodyJson.get("item");
                if (singleItemNode == null || !singleItemNode.isTextual() || singleItemNode.asText().isBlank()) {
                    return badRequest(response, "Provide either 'items' (array of strings) or 'item' (single string)");
                }
                items.add(singleItemNode.asText());
            }

            ObjectNode inputPayload = MAPPER.createObjectNode();
            inputPayload.put("processType", processType);
            ArrayNode arr = inputPayload.putArray("items");
            items.forEach(arr::add);
            // Set maxConcurrency: use caller's value if provided, otherwise use items length for parallel processing
            int maxConcurrency = items.size();
            if (bodyJson.has("maxConcurrency") && bodyJson.get("maxConcurrency").isNumber()) {
                maxConcurrency = bodyJson.get("maxConcurrency").asInt();
            }
            inputPayload.put("maxConcurrency", maxConcurrency);

            String stateMachineArn = System.getenv("STATE_MACHINE_ARN");
            if (stateMachineArn == null || stateMachineArn.isEmpty()) {
                response.setStatusCode(500);
                response.setBody(writeJson(Map.of(
                        "message", "STATE_MACHINE_ARN not configured"
                )));
                return response;
            }

            String executionName = "execution-" + UUID.randomUUID();

            AWSStepFunctions sfn = AWSStepFunctionsClientBuilder.defaultClient();

            StartExecutionRequest req = new StartExecutionRequest()
                .withStateMachineArn(stateMachineArn)
                .withName(executionName)
                .withInput(inputPayload.toString());

            StartExecutionResult startRes = sfn.startExecution(req);

            String executionArn = startRes.getExecutionArn();
            String[] parts = executionArn.split(":");
            String executionId = parts[parts.length - 1];

            Map<String, Object> ok = new HashMap<>();
            ok.put("message", "State Machine execution started successfully");
            ok.put("executionId", executionId);

            response.setStatusCode(200);
            response.setBody(writeJson(ok));
            return response;
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody(writeJson(Map.of(
                    "message", "Failed to start state machine execution",
                    "error", e.getMessage()
            )));
            return response;
        }
    }

    private static String writeJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization error\"}";
        }
    }

    private APIGatewayProxyResponseEvent badRequest(APIGatewayProxyResponseEvent response, String message) {
        response.setStatusCode(400);
        response.setBody(writeJson(Map.of(
                "message", message
        )));
        return response;
    }
}
