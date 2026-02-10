import json
import os

from utils.prompy import agent_dir


def _resolve_target_dir(path: str) -> str:
    """Resolve target directory; empty path defaults to work."""
    work_dir = os.path.join(agent_dir, "work")
    if not path or not path.strip():
        return work_dir
    normalized_path = path.strip().replace("\\", "/")
    return os.path.normpath(os.path.join(work_dir, normalized_path))


def read_code(file_name: str, path: str = "") -> dict:
    """Read a file and return its contents for the LLM to process."""
    target_dir = _resolve_target_dir(path)
    file_path = os.path.join(target_dir, file_name)
    if not os.path.isfile(file_path):
        return {"success": False, "error": f"File not found: {file_path}", "content": ""}
    try:
        with open(file_path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()
        return {"success": True, "file_path": file_path, "content": content}
    except Exception as e:
        return {"success": False, "error": str(e), "content": ""}


def write_code(code: str, file_name: str, path: str = "") -> None:
    """Write code to a new file at the given path with the given name."""

    print(f"*** Writing code to {file_name} in {path or 'work'}")

    target_dir = _resolve_target_dir(path)
    os.makedirs(target_dir, exist_ok=True)
    file_path = os.path.join(target_dir, file_name)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(code)


def update_code_section(
    code_section: str,
    begin_marker: str,
    end_marker: str,
    file_name: str,
    path: str = "",
) -> dict:
    """Replace the content between begin_marker and end_marker (inclusive lines) with code_section.
    Markers stay on their own lines; only the content between them is replaced."""
    target_dir = _resolve_target_dir(path)
    file_path = os.path.join(target_dir, file_name)
    if not os.path.isfile(file_path):
        return {"success": False, "error": f"File not found: {file_path}"}
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            lines = f.readlines()
    except Exception as e:
        return {"success": False, "error": str(e)}
    begin_idx = None
    end_idx = None
    for i, line in enumerate(lines):
        if begin_marker in line:
            begin_idx = i
            break
    if begin_idx is None:
        return {"success": False, "error": f"Begin marker not found: {begin_marker!r}"}
    for i in range(begin_idx + 1, len(lines)):
        if end_marker in lines[i]:
            end_idx = i
            break
    if end_idx is None:
        return {"success": False, "error": f"End marker not found: {end_marker!r}"}
    new_content = (
        "".join(lines[: begin_idx + 1])
        + (code_section.rstrip() + "\n" if code_section else "")
        + "".join(lines[end_idx:])
    )
    try:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)
    except Exception as e:
        return {"success": False, "error": str(e)}
    print(f"*** Updated section in {file_name} (between {begin_marker!r} and {end_marker!r})")
    return {"success": True, "file_path": file_path}


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

update_code_section_json = {
    "name": "update_code_section",
    "description": "Replace the content between begin_marker and end_marker in a file. Markers stay on their own lines; only the content between them is replaced. Use for targeted edits (e.g. # BEGIN_SECTION / # END_SECTION).",
    "parameters": {
        "type": "object",
        "properties": {
            "code_section": {
                "type": "string",
                "description": "The new code to place between the markers.",
            },
            "begin_marker": {
                "type": "string",
                "description": "The string that identifies the start line (e.g. '# BEGIN_FOO'). The line containing this stays; content after it is replaced.",
            },
            "end_marker": {
                "type": "string",
                "description": "The string that identifies the end line (e.g. '# END_FOO'). The line containing this stays; content before it is replaced.",
            },
            "file_name": {
                "type": "string",
                "description": "The name of the file to update.",
            },
            "path": {
                "type": "string",
                "description": "Directory path relative to work/; empty defaults to work.",
            },
        },
        "required": ["code_section", "begin_marker", "end_marker", "file_name"],
        "additionalProperties": False,
    },
}

read_code_json = {
    "name": "read_code",
    "description": "Read a file and return its contents. Use before write_code or update_code_section to see existing code. Path: same as write_code (defaults to work).",
    "parameters": {
        "type": "object",
        "properties": {
            "file_name": {
                "type": "string",
                "description": "The name of the file to read.",
            },
            "path": {
                "type": "string",
                "description": "Directory path relative to work/; empty defaults to work.",
            },
        },
        "required": ["file_name"],
        "additionalProperties": False,
    },
}

tools = [
    {"type": "function", "function": write_code_json},
    {"type": "function", "function": update_code_section_json},
    {"type": "function", "function": read_code_json},
]

def handle_tool_calls(tool_calls):
    results = []
    for tool_call in tool_calls:
        tool_name = tool_call.function.name
        arguments = json.loads(tool_call.function.arguments)
        tool = globals().get(tool_name)
        result = tool(**arguments) if tool else {}
        results.append({"role": "tool", "content": json.dumps(result), "tool_call_id": tool_call.id})
    return results
