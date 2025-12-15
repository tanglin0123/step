import json
import datetime

def handler(event, context):
    try:
        input_data = event or {}
        message = input_data.get('message', 'Hello from Lambda')
        custom_data = input_data.get('customData', {})

        processed_data = {
            'originalInput': input_data,
            'message': message,
            'customData': custom_data,
            'processedAt': datetime.datetime.utcnow().isoformat() + 'Z',
            'status': 'processed',
            'responseData': {
                'greeting': f"{message} - Processed at {datetime.datetime.utcnow().isoformat()}Z",
                'inputFieldCount': len(input_data.keys()),
                'receivedFields': list(input_data.keys())
            }
        }

        return processed_data
    except Exception as e:
        raise {
            'statusCode': 500,
            'message': 'Lambda function execution failed',
            'error': str(e)
        }
