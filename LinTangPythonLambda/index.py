import json
import datetime
import time
from typing import Any, Dict, List


def _process_single(item: Any, base_input: Dict[str, Any]) -> Dict[str, Any]:
    """Process a single item and return a result dict."""
    # Wait 10 seconds before processing
    time.sleep(10)
    
    # Allow item to be a string or object; normalize string
    item_value = item if not isinstance(item, dict) else item.get('item', item)
    
    # Fail if item is "FAIL!"
    if item_value == "FAIL!":
        raise ValueError(f"Item processing failed for: {item_value}")
    
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat() + 'Z'
    custom_data = base_input.get('customData', {})

    
    return {
        'item': item_value,
        'processedItem': item_value.upper(),
        'customData': custom_data,
        'processedAt': timestamp,
        'status': 'processed',
        'message': f"Processed item: {item_value} at {timestamp}"
    }


def handler(event, context):
    try:
        input_data: Dict[str, Any] = event if isinstance(event, dict) else {}

        # If invoked with a bare string, return a single processed item
        if not isinstance(event, dict) and isinstance(event, str):
            return _process_single(event, {})

        # Prefer list of items if provided; else treat 'item' as single-item input
        if 'items' in input_data and isinstance(input_data['items'], list):
            return [_process_single(it, input_data) for it in input_data['items']]

        if 'item' in input_data:
            return _process_single(input_data['item'], input_data)

        # Default behavior with a placeholder when no items provided
        return [_process_single('default-item', input_data)]
    except ValueError as e:
        # Re-raise ValueError to fail the Lambda execution
        raise
