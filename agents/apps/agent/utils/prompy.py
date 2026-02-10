"""Prompt session setup with persisted command history."""
import os

from prompt_toolkit import PromptSession
from prompt_toolkit.history import FileHistory
from prompt_toolkit.styles import Style

# Agent app directory (parent of utils/)
agent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
history_file = os.path.join(agent_dir, ".agent_history")
prompt_style = Style.from_dict({"prompt": "ansicyan", "prompt-tokens": "ansibrightblack"})
session = PromptSession(style=prompt_style, history=FileHistory(history_file))
