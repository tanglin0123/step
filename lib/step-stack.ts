import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sfn from 'aws-cdk-lib/aws-stepfunctions';
import * as tasks from 'aws-cdk-lib/aws-stepfunctions-tasks';

export class StepStack extends cdk.Stack {
  public readonly stateMachine: sfn.StateMachine;

  constructor(app: cdk.App, id: string, props?: cdk.StackProps) {
    super(app, id, {
      ...props,
      stackName: 'step-stack',
      env: {
        account: '685915392751', // Replace with your AWS Account ID
        region: 'us-west-2',     // Replace with your desired AWS Region (e.g., 'us-east-1')
      },
    });

    const processLambdaFunction = new lambda.Function(this, 'MyLambdaFunction', {
      code: lambda.Code.fromAsset('LinTangPythonLambda'),
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.handler',
      timeout: cdk.Duration.seconds(3),
    });

    const processJob = new tasks.LambdaInvoke(this, 'ProcessJob', {
      lambdaFunction: processLambdaFunction,
      payloadResponseOnly: true,
      resultPath: '$.results',
    });

    // Separate instances for Map processors
    const parallelProcessJob = new tasks.LambdaInvoke(this, 'ParallelProcessJob', {
      lambdaFunction: processLambdaFunction,
      payloadResponseOnly: true,
      payload: sfn.TaskInput.fromObject({
        item: sfn.JsonPath.stringAt('$'),
      }),
    });

    const loopProcessJob = new tasks.LambdaInvoke(this, 'LoopProcessJob', {
      lambdaFunction: processLambdaFunction,
      payloadResponseOnly: true,
      payload: sfn.TaskInput.fromObject({
        item: sfn.JsonPath.stringAt('$'),
      }),
    });

    // Failure state for handling errors
    const failureState = new sfn.Fail(this, 'ProcessingFailed', {
      error: 'ProcessingError',
      cause: 'An error occurred during item processing',
    });

    // Add error handling to whole process Lambda
    processJob.addCatch(failureState);

    const parallelProcess = new sfn.Map(this, 'ParallelProcess', {
      itemsPath: '$.items',
      resultPath: '$.results',
      maxConcurrencyPath: '$.maxConcurrency',
    });
    parallelProcess.itemProcessor(parallelProcessJob);
    // Add error handling to parallel Map
    parallelProcess.addCatch(failureState);

    const loopProcess = new sfn.Map(this, 'LoopProcess', {
      itemsPath: '$.items',
      resultPath: '$.results',
      maxConcurrency: 1,
    });
    loopProcess.itemProcessor(loopProcessJob);
    // Add error handling to loop Map
    loopProcess.addCatch(failureState);

    // No transform states needed when Lambda and Map write directly to $.results

    // Wait states before each processor
    const waitBeforeWhole = new sfn.Wait(this, 'WaitBeforeWhole', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
    });

    const waitBeforeParallel = new sfn.Wait(this, 'WaitBeforeParallel', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
    });

    const waitBeforeLoop = new sfn.Wait(this, 'WaitBeforeLoop', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
    });

    // Wait states after each processor
    const waitAfterWhole = new sfn.Wait(this, 'WaitAfterWhole', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
    });

    const waitAfterParallel = new sfn.Wait(this, 'WaitAfterParallel', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
    });

    const waitAfterLoop = new sfn.Wait(this, 'WaitAfterLoop', {
      time: sfn.WaitTime.duration(cdk.Duration.seconds(1)),
    });

    // Chain: waitBefore → processor → waitAfter for each path
    const wholeChain = sfn.Chain.start(waitBeforeWhole)
      .next(processJob)
      .next(waitAfterWhole);

    const parallelChain = sfn.Chain.start(waitBeforeParallel)
      .next(parallelProcess)
      .next(waitAfterParallel);

    const loopChain = sfn.Chain.start(waitBeforeLoop)
      .next(loopProcess)
      .next(waitAfterLoop);

    const finalState = new sfn.Pass(this, 'FinalState', {
      resultPath: '$',
      parameters: {
        originalInput: {
          'processType.$': '$.processType',
          'items.$': '$.items',
          'maxConcurrency.$': '$.maxConcurrency',
        },
        'results.$': '$.results',
        'count.$': 'States.ArrayLength($.results)',
        'processedAt.$': '$$.State.EnteredTime',
        'status': 'processed',
      },
    });

    const successState = new sfn.Succeed(this, 'Succeed');

    const choiceState = new sfn.Choice(this, 'ProcessModeChoice', {
      comment: 'Switch between single, loop, and parallel processing',
    })
      .when(sfn.Condition.stringEquals('$.processType', 'parallel'), parallelChain)
      .when(sfn.Condition.stringEquals('$.processType', 'loop'), loopChain)
      .when(sfn.Condition.stringEquals('$.processType', 'whole'), wholeChain)
      .otherwise(failureState);

    // All branches converge to finalState
    choiceState.afterwards().next(finalState);
    finalState.next(successState);

    this.stateMachine = new sfn.StateMachine(this, 'MyStateMachine', {
      definitionBody: sfn.DefinitionBody.fromChainable(choiceState),
      timeout: cdk.Duration.minutes(5),
      stateMachineName: 'ProcessAndReportJob',
    });

    new cdk.CfnOutput(this, 'StateMachineArn', {
      value: this.stateMachine.stateMachineArn,
      description: 'State Machine ARN',
      exportName: 'StateMachineArn',
    });
  }
}
