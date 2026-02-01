#!/usr/bin/env python3

import argparse
import constants.ansi as ansi
import os
import sys
import json
from prompt_toolkit import PromptSession
from prompt_toolkit.auto_suggest import AutoSuggestFromHistory
from prompt_toolkit.styles import Style
from utils.emoji import get_book_marker
from utils.screen import clear_screen, wait_key
from rich.console import Console
from rich.syntax import Syntax

# In-memory command history (up/down arrows)
# Use prompt_toolkit styling for the prompt (ANSI codes are not interpreted inside session.prompt)
prompt_style = Style.from_dict({"prompt": "bold ansimagenta"})
session = PromptSession(style=prompt_style)

def print_help():
    """Print the help message with available commands."""
    print(f"\n{ansi.DIM}help - Show this help message{ansi.RESET}")
    print(f"{ansi.DIM}exit - Exit the agent{ansi.RESET}")
    print(f"{ansi.DIM}clear - Clear the screen{ansi.RESET}")
    print(f"{ansi.DIM}exit - Exit the agent{ansi.RESET}\n")


def print_welcome():
    """Print the welcome message and command list."""
    print(f"{ansi.BOLD}{ansi.MAGENTA}Welcome to the AI Discovery Agent!{ansi.RESET}\n")
    print(f"{ansi.DIM}{ansi.RESET}{ansi.CYAN}This is an AI agent that will help you find better code for discovery jobs.{ansi.RESET}\n")
    print(f"{ansi.DIM}You can use the following commands:{ansi.RESET}")
    print_help()


def agent_loop():
    """Run the main command loop until the user exits."""
    while True:
        command = session.prompt(
            [("class:prompt", "Ask anything or Enter a command: ")],
            auto_suggest=AutoSuggestFromHistory(),
        )
        if command == "help":
            print_help()
        elif command == "exit":
            print(f"\n{ansi.DIM}Thanks for using the AI Discovery Agent.{ansi.RESET}")
            print(f"{ansi.BOLD}{ansi.MAGENTA}Goodbye!{ansi.RESET}\n")
            break
        elif command == "clear" or command == "cls":
            clear_screen()


def _parse_args():
    parser = argparse.ArgumentParser(
        description="AI Discovery Agent with command history and ANSI-colored output.",
    )
    parser.add_argument(
        "-de", "--disable-emojis",
        action="store_true",
        help="For example: uses a bullet (•) instead of emoji for list items.",
    )
    return parser.parse_args()


def main():
    """Entry point: clear screen, show welcome, run agent loop."""
    clear_screen()
    print_welcome()
    agent_loop()


if __name__ == "__main__":
    args = _parse_args()
    if args.disable_emojis:
        import utils.emoji as emoji_module
        emoji_module.bool_use_emoji = False
    main()
