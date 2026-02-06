import json
import os

from utils.prompy import agent_dir


def write_code(code: str, file_name: str, path: str = "") -> None:
    """Write code to a new file at the given path with the given name."""
    if not path or not path.strip():
        path = os.path.join(agent_dir, "work")
    os.makedirs(path, exist_ok=True)
    file_path = os.path.join(path, file_name)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(code)

write_code_json = {
    "name": "write_code",
    "description": "Write code to a new file at the given path with the given name.",
    "parameters": {
        "type": "object",
        "properties": {
            "code": {
                "type": "string",
                "description": "The code to write to the file."
            },
            "file_name": {
                "type": "string",
                "description": "The name of the file to write the code to."
            },
            "path": {
                "type": "string",
                "description": "The directory path to write the file to. Defaults to the work directory if empty or omitted."
            }
        },
        "required": ["code", "file_name"],
        "additionalProperties": False
    }
}

tools = [{"type": "function", "function": write_code_json}]

def handle_tool_calls(tool_calls):
    results = []
    for tool_call in tool_calls:
        tool_name = tool_call.function.name
        arguments = json.loads(tool_call.function.arguments)
        tool = globals().get(tool_name)
        result = tool(**arguments) if tool else {}
        results.append({"role": "tool", "content": json.dumps(result), "tool_call_id": tool_call.id})
    return results
