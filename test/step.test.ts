import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { createApp } from '../lib/app';

describe('CDK App', () => {
  let app: cdk.App;
  let templates: Map<string, Template>;

  beforeEach(() => {
    app = createApp();
    templates = new Map();
    app.node.children.forEach((stack: any) => {
      templates.set(stack.stackName, Template.fromStack(stack));
    });
  });

  describe('StepStack', () => {
    test('has step-stack name', () => {
      const stepStack = app.node.children.find((s: any) => s.stackName === 'step-stack');
      expect(stepStack).toBeDefined();
    });

    test('creates the processing Lambda only', () => {
      const template = templates.get('step-stack')!;
      template.resourceCountIs('AWS::Lambda::Function', 1);
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'python3.11',
        Handler: 'index.handler',
      });
    });

    test('creates a single state machine with the expected name', () => {
      const template = templates.get('step-stack')!;
      template.resourceCountIs('AWS::StepFunctions::StateMachine', 1);
      template.hasResourceProperties('AWS::StepFunctions::StateMachine', {
        StateMachineName: 'ProcessAndReportJob',
      });
    });

    test('state machine definition contains key states', () => {
      const template = templates.get('step-stack')!;
      const resources = template.findResources('AWS::StepFunctions::StateMachine');
      const machine = Object.values(resources)[0] as any;
      const definitionString = JSON.stringify(machine.Properties.DefinitionString);

      expect(definitionString).toContain('ProcessModeChoice');
      expect(definitionString).toContain('ProcessJob');
      expect(definitionString).toContain('FinalState');
      expect(definitionString).toContain('ProcessingFailed');
    });
  });

  describe('ApiStack', () => {
    test('has api-stack name', () => {
      const apiStack = app.node.children.find((s: any) => s.stackName === 'api-stack');
      expect(apiStack).toBeDefined();
    });

    test('creates API Gateway REST API', () => {
      const template = templates.get('api-stack')!;
      template.resourceCountIs('AWS::ApiGateway::RestApi', 1);
      template.hasResourceProperties('AWS::ApiGateway::RestApi', {
        Name: 'Step Function Trigger API',
      });
    });

    test('creates two Java Lambdas for trigger and check', () => {
      const template = templates.get('api-stack')!;
      template.resourceCountIs('AWS::Lambda::Function', 2);
      template.hasResourceProperties('AWS::Lambda::Function', {
        Handler: 'com.lintang.lambda.TriggerHandler::handleRequest',
        Runtime: 'java17',
      });
      template.hasResourceProperties('AWS::Lambda::Function', {
        Handler: 'com.lintang.lambda.CheckHandler::handleRequest',
        Runtime: 'java17',
      });
    });

    test('trigger Lambda has STATE_MACHINE_ARN environment variable', () => {
      const template = templates.get('api-stack')!;
      template.hasResourceProperties('AWS::Lambda::Function', {
        Handler: 'com.lintang.lambda.TriggerHandler::handleRequest',
        Environment: Match.objectLike({
          Variables: Match.objectLike({
            STATE_MACHINE_ARN: Match.anyValue(),
          }),
        }),
      });
    });

    test('grants trigger Lambda permission to start executions', () => {
      const template = templates.get('api-stack')!;
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: Match.objectLike({
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Action: 'states:StartExecution',
            }),
          ]),
        }),
      });
    });
  });
});
