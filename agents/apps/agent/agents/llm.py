"""LLM integration (mocked for now)."""

import config.env
from openai import OpenAI

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
Use the correct language tag (java, python, javascript, etc.) so the code renders with syntax highlighting. Be concise—return the code with minimal prose when a code-only response is requested."""


def _stream_content(stream):
    """Yield content chunks from the API stream."""
    for chunk in stream:
        delta = chunk.choices[0].delta if chunk.choices else None
        if delta and getattr(delta, "content", None):
            yield delta.content


def llm(prompt: str) -> None:
    """Call the LLM with the given prompt; stream response and typewrite to screen; update history."""
    _messages.append({"role": "user", "content": prompt})
    messages = [{"role": "system", "content": SYSTEM_PROMPT}] + _messages
    stream = openai.chat.completions.create(
        model="gpt-4.1-nano",
        messages=messages,
        stream=True,
    )

    parts = []

    def stream_and_accumulate():
        for chunk in _stream_content(stream):
            parts.append(chunk)
            yield chunk

    print_llm_response_stream(stream_and_accumulate(), delay=0.02)
    full_response = "".join(parts)
    _messages.append({"role": "assistant", "content": full_response})
    