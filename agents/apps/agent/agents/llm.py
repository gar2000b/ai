"""LLM integration (mocked for now)."""

from dotenv import load_dotenv
from openai import OpenAI
from utils.screen import print_llm_response


def llm(prompt: str) -> None:
    """Call the LLM with the given prompt. Currently a mock that prints to screen."""
    print_llm_response(prompt)
