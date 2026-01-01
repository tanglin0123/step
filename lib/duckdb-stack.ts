import * as cdk from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';

export class DuckDbStack extends cdk.Stack {
  public readonly duckDbLambda: lambda.Function;

  constructor(scope: cdk.App, id: string, props?: cdk.StackProps) {
    super(scope, id, {
      ...props,
      stackName: 'duckdb-stack',
    });

    // DuckDB Lambda layer (public prebuilt layer from bengeois/aws-layer-duckdb-python)
    // DuckDB 1.1.3, Python 3.11, x86_64, us-west-2
    const duckDbLayer = lambda.LayerVersion.fromLayerVersionArn(
      this,
      'DuckDbLayer',
      'arn:aws:lambda:us-west-2:911510765542:layer:duckdb-python311-x86_64:12'
    );

    // Create Lambda function with DuckDB
    this.duckDbLambda = new lambda.Function(this, 'DuckDbLambda', {
      code: lambda.Code.fromAsset('DuckDbLambda'),
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.handler',
      timeout: cdk.Duration.seconds(900),
      memorySize: 1024, // DuckDB may need more memory for processing
      layers: [duckDbLayer],
    });

    // Grant S3 read permissions
    // Option 1: Grant read access to all S3 buckets (broad)
    this.duckDbLambda.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: ['s3:GetObject', 's3:ListBucket'],
        resources: ['*'], // Replace with specific bucket ARNs: ['arn:aws:s3:::your-bucket/*', 'arn:aws:s3:::your-bucket']
      })
    );

    // Option 2: If you have a specific bucket, use this instead:
    // const bucket = s3.Bucket.fromBucketName(this, 'DataBucket', 'your-bucket-name');
    // bucket.grantRead(this.duckDbLambda);

    // Output the Lambda ARN
    new cdk.CfnOutput(this, 'DuckDbLambdaArn', {
      value: this.duckDbLambda.functionArn,
      description: 'DuckDB Lambda Function ARN',
      exportName: 'DuckDbLambdaArn',
    });

    new cdk.CfnOutput(this, 'DuckDbLambdaName', {
      value: this.duckDbLambda.functionName,
      description: 'DuckDB Lambda Function Name',
      exportName: 'DuckDbLambdaName',
    });
  }
}
