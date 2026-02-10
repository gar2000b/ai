"""Collective tools and handle_tool_calls that delegates to code and test modules."""

import json

from . import code
from . import command
from . import test

CODE_TOOL_NAMES = {"write_code", "update_code_section", "read_code"}
COMMAND_TOOL_NAMES = {"run_program", "work", "list"}
TEST_TOOL_NAMES = {"foo", "bar", "baz", "bat"}

tools = code.tools + command.tools + test.tools


def handle_tool_calls(tool_calls):
    results = []
    for tool_call in tool_calls:
        tool_name = tool_call.function.name
        if tool_name in CODE_TOOL_NAMES:
            results.extend(code.handle_tool_calls([tool_call]))
        elif tool_name in COMMAND_TOOL_NAMES:
            results.extend(command.handle_tool_calls([tool_call]))
        elif tool_name in TEST_TOOL_NAMES:
            results.extend(test.handle_tool_calls([tool_call]))
        else:
            results.append({
                "role": "tool",
                "content": json.dumps({}),
                "tool_call_id": tool_call.id,
            })
    return results
