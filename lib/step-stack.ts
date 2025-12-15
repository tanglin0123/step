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
      code: lambda.Code.fromAsset('LinTangPythonLambda'),
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.handler',
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

    // Lambda function to trigger the Step Function (Java)
    const triggerStepFunctionLambda = new lambda.Function(this, 'TriggerStateMachineLambda', {
      code: lambda.Code.fromAsset('LinTangJavaLambda/target/function.jar'),
      runtime: lambda.Runtime.JAVA_17,
      handler: 'com.lintang.lambda.TriggerHandler::handleRequest',
      timeout: cdk.Duration.seconds(10),
      memorySize: 512,
      environment: {
        STATE_MACHINE_ARN: stateMachine.stateMachineArn,
      },
    });

    // Grant Lambda permission to start Step Function executions
    stateMachine.grantStartExecution(triggerStepFunctionLambda);

    // Lambda function to check Step Function execution status
    const checkExecutionStatusLambda = new lambda.Function(this, 'CheckExecutionStatusLambda', {
      code: lambda.Code.fromAsset('LinTangJavaLambda/target/function.jar'),
      runtime: lambda.Runtime.JAVA_17,
      handler: 'com.lintang.lambda.CheckHandler::handleRequest',
      timeout: cdk.Duration.seconds(10),
      memorySize: 512,
      environment: {
        AWS_ACCOUNT_ID: this.account,
      },
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
