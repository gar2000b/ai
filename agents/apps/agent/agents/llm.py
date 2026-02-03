"""LLM integration (mocked for now)."""

import config.env
from dotenv import load_dotenv
from openai import OpenAI
from utils.screen import print_llm_response

openai = OpenAI(api_key=config.env.openai_api_key)

def llm(prompt: str) -> None:
    """Call the LLM with the given prompt. Currently a mock that prints to screen."""
    # print(f"OpenAI API Key: {config.env.openai_api_key}")
    request = prompt
    messages = [{"role": "user", "content": request}]
    response = openai.chat.completions.create(
        model="gpt-4.1-nano",
        messages=messages,
    )
    print_llm_response(response.choices[0].message.content)
