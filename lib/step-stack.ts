import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sfn from 'aws-cdk-lib/aws-stepfunctions';
import * as tasks from 'aws-cdk-lib/aws-stepfunctions-tasks';

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
  }
}
