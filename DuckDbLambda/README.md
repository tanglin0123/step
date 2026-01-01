# DuckDbLambda

Python AWS Lambda that uses DuckDB to query Parquet files stored in S3 (via DuckDB `httpfs` extension).

## Handler

- Entry point: `index.handler`
- Runtime: Python 3.11
- Source: [`DuckDbLambda/index.py`](index.py)

Expected event:

```json
{
  "s3_path": "s3://bucket-name/path/to/file.parquet",
  "query": "SELECT * FROM parquet_data WHERE column > 100 LIMIT 10"
}
```

If `s3_path` is omitted it defaults to `s3://your-bucket/data/*.parquet`.  
If `query` is omitted it defaults to `SELECT * FROM parquet_data LIMIT 10`.

Response body includes:

- `row_count`
- `columns`
- `data` (list of row objects)

## Dependencies (via Lambda Layer)

This Lambda uses a **public prebuilt Lambda layer** that includes DuckDB 1.1.3 for Python 3.11 on x86_64.

- **Blog**: https://www.bbourgeois.dev/blog/2025/04-duckdb-aws-lambda-layers
- **GitHub**: https://github.com/bengeois/aws-layer-duckdb-python
- **Layer ARN** (us-west-2): `arn:aws:lambda:us-west-2:911510765542:layer:duckdb-python311-x86_64:12`

The layer is automatically attached in [`lib/duckdb-stack.ts`](../lib/duckdb-stack.ts). **No Docker or manual packaging required**.

## Deployment

From the repo root:

```bash
npm install
npm run build
cdk deploy DuckDbStack
```

The deployment will:
- Create the Lambda function
- Attach the public DuckDB layer
- Grant S3 read permissions (`s3:GetObject`, `s3:ListBucket`)

## Local development (editor import resolution)

If VS Code shows `Import "duckdb" could not be resolved`, install `duckdb` into your local Python environment for editor support:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r DuckDbLambda/requirements.txt
```

Then in VS Code:

- Run **Python: Select Interpreter** and pick `.venv`
- Optionally run **Python: Restart Language Server**

## Changing DuckDB version or Python runtime

If you need a different DuckDB version, Python runtime, or AWS region:

1. Find the appropriate layer ARN from https://github.com/bengeois/aws-layer-duckdb-python/blob/main/data/arns.json
2. Update the `duckDbLayer` ARN in [`lib/duckdb-stack.ts`](../lib/duckdb-stack.ts)
3. Update the `runtime` if changing Python version
4. Update [`DuckDbLambda/requirements.txt`](requirements.txt) to match the layer version (for local dev)

## Notes / gotchas

- The handler runs `INSTALL httpfs; LOAD httpfs;` at runtime. On first execution this may download the extension from the internet. If your Lambda is in a VPC without NAT, this can fail.
- The stack currently grants broad S3 permissions (`s3:GetObject`, `s3:ListBucket` on `*`). Prefer scoping to specific bucket ARNs for production.
- The public layer is maintained at https://github.com/bengeois/aws-layer-duckdb-python and covers all AWS regions, Python runtimes, and DuckDB versions.
