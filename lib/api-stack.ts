import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as sfn from 'aws-cdk-lib/aws-stepfunctions';

interface ApiStackProps extends cdk.StackProps {
  stateMachine: sfn.IStateMachine;
}

export class ApiStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string, props: ApiStackProps) {
    super(scope, id, {
      ...props,
      stackName: 'api-stack',
    });

    const { stateMachine } = props;

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

    stateMachine.grantStartExecution(triggerStepFunctionLambda);

    const checkExecutionStatusLambda = new lambda.Function(this, 'CheckExecutionStatusLambda', {
      code: lambda.Code.fromAsset('LinTangJavaLambda/target/function.jar'),
      runtime: lambda.Runtime.JAVA_17,
      handler: 'com.lintang.lambda.CheckHandler::handleRequest',
      timeout: cdk.Duration.seconds(10),
      memorySize: 512,
      environment: {
        AWS_ACCOUNT_ID: this.account,
        STATE_MACHINE_ARN: stateMachine.stateMachineArn,
      },
    });

    const stateMachineArn = stateMachine.stateMachineArn;
    const stateMachineName = stateMachineArn.split(':').pop();
    const executionArn = cdk.Stack.of(this).formatArn({
      service: 'states',
      resource: 'execution',
      resourceName: `${stateMachineName}:*`,
    });

    checkExecutionStatusLambda.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: ['states:DescribeExecution', 'states:GetExecutionHistory'],
        resources: [executionArn],
      })
    );

    const api = new apigateway.RestApi(this, 'StepFunctionApi', {
      restApiName: 'Step Function Trigger API',
      description: 'API to trigger Step Functions state machine',
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
        allowHeaders: apigateway.Cors.DEFAULT_HEADERS,
      },
    });

    const triggerResource = api.root.addResource('trigger');
    const postMethod = triggerResource.addMethod(
      'POST',
      new apigateway.LambdaIntegration(triggerStepFunctionLambda),
      {
        requestParameters: {
          'method.request.header.Content-Type': true,
        },
      }
    );

    postMethod.addMethodResponse({
      statusCode: '200',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    postMethod.addMethodResponse({
      statusCode: '400',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    postMethod.addMethodResponse({
      statusCode: '500',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    const checkResource = api.root.addResource('check');
    const getMethod = checkResource.addMethod(
      'GET',
      new apigateway.LambdaIntegration(checkExecutionStatusLambda),
      {
        requestParameters: {
          'method.request.querystring.executionId': true,
        },
      }
    );

    getMethod.addMethodResponse({
      statusCode: '200',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    getMethod.addMethodResponse({
      statusCode: '400',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    getMethod.addMethodResponse({
      statusCode: '404',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    getMethod.addMethodResponse({
      statusCode: '500',
      responseModels: {
        'application/json': apigateway.Model.EMPTY_MODEL,
      },
    });

    new cdk.CfnOutput(this, 'ApiEndpoint', {
      value: api.url,
      description: 'API Gateway endpoint URL',
      exportName: 'StepFunctionApiEndpoint',
    });
  }
}
