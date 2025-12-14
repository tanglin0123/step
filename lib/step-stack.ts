import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sfn from 'aws-cdk-lib/aws-stepfunctions';
import * as tasks from 'aws-cdk-lib/aws-stepfunctions-tasks';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as iam from 'aws-cdk-lib/aws-iam';

export class StepStack extends cdk.Stack {
  constructor(app: cdk.App, id: string, props?: cdk.StackProps) {
    super(app, id, {
      ...props,
      stackName: "step-stack",
      env: {
        account: '685915392751', // Replace with your AWS Account ID
        region: 'us-west-2',     // Replace with your desired AWS Region (e.g., 'us-east-1')
      },
    });

    const helloFunction = new lambda.Function(this, 'MyLambdaFunction', {
      code: lambda.Code.fromInline(`
        const corsHeaders = {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Headers': 'Content-Type',
          'Access-Control-Allow-Methods': 'OPTIONS,POST'
        };

        exports.handler = async (event) => {
          try {
            console.log('Lambda function received event:', JSON.stringify(event, null, 2));

            // Process the input from the state machine
            const input = event || {};
            
            // Extract fields from the input
            const message = input.message || 'Hello from Lambda';
            const customData = input.customData || {};

            // Perform some processing
            const processedData = {
              originalInput: input,
              message: message,
              customData: customData,
              processedAt: new Date().toISOString(),
              status: 'processed',
              responseData: {
                greeting: \`\${message} - Processed at \${new Date().toISOString()}\`,
                inputFieldCount: Object.keys(input).length,
                receivedFields: Object.keys(input)
              }
            };

            console.log('Processing complete:', JSON.stringify(processedData, null, 2));

            return processedData;
          } catch (error) {
            console.error('Error in Lambda function:', error);
            throw {
              statusCode: 500,
              message: 'Lambda function execution failed',
              error: error.message
            };
          }
        };
      `),
      runtime: lambda.Runtime.NODEJS_16_X,
      handler: "index.handler",
      timeout: cdk.Duration.seconds(3)
    });

    const processJob = new tasks.LambdaInvoke(this, 'ProcessJob', {
      lambdaFunction: helloFunction,
      outputPath: '$.Payload', // Extract the payload from the Lambda's output
      // resultPath: '$.lambdaResult', // Store the Lambda result in the state machine context
    });

    const waitTask = new sfn.Wait(this, 'Wait', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(5)),
    });

    const finalState = new sfn.Pass(this, 'FinalState', {
      resultPath: '$.finalResult',
      result: sfn.Result.fromObject({
        message: 'State machine execution completed',
        timestamp: new Date().toISOString(),
      }),
    });

    const stateMachineDefinition = processJob
      .next(waitTask)
      .next(finalState)
      .next(new sfn.Succeed(this, "GreetedWorld"));

    const stateMachine = new sfn.StateMachine(this, 'MyStateMachine', {
      definitionBody: sfn.DefinitionBody.fromChainable(stateMachineDefinition),
      timeout: cdk.Duration.minutes(5),
      stateMachineName: 'ProcessAndReportJob',
      // ... other StateMachineProps
    });

    // Lambda function to trigger the Step Function
    const triggerStepFunctionLambda = new lambda.Function(this, 'TriggerStateMachineLambda', {
      code: lambda.Code.fromInline(`
        const AWS = require('aws-sdk');
        const stepFunctions = new AWS.StepFunctions();
        const crypto = require('crypto');

        const corsHeaders = {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Headers': 'Content-Type',
          'Access-Control-Allow-Methods': 'OPTIONS,POST'
        };

        // Generate UUID v4
        function generateUUID() {
          return crypto.randomBytes(16).toString('hex').replace(
            /(\w{8})(\w{4})(\w{4})(\w{4})(\w{12})/,
            '$1-$2-$3-$4-$5'
          );
        }

        exports.handler = async (event) => {
          try {
            console.log('Received event:', JSON.stringify(event, null, 2));

            // Parse the request body
            let requestBody = event.body;
            
            // If body is a string, parse it as JSON
            if (typeof requestBody === 'string') {
              requestBody = JSON.parse(requestBody);
            }

            // Validate that we have a request body
            if (!requestBody) {
              return {
                statusCode: 400,
                headers: {
                  'Content-Type': 'application/json',
                  ...corsHeaders,
                },
                body: JSON.stringify({
                  message: 'Request body is required',
                  error: 'No JSON body provided'
                })
              };
            }

            console.log('Parsed request body:', requestBody);

            // Generate UUID for execution name
            const executionName = \`execution-\${generateUUID()}\`;

            // Start the Step Function execution with the request body as input
            const params = {
              stateMachineArn: process.env.STATE_MACHINE_ARN,
              input: JSON.stringify(requestBody),
              name: executionName
            };

            console.log('Starting state machine with params:', params);
            const result = await stepFunctions.startExecution(params).promise();

            return {
              statusCode: 200,
              headers: {
                'Content-Type': 'application/json',
                ...corsHeaders,
              },
              body: JSON.stringify({
                message: 'State Machine execution started successfully',
                executionId: result.executionArn.split(':').pop()
              })
            };
          } catch (error) {
            console.error('Error starting execution:', error);
            return {
              statusCode: 500,
              headers: {
                'Content-Type': 'application/json',
                ...corsHeaders,
              },
              body: JSON.stringify({
                message: 'Failed to start state machine execution',
                error: error.message
              })
            };
          }
        };
      `),
      runtime: lambda.Runtime.NODEJS_16_X,
      handler: 'index.handler',
      timeout: cdk.Duration.seconds(10),
      environment: {
        STATE_MACHINE_ARN: stateMachine.stateMachineArn,
      },
    });

    // Grant Lambda permission to start Step Function executions
    stateMachine.grantStartExecution(triggerStepFunctionLambda);

    // Lambda function to check Step Function execution status
    const checkExecutionStatusLambda = new lambda.Function(this, 'CheckExecutionStatusLambda', {
      code: lambda.Code.fromInline(`
        const AWS = require('aws-sdk');
        const stepFunctions = new AWS.StepFunctions();

        const corsHeaders = {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Headers': 'Content-Type',
          'Access-Control-Allow-Methods': 'OPTIONS,GET'
        };

        exports.handler = async (event) => {
          try {
            console.log('Received event:', JSON.stringify(event, null, 2));

            // Get execution ID from query string or path parameter
            const executionId = event.queryStringParameters?.executionId || event.pathParameters?.executionId;

            if (!executionId) {
              return {
                statusCode: 400,
                headers: {
                  'Content-Type': 'application/json',
                  ...corsHeaders,
                },
                body: JSON.stringify({
                  message: 'Execution ID is required',
                  error: 'Missing executionId parameter'
                })
              };
            }

            // Construct the execution ARN
            const executionArn = \`arn:aws:states:us-west-2:685915392751:execution:ProcessAndReportJob:\${executionId}\`;

            // Describe the execution
            const params = {
              executionArn: executionArn
            };

            console.log('Checking execution with ARN:', executionArn);
            const execution = await stepFunctions.describeExecution(params).promise();

            // Parse the output if available
            let output = null;
            if (execution.output) {
              try {
                output = JSON.parse(execution.output);
              } catch (e) {
                output = execution.output;
              }
            }

            // Get execution history for detailed information
            const historyParams = {
              executionArn: executionArn
            };

            const history = await stepFunctions.getExecutionHistory(historyParams).promise();

            return {
              statusCode: 200,
              headers: {
                'Content-Type': 'application/json',
                ...corsHeaders,
              },
              body: JSON.stringify({
                executionId: executionId,
                status: execution.status,
                output: output,
                startDate: execution.startDate,
                stopDate: execution.stopDate,
                cause: execution.cause,
                error: execution.error
              })
            };
          } catch (error) {
            console.error('Error checking execution status:', error);

            // Check if it's an execution not found error
            if (error.code === 'ExecutionDoesNotExist') {
              return {
                statusCode: 404,
                headers: {
                  'Content-Type': 'application/json',
                  ...corsHeaders,
                },
                body: JSON.stringify({
                  message: 'Execution not found',
                  error: error.message,
                  executionId: event.queryStringParameters?.executionId || event.pathParameters?.executionId
                })
              };
            }

            return {
              statusCode: 500,
              headers: {
                'Content-Type': 'application/json',
                ...corsHeaders,
              },
              body: JSON.stringify({
                message: 'Failed to check execution status',
                error: error.message
              })
            };
          }
        };
      `),
      runtime: lambda.Runtime.NODEJS_16_X,
      handler: 'index.handler',
      timeout: cdk.Duration.seconds(10),
    });

    // Grant Lambda permission to describe and get execution history
    checkExecutionStatusLambda.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          'states:DescribeExecution',
          'states:GetExecutionHistory'
        ],
        resources: [
          `arn:aws:states:${this.region}:${this.account}:execution:ProcessAndReportJob:*`
        ]
      })
    );

    // Create API Gateway
    const api = new apigateway.RestApi(this, 'StepFunctionApi', {
      restApiName: 'Step Function Trigger API',
      description: 'API to trigger Step Functions state machine',
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
        allowHeaders: apigateway.Cors.DEFAULT_HEADERS,
      },
    });

    // Create POST resource with JSON request model
    const triggerResource = api.root.addResource('trigger');
    
    // Add POST method with Lambda integration
    const postMethod = triggerResource.addMethod(
      'POST',
      new apigateway.LambdaIntegration(triggerStepFunctionLambda),
      {
        requestParameters: {
          'method.request.header.Content-Type': true,
        },
      }
    );

    // Add method response for 200
    postMethod.addMethodResponse({
      statusCode: '200',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Add method response for 400
    postMethod.addMethodResponse({
      statusCode: '400',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Add method response for 500
    postMethod.addMethodResponse({
      statusCode: '500',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Create GET resource for checking execution status
    const checkResource = api.root.addResource('check');

    // Add GET method with Lambda integration
    const getMethod = checkResource.addMethod(
      'GET',
      new apigateway.LambdaIntegration(checkExecutionStatusLambda),
      {
        requestParameters: {
          'method.request.querystring.executionId': true,
        },
      }
    );

    // Add method response for 200
    getMethod.addMethodResponse({
      statusCode: '200',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Add method response for 400
    getMethod.addMethodResponse({
      statusCode: '400',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Add method response for 404
    getMethod.addMethodResponse({
      statusCode: '404',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Add method response for 500
    getMethod.addMethodResponse({
      statusCode: '500',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    // Output the API endpoint
    new cdk.CfnOutput(this, 'ApiEndpoint', {
      value: api.url,
      description: 'API Gateway endpoint URL',
      exportName: 'StepFunctionApiEndpoint',
    });

    new cdk.CfnOutput(this, 'StateMachineArn', {
      value: stateMachine.stateMachineArn,
      description: 'State Machine ARN',
      exportName: 'StateMachineArn',
    });
  }
}
