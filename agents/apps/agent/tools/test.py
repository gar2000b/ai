import json


def foo() -> int:
    """Test tool that returns 1."""
    return 1


def bar() -> int:
    """Test tool that returns 2."""
    return 2


def baz() -> int:
    """Test tool that returns 3."""
    return 3


def bat() -> int:
    """Test tool that returns 4."""
    return 4


foo_json = {
    "name": "foo",
    "description": "Test tool that returns 1.",
    "parameters": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": False,
    },
}

bar_json = {
    "name": "bar",
    "description": "Test tool that returns 2.",
    "parameters": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": False,
    },
}

baz_json = {
    "name": "baz",
    "description": "Test tool that returns 3.",
    "parameters": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": False,
    },
}

bat_json = {
    "name": "bat",
    "description": "Test tool that returns 4.",
    "parameters": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": False,
    },
}

tools = [
    {"type": "function", "function": foo_json},
    {"type": "function", "function": bar_json},
    {"type": "function", "function": baz_json},
    {"type": "function", "function": bat_json},
]


def handle_tool_calls(tool_calls):
    results = []
    for tool_call in tool_calls:
        tool_name = tool_call.function.name
        arguments = json.loads(tool_call.function.arguments or "{}")
        tool = globals().get(tool_name)
        result = tool(**arguments) if tool else {}
        results.append({"role": "tool", "content": json.dumps(result), "tool_call_id": tool_call.id})
    return results
