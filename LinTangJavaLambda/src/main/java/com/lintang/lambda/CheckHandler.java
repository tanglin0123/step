package com.lintang.lambda;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionRequest;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionResult;
import com.amazonaws.services.stepfunctions.model.GetExecutionHistoryRequest;
import com.amazonaws.services.stepfunctions.model.GetExecutionHistoryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class CheckHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, String> corsHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        h.put("Access-Control-Allow-Headers", "Content-Type");
        h.put("Access-Control-Allow-Methods", "OPTIONS,GET");
        h.put("Content-Type", "application/json");
        return h;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(corsHeaders());

        try {
            String executionId = null;
            if (event != null) {
                if (event.getQueryStringParameters() != null) {
                    executionId = event.getQueryStringParameters().get("executionId");
                }
                if ((executionId == null || executionId.isEmpty()) && event.getPathParameters() != null) {
                    executionId = event.getPathParameters().get("executionId");
                }
            }

            if (executionId == null || executionId.isEmpty()) {
                response.setStatusCode(400);
                response.setBody(writeJson(Map.of(
                        "message", "Execution ID is required",
                        "error", "Missing executionId parameter"
                )));
                return response;
            }

            String region = System.getenv().getOrDefault("AWS_REGION", "us-west-2");
            String accountId = System.getenv().getOrDefault("AWS_ACCOUNT_ID", "685915392751");
            String stateMachineName = "ProcessAndReportJob";
            String executionArn = String.format("arn:aws:states:%s:%s:execution:%s:%s", region, accountId, stateMachineName, executionId);

            AWSStepFunctions sfn = AWSStepFunctionsClientBuilder.defaultClient();

            DescribeExecutionRequest describeReq = new DescribeExecutionRequest()
                    .withExecutionArn(executionArn);

            DescribeExecutionResult describeRes = sfn.describeExecution(describeReq);

            JsonNode output = null;
            if (describeRes.getOutput() != null) {
                try {
                    output = MAPPER.readTree(describeRes.getOutput());
                } catch (Exception e) {
                    // leave output as raw string
                }
            }

            GetExecutionHistoryRequest historyReq = new GetExecutionHistoryRequest()
                    .withExecutionArn(executionArn);
            GetExecutionHistoryResult history = sfn.getExecutionHistory(historyReq);

            Map<String, Object> ok = new HashMap<>();
            ok.put("executionId", executionId);
            ok.put("status", describeRes.getStatus());
            ok.put("output", output != null ? output : describeRes.getOutput());
            ok.put("startDate", describeRes.getStartDate());
            ok.put("stopDate", describeRes.getStopDate());
            ok.put("cause", describeRes.getCause());
            ok.put("error", describeRes.getError());
            ok.put("events", history.getEvents());

            response.setStatusCode(200);
            response.setBody(writeJson(ok));
            return response;
        } catch (AmazonServiceException ase) {
            if ("ExecutionDoesNotExist".equals(ase.getErrorCode())) {
                response.setStatusCode(404);
                response.setBody(writeJson(Map.of(
                        "message", "Execution not found",
                        "error", ase.getErrorMessage()
                )));
                return response;
            }
            response.setStatusCode(500);
            response.setBody(writeJson(Map.of(
                    "message", "Failed to check execution status",
                    "error", ase.getErrorMessage()
            )));
            return response;
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody(writeJson(Map.of(
                    "message", "Failed to check execution status",
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
