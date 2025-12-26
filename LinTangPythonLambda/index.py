import json
import datetime
from typing import Any, Dict, List


def _process_single(item: Any, base_input: Dict[str, Any]) -> Dict[str, Any]:
    """Process a single item and return a result dict."""
    # Allow item to be a string or object; normalize string
    item_value = item if not isinstance(item, dict) else item.get('item', item)
    
    # Fail if item is "FAIL!"
    if item_value == "FAIL!":
        raise ValueError(f"Item processing failed for: {item_value}")
    
    timestamp = datetime.datetime.utcnow().isoformat() + 'Z'
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

        # If the map state passes a bare string, treat it as a single item
        if not isinstance(event, dict) and isinstance(event, str):
            single_result = _process_single(event, {})
            return {
                'originalInput': event,
                'results': [single_result],
                'count': 1,
                'status': 'processed',
            }

        # Prefer list of items if provided
        items: List[Any] = []
        if 'items' in input_data and isinstance(input_data['items'], list):
            items = input_data['items']
        elif 'item' in input_data:
            items = [input_data['item']]
        else:
            # No items provided; default behavior with a placeholder
            items = ['default-item']

        results: List[Dict[str, Any]] = []
        for it in items:
            results.append(_process_single(it, input_data))

        # Build aggregate response
        aggregate = {
            'originalInput': input_data,
            'results': results,
            'count': len(results),
            'processedAt': datetime.datetime.utcnow().isoformat() + 'Z',
            'status': 'processed',
            'receivedFields': list(input_data.keys()),
        }
        return aggregate
    except ValueError as e:
        # Re-raise ValueError to fail the Lambda execution
        raise
