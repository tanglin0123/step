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
          exports.handler = (event, context, callback) => {
              callback(null, "Hello World!");
          };
      `),
      runtime: lambda.Runtime.NODEJS_18_X,
      handler: "index.handler",
      timeout: cdk.Duration.seconds(3)
    });

    // const stateMachine = new sfn.StateMachine(this, 'MyStateMachine', {
    //   definition: new tasks.LambdaInvoke(this, "MyLambdaTask", {
    //     lambdaFunction: helloFunction
    //   }).next(new sfn.Succeed(this, "GreetedWorld"))
    // });

    const processJob = new tasks.LambdaInvoke(this, 'ProcessJob', {
      lambdaFunction: helloFunction,
      outputPath: '$.Payload', // Extract the payload from the Lambda's output
    });

    const waitTask = new sfn.Wait(this, 'Wait', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(5)),
    });

    const stateMachineDefinition = processJob
      .next(waitTask)
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

        exports.handler = async (event) => {
          try {
            const params = {
              stateMachineArn: process.env.STATE_MACHINE_ARN,
              input: JSON.stringify(event.body || {})
            };

            const result = await stepFunctions.startExecution(params).promise();

            return {
              statusCode: 200,
              body: JSON.stringify({
                message: 'State Machine execution started',
                executionArn: result.executionArn
              })
            };
          } catch (error) {
            console.error('Error starting execution:', error);
            return {
              statusCode: 500,
              body: JSON.stringify({
                message: 'Failed to start state machine execution',
                error: error.message
              })
            };
          }
        };
      `),
      runtime: lambda.Runtime.NODEJS_18_X,
      handler: 'index.handler',
      timeout: cdk.Duration.seconds(10),
      environment: {
        STATE_MACHINE_ARN: stateMachine.stateMachineArn,
      },
    });

    // Grant Lambda permission to start Step Function executions
    stateMachine.grantStartExecution(triggerStepFunctionLambda);

    // Create API Gateway
    const api = new apigateway.RestApi(this, 'StepFunctionApi', {
      restApiName: 'Step Function Trigger API',
      description: 'API to trigger Step Functions state machine',
    });

    // Create POST resource
    const triggerResource = api.root.addResource('trigger');
    triggerResource.addMethod('POST', new apigateway.LambdaIntegration(triggerStepFunctionLambda));

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
