import * as cdk from 'aws-cdk-lib';
import { StepStack } from './step-stack';
import { ApiStack } from './api-stack';
import { DuckDbStack } from './duckdb-stack';

export function createApp(): cdk.App {
  const app = new cdk.App();
  const env = {
    account: '685915392751',
    region: 'us-west-2',
  };

  const stepStack = new StepStack(app, 'StepStack', { env });
  const apiStack = new ApiStack(app, 'ApiStack', { env, stateMachine: stepStack.stateMachine });
  const duckDbStack = new DuckDbStack(app, 'DuckDbStack', { env });

  return app;
}
