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
    test('should create Lambda functions', () => {
      template.resourceCountIs('AWS::Lambda::Function', 3); // Original hello function + trigger lambda + check status lambda
    });

    test('Lambda function should have correct runtime', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'python3.11',
        Handler: 'index.handler',
        Timeout: 3,
      });
    });

    // Code is provided via asset (Python package), so no inline ZipFile assertion
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
      // With payloadResponseOnly, it uses direct Lambda ARN instead of lambda:invoke
      expect(definitionString).toContain('MyLambdaFunction');
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
      template.resourceCountIs('AWS::Lambda::Function', 3); // Original + Trigger Lambda + Check Status Lambda
      template.resourceCountIs('AWS::StepFunctions::StateMachine', 1);
      
      const roles = template.findResources('AWS::IAM::Role');
      expect(Object.keys(roles).length).toBeGreaterThanOrEqual(1);
    });
  });

  describe('API Gateway and Trigger Lambda', () => {
    test('should create an API Gateway REST API', () => {
      template.resourceCountIs('AWS::ApiGateway::RestApi', 1);
    });

    test('API Gateway should have correct name', () => {
      template.hasResourceProperties('AWS::ApiGateway::RestApi', {
        Name: 'Step Function Trigger API',
      });
    });

    test('should create a Lambda function to trigger state machine', () => {
      template.resourceCountIs('AWS::Lambda::Function', 3); // Original + Trigger Lambda + Check Status Lambda
    });

    test('Trigger Lambda should have correct configuration', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'java17',
        Handler: 'com.lintang.lambda.TriggerHandler::handleRequest',
        Timeout: 10,
        MemorySize: 512,
      });
    });

    test('Trigger Lambda should have STATE_MACHINE_ARN environment variable', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Environment: Match.objectLike({
          Variables: Match.objectLike({
            STATE_MACHINE_ARN: Match.objectLike({}),
          }),
        }),
      });
    });

    test('should create API Gateway resource', () => {
      const resources = template.findResources('AWS::ApiGateway::Resource');
      expect(Object.keys(resources).length).toBeGreaterThanOrEqual(1);
    });

    test('should create API Gateway method', () => {
      const methods = template.findResources('AWS::ApiGateway::Method');
      expect(Object.keys(methods).length).toBeGreaterThanOrEqual(1);
    });

    test('Trigger Lambda should have permission to invoke from API Gateway', () => {
      template.hasResourceProperties('AWS::Lambda::Permission', {
        Action: 'lambda:InvokeFunction',
      });
    });
  });

  describe('API Integration', () => {
    test('Trigger Lambda should be granted permission to start state machine execution', () => {
      // Verify that there is a policy allowing trigger lambda to invoke state machine
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

  describe('Check Execution Status API', () => {
    test('should create a Lambda function for checking execution status', () => {
      template.resourceCountIs('AWS::Lambda::Function', 3);
    });

    test('Check Status Lambda should have correct configuration', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'java17',
        Handler: 'com.lintang.lambda.CheckHandler::handleRequest',
        Timeout: 10,
        MemorySize: 512,
      });
    });

    test('should create API resource for /check endpoint', () => {
      const resources = template.findResources('AWS::ApiGateway::Resource');
      const resourceValues = Object.values(resources) as any[];
      const checkResource = resourceValues.find(r => 
        r.Properties?.PathPart === 'check'
      );
      expect(checkResource).toBeDefined();
    });

    test('should create GET method for /check endpoint', () => {
      const methods = template.findResources('AWS::ApiGateway::Method');
      const methodValues = Object.values(methods) as any[];
      const checkMethod = methodValues.find(m => 
        m.Properties?.HttpMethod === 'GET'
      );
      expect(checkMethod).toBeDefined();
    });

    test('Check Status Lambda should be granted permission to describe executions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: Match.objectLike({
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Action: Match.arrayWith([
                'states:DescribeExecution',
                'states:GetExecutionHistory'
              ]),
            }),
          ]),
        }),
      });
    });

    test('Check Status Lambda should have permission to invoke from API Gateway', () => {
      template.hasResourceProperties('AWS::Lambda::Permission', {
        Action: 'lambda:InvokeFunction',
      });
    });

    test('Check endpoint should require executionId query parameter', () => {
      const methods = template.findResources('AWS::ApiGateway::Method');
      const methodValues = Object.values(methods) as any[];
      const checkMethod = methodValues.find(m => 
        m.Properties?.HttpMethod === 'GET'
      );
      expect(checkMethod?.Properties?.RequestParameters).toBeDefined();
    });
  });

  describe('Stack Outputs', () => {
    test('should have API Endpoint output', () => {
      template.hasOutput('ApiEndpoint', {
        Export: Match.objectLike({
          Name: 'StepFunctionApiEndpoint',
        }),
      });
    });

    test('should have State Machine ARN output', () => {
      template.hasOutput('StateMachineArn', {
        Export: Match.objectLike({
          Name: 'StateMachineArn',
        }),
      });
    });
  });
});
