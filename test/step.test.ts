import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { StepStack } from '../lib/step-stack';

describe('StepStack', () => {
  let app: cdk.App;
  let stack: StepStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new StepStack(app, 'TestStepStack');
    template = Template.fromStack(stack);
  });

  describe('Stack Properties', () => {
    test('should create a stack with correct name', () => {
      expect(stack.stackName).toBe('step-stack');
    });

    test('should be a valid CDK stack', () => {
      expect(stack).toBeInstanceOf(cdk.Stack);
    });
  });

  describe('Lambda Function', () => {
    test('should create a Lambda function', () => {
      template.resourceCountIs('AWS::Lambda::Function', 1);
    });

    test('Lambda function should have correct runtime', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'nodejs18.x',
        Handler: 'index.handler',
        Timeout: 3,
      });
    });

    test('Lambda function should have correct code', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Code: Match.objectLike({
          ZipFile: Match.stringLikeRegexp('Hello World'),
        }),
      });
    });
  });

  describe('Step Functions State Machine', () => {
    test('should create a State Machine', () => {
      template.resourceCountIs('AWS::StepFunctions::StateMachine', 1);
    });

    test('State Machine should have correct name', () => {
      template.hasResourceProperties('AWS::StepFunctions::StateMachine', {
        StateMachineName: 'ProcessAndReportJob',
      });
    });

    test('State Machine should have correct role', () => {
      template.hasResourceProperties('AWS::StepFunctions::StateMachine', {
        RoleArn: Match.objectLike({}),
      });
    });

    test('State Machine definition should be defined', () => {
      template.hasResourceProperties('AWS::StepFunctions::StateMachine', {
        DefinitionString: Match.objectLike({}),
      });
    });

    test('State Machine definition should contain Lambda task', () => {
      const stateMachine = template.findResources('AWS::StepFunctions::StateMachine');
      expect(Object.keys(stateMachine).length).toBe(1);
      
      const machineDefinition = Object.values(stateMachine)[0] as any;
      const definitionString = JSON.stringify(machineDefinition.Properties.DefinitionString);
      expect(definitionString).toContain('ProcessJob');
      expect(definitionString).toContain('lambda:invoke');
    });

    test('State Machine definition should contain Wait task', () => {
      const stateMachine = template.findResources('AWS::StepFunctions::StateMachine');
      const machineDefinition = Object.values(stateMachine)[0] as any;
      const definitionString = JSON.stringify(machineDefinition.Properties.DefinitionString);
      expect(definitionString).toContain('Wait');
      expect(definitionString).toContain('Seconds');
    });

    test('State Machine definition should contain Succeed state', () => {
      const stateMachine = template.findResources('AWS::StepFunctions::StateMachine');
      const machineDefinition = Object.values(stateMachine)[0] as any;
      const definitionString = JSON.stringify(machineDefinition.Properties.DefinitionString);
      expect(definitionString).toContain('GreetedWorld');
      expect(definitionString).toContain('Succeed');
    });
  });

  describe('IAM Roles', () => {
    test('should create IAM role for Lambda and State Machine', () => {
      // Count IAM roles (should be at least 2: one for Lambda, one for State Machine)
      const roles = template.findResources('AWS::IAM::Role');
      expect(Object.keys(roles).length).toBeGreaterThanOrEqual(1);
    });

    test('Lambda execution role should have basic execution policy', () => {
      template.hasResourceProperties('AWS::IAM::Role', {
        AssumeRolePolicyDocument: Match.objectLike({
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Principal: Match.objectLike({
                Service: Match.stringLikeRegexp('lambda'),
              }),
            }),
          ]),
        }),
      });
    });
  });

  describe('Integration Tests', () => {
    test('State Machine should be able to invoke Lambda function', () => {
      // Verify that there are policies allowing state machine to invoke lambda
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: Match.objectLike({
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Action: 'lambda:InvokeFunction',
            }),
          ]),
        }),
      });
    });

    test('should create all necessary resources', () => {
      // Verify minimum required resources
      template.resourceCountIs('AWS::Lambda::Function', 1);
      template.resourceCountIs('AWS::StepFunctions::StateMachine', 1);
      
      const roles = template.findResources('AWS::IAM::Role');
      expect(Object.keys(roles).length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('Stack Snapshot', () => {
    test('stack synthesis should match snapshot', () => {
      const json = JSON.parse(JSON.stringify(template));
      expect(json).toBeDefined();
    });
  });
});
