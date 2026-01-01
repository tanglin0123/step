import json
import duckdb
import os


def handler(event, context):
    """
    Lambda handler that uses DuckDB to query Parquet files from S3.
    
    Expected event structure:
    {
        "s3_path": "s3://bucket-name/path/to/file.parquet",
        "query": "SELECT * FROM parquet_data WHERE column > 100 LIMIT 10"
    }
    """
    try:
        # Get S3 path and query from event
        s3_path = event.get('s3_path', 's3://your-bucket/data/*.parquet')
        query = event.get('query', 'SELECT * FROM parquet_data LIMIT 10')
        
        # Create an in-memory DuckDB connection
        con = duckdb.connect(':memory:')
        
        # Set DuckDB home directory to /tmp (Lambda's writable directory)
        con.execute("SET home_directory='/tmp';")
        
        # Install and load httpfs extension for S3 access
        con.execute("INSTALL httpfs;")
        con.execute("LOAD httpfs;")
        
        # Configure S3 region (uses AWS Lambda's IAM role credentials automatically)
        region = os.environ.get('AWS_REGION', 'us-west-2')
        con.execute(f"SET s3_region='{region}';")
        
        # Only create parquet_data view if the query references it
        if 'parquet_data' in query.lower():
            # Create a view/table from the S3 Parquet file
            con.execute(f"CREATE VIEW parquet_data AS SELECT * FROM read_parquet('{s3_path}');")
        
        # Execute the user's query
        result = con.execute(query).fetchall()
        columns = [desc[0] for desc in con.description]
        
        # Convert result to list of dicts
        result_dicts = [dict(zip(columns, row)) for row in result]
        
        # Close connection
        con.close()
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Query executed successfully',
                's3_path': s3_path,
                'row_count': len(result_dicts),
                'columns': columns,
                'data': result_dicts
            }, default=str)
        }
        
    except Exception as e:
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': str(e),
                'message': 'Failed to process data with DuckDB'
            })
        }
