"""LLM integration (mocked for now)."""

import types

import tiktoken

import config.env
from openai import OpenAI

import constants.ansi as ansi

from tools.tools import handle_tool_calls, tools
from utils.screen import print_llm_response_stream

openai = OpenAI(api_key=config.env.openai_api_key)

# Chat history for context (list of {"role": "user"|"assistant", "content": str})
_messages: list[dict[str, str]] = []

# Model config: {model_id: context_window_tokens}
MODELS = {
    "gpt-4.1-mini": 128_000,
    "gpt-4.1-nano": 128_000,
    "gpt-5.2": 400_000,
    # Add more as needed
}

MODEL = "gpt-5.2"
CONTEXT_WINDOW = MODELS.get(MODEL, 128_000)


def clear_messages() -> None:
    """Clear the chat history."""
    _messages.clear()


def print_messages() -> None:
    """Traverse _messages and render them nicely to the console (includes SYSTEM_PROMPT)."""
    print()
    # System prompt first (always included in API calls)
    print(f"  {ansi.BOLD}{ansi.MAGENTA}System:{ansi.RESET}")
    for line in SYSTEM_PROMPT.strip().split("\n"):
        print(f"    {ansi.DIM}{line}{ansi.RESET}")
    print()
    if not _messages:
        print(f"  {ansi.DIM}(No messages in context){ansi.RESET}\n")
        return
    for i, msg in enumerate(_messages):
        role = msg.get("role", "?")
        content = msg.get("content", "")
        # Role header with color
        if role == "user":
            header = f"{ansi.BOLD}{ansi.CYAN}User:{ansi.RESET}"
        elif role == "assistant":
            header = f"{ansi.BOLD}{ansi.YELLOW}Assistant:{ansi.RESET}"
        elif role == "tool":
            header = f"{ansi.DIM}Tool:{ansi.RESET}"
        else:
            header = f"{ansi.DIM}{role}:{ansi.RESET}"
        print(f"  {header}")
        # Content
        if role == "tool":
            preview = content[:200] + "..." if len(content) > 200 else content
            for line in preview.replace("\r", "").split("\n"):
                print(f"    {ansi.DIM}{line}{ansi.RESET}")
        elif content.strip():
            for line in content.strip().split("\n"):
                print(f"    {line}")
        # Tool calls (assistant message with tool_calls)
        if role == "assistant" and "tool_calls" in msg:
            for tc in msg["tool_calls"]:
                fn = tc.get("function", {})
                name = fn.get("name", "?")
                args = fn.get("arguments", "{}")
                args_preview = args[:80] + "..." if len(args) > 80 else args
                print(f"    {ansi.DIM}→ {name}({args_preview}){ansi.RESET}")
        print()
    print()


def _num_tokens_from_messages(messages: list) -> int:
    """Estimate token count for messages (system + chat). Uses o200k_base for GPT models."""
    try:
        encoding = tiktoken.encoding_for_model(MODEL)
    except KeyError:
        encoding = tiktoken.get_encoding("o200k_base")
    tokens_per_message = 3
    num_tokens = 0
    for msg in messages:
        num_tokens += tokens_per_message
        for key, value in msg.items():
            if isinstance(value, str):
                num_tokens += len(encoding.encode(value))
            elif value is not None:
                num_tokens += len(encoding.encode(str(value)))
    num_tokens += 3  # reply priming
    return num_tokens


def get_context_size() -> str:
    """Return token usage string: 'used / max (remaining)'."""
    messages = [{"role": "system", "content": SYSTEM_PROMPT}] + _messages
    used = _num_tokens_from_messages(messages)
    remaining = max(0, CONTEXT_WINDOW - used)
    return f"{used:,} / {CONTEXT_WINDOW:,} ({remaining:,} left)"


SYSTEM_PROMPT = """Code: wrap in ```language blocks; use correct tag (java, python, etc.); be concise when code-only requested.
write_code: use when user asks to write code to file. Java: class name = filename (HelloWorld1.java → "public class HelloWorld1"); one class per file. Path: omit if unspecified; if specified use relative (abc, abc/def); include path when user says directory/folder. Guess filename if needed (Java: Hello.java, Python: main.py).
update_code_section: use when updating only a section of an existing file between markers (e.g. # BEGIN_FOO / # END_FOO). Markers stay; content between them is replaced. Path: same as write_code.
run_program: use for run/execute/test (e.g. fruits, hello_world/HelloWorld1). Success: don't repeat output. Error: provide commentary, fix with write_code, run again—never ask "fix it?".
read_code: read a file and return contents for subsequent edits. list: view/show/display file contents. work: list work directory contents.
Arithmetic: process tool results mathematically; return final result to user.
Sequences/loops: execute fully; no confirmation or pausing. "N times in order A,B,C" = for each 1..N do A then B then C (not all A's then all B's). Complete each iteration before next; never batch. Never write multiple items before list+run of the first. Don't return until all N iterations done.
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
        print("*** Calling LLM ***")
        messages = [{"role": "system", "content": SYSTEM_PROMPT}] + _messages
        stream = openai.chat.completions.create(
            model=MODEL,
            messages=messages,
            stream=True,
            tools=tools,
            parallel_tool_calls=False
        )

        accumulated = {}
        try:
            print_llm_response_stream(_stream_and_accumulate(stream, accumulated), delay=0.02)
        except KeyboardInterrupt:
            print(f"\n{ansi.DIM}[Interrupted]{ansi.RESET}\n")
            return
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
