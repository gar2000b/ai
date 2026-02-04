"""LLM integration (mocked for now)."""

import config.env
from openai import OpenAI

from utils.screen import print_llm_response_stream

openai = OpenAI(api_key=config.env.openai_api_key)


def _stream_content(stream):
    """Yield content chunks from the API stream."""
    for chunk in stream:
        delta = chunk.choices[0].delta if chunk.choices else None
        if delta and getattr(delta, "content", None):
            yield delta.content


def llm(prompt: str) -> None:
    """Call the LLM with the given prompt; stream response and typewrite to screen."""
    messages = [{"role": "user", "content": prompt}]
    stream = openai.chat.completions.create(
        model="gpt-4.1-nano",
        messages=messages,
        stream=True,
    )
    print_llm_response_stream(_stream_content(stream), delay=0.02)