"""LLM integration (mocked for now)."""

import types

import config.env
from openai import OpenAI

from tools.tools import handle_tool_calls, tools
from utils.screen import print_llm_response_stream

openai = OpenAI(api_key=config.env.openai_api_key)

# Chat history for context (list of {"role": "user"|"assistant", "content": str})
_messages: list[dict[str, str]] = []

SYSTEM_PROMPT = """When the user asks for code, always wrap it in markdown fenced code blocks with the language specified.
Format: ```language
code
```
Example for Java: ```java
public class Hello { ... }
```
Example for Python: ```python
def main(): ...
```
Use the correct language tag (java, python, javascript, etc.) so the code renders with syntax highlighting. Be concise—return the code with minimal prose when a code-only response is requested.
If the user asks for code to be written to a file, always use the write_code tool to write the code to the file (but only if the user asks for it to be written to a file).
If the user asks for code to be written to a file, but they do not specify a path, then do not include a path argument in the write_code tool call.
If the user asks for code to be written to a file, but they do not specify a file name, then guess the file name based on the user's request. Java files should be named like "Hello.java", Python files should be named like "main.py", etc.
When arithmetic expressions are presented in the user prompt with tool calls included. Makes sure the results from the tools are called and processed as you would expect mathematically, for examples: result = foo + bar, where result is the result of the arithmetic expression. Always return the result of the arithmetic expression back to the user in the final response.
"""


def _stream_and_accumulate(stream, accumulated):
    """Yield content for typewriting; fill accumulated with content, finish_reason, tool_calls."""
    tool_calls_by_index = {}
    for chunk in stream:
        if not chunk.choices:
            continue
        choice = chunk.choices[0]
        delta = choice.delta
        if delta and delta.content:
            accumulated["content"] = accumulated.get("content", "") + delta.content
            yield delta.content
        if choice.finish_reason:
            accumulated["finish_reason"] = choice.finish_reason
        if delta and delta.tool_calls:
            for tool_call_delta in delta.tool_calls:
                index = getattr(tool_call_delta, "index", 0)
                if index not in tool_calls_by_index:
                    tool_calls_by_index[index] = {"id": "", "name": "", "arguments": ""}
                if getattr(tool_call_delta, "id", None):
                    tool_calls_by_index[index]["id"] = tool_call_delta.id
                function = getattr(tool_call_delta, "function", None)
                if function:
                    if getattr(function, "name", None):
                        tool_calls_by_index[index]["name"] = function.name
                    if getattr(function, "arguments", None):
                        tool_calls_by_index[index]["arguments"] += function.arguments
    accumulated.setdefault("content", "")
    accumulated.setdefault("finish_reason", "stop")
    accumulated["tool_calls"] = [
        types.SimpleNamespace(
            id=tool_calls_by_index[index]["id"],
            function=types.SimpleNamespace(
                name=tool_calls_by_index[index]["name"],
                arguments=tool_calls_by_index[index]["arguments"],
            ),
        )
        for index in sorted(tool_calls_by_index)
    ] if tool_calls_by_index else None


def llm(prompt: str) -> None:
    """Call the LLM with the given prompt; stream response and typewrite to screen; update history."""
    _messages.append({"role": "user", "content": prompt})
    done = False
    while not done:
        messages = [{"role": "system", "content": SYSTEM_PROMPT}] + _messages
        stream = openai.chat.completions.create(
            model="gpt-4.1-nano",
            messages=messages,
            stream=True,
            tools=tools,
        )

        accumulated = {}
        print_llm_response_stream(_stream_and_accumulate(stream, accumulated), delay=0.02)
        if accumulated["finish_reason"] == "tool_calls" and accumulated["tool_calls"]:
            accumulated_tool_calls = accumulated["tool_calls"]
            _messages.append({
                "role": "assistant",
                "content": accumulated["content"] or "",
                "tool_calls": [
                    {
                        "id": tool_call.id,
                        "type": "function",
                        "function": {"name": tool_call.function.name, "arguments": tool_call.function.arguments},
                    }
                    for tool_call in accumulated_tool_calls
                ],
            })
            _messages.extend(handle_tool_calls(accumulated_tool_calls))
        else:
            _messages.append({"role": "assistant", "content": accumulated["content"]})
            done = True
