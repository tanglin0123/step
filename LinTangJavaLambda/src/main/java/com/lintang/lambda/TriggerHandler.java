package com.lintang.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
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
                Map<String, Object> body = Map.of(
                        "message", "Request body is required",
                        "error", "No JSON body provided"
                );
                response.setStatusCode(400);
                response.setBody(writeJson(body));
                return response;
            }

            JsonNode bodyJson = MAPPER.readTree(event.getBody());

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
                    .withInput(bodyJson.toString());

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
}
