"""Tool wrappers for run_program, work, and list commands."""

import json
import os

from commands.command import list_file as _list_file
from commands.command import run_program as _run_program
from utils.prompy import agent_dir
from utils.screen import print_work_directory


def run_program(arg: str = None, file_name: str = None, **kwargs) -> dict:
    """Compile (if Java) and run a program by name or path. Paths relative to work/ or absolute."""
    name = arg or file_name
    if not name:
        return {"success": False, "error": "Missing arg (program name or path)"}
    work_dir = os.path.join(agent_dir, "work")
    result = _run_program(work_dir, name)
    # Truncate long stdout for LLM context (full output already shown on screen)
    if result.get("stdout") and len(result["stdout"]) > 1500:
        lines = result["stdout"].rstrip().split("\n")
        result = dict(result)
        result["stdout"] = "\n".join(lines[:10]) + f"\n... ({len(lines) - 10} more lines; full output on screen)"
    return result


def work() -> dict:
    """List the contents of the work directory (ASCII tree)."""
    work_dir = os.path.join(agent_dir, "work")
    print(f"\nWork directory: {work_dir}")
    return print_work_directory(agent_dir)


def list(file_path: str) -> dict:
    """Display a file with syntax highlighting. Paths relative to work/ or absolute (e.g. main.py, src/utils.py)."""
    work_dir = os.path.join(agent_dir, "work")
    if os.path.isabs(file_path):
        path = os.path.dirname(file_path)
        file_name = os.path.basename(file_path)
    else:
        full = os.path.normpath(os.path.join(work_dir, file_path))
        path = os.path.dirname(full)
        file_name = os.path.basename(full)
    _list_file(path, file_name)
    return {"success": True, "file_path": file_path}


run_program_json = {
    "name": "run_program",
    "description": "Compile (if Java) and run a Python or Java program by name or path. Paths relative to work/ or absolute (e.g. fruits, hello_world/HelloWorld1). Returns {success, stdout, stderr, returncode, arg?, error?}; check returncode and stderr on failure.",
    "parameters": {
        "type": "object",
        "properties": {
            "arg": {
                "type": "string",
                "description": "Program name or path (e.g. fruits, work/Fibonacci, hello_world/HelloWorld1).",
            }
        },
        "required": ["arg"],
        "additionalProperties": False,
    },
}

work_json = {
    "name": "work",
    "description": "List the contents of the work directory in ASCII tree format. Returns {success, work_dir, tree, error?}; tree is plain ASCII, also shown on screen.",
    "parameters": {
        "type": "object",
        "properties": {},
        "required": [],
        "additionalProperties": False,
    },
}

list_json = {
    "name": "list",
    "description": "Display a file with syntax highlighting. Paths relative to work/ or absolute (e.g. main.py, src/utils.py, hello_world/HelloWorld1.java). Returns {success, file_path}; content is shown on screen.",
    "parameters": {
        "type": "object",
        "properties": {
            "file_path": {
                "type": "string",
                "description": "Path to the file (e.g. main.py, hello_world/HelloWorld1.java).",
            }
        },
        "required": ["file_path"],
        "additionalProperties": False,
    },
}

tools = [
    {"type": "function", "function": run_program_json},
    {"type": "function", "function": work_json},
    {"type": "function", "function": list_json},
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
