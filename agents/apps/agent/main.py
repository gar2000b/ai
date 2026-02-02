#!/usr/bin/env python3

import argparse
import constants.ansi as ansi
import os
import subprocess
import sys
import json
import utils.emoji as emoji_module
from prompt_toolkit import PromptSession
from prompt_toolkit.application.run_in_terminal import run_in_terminal
from prompt_toolkit.auto_suggest import AutoSuggestFromHistory
from prompt_toolkit.history import FileHistory
from prompt_toolkit.key_binding import KeyBindings
from prompt_toolkit.styles import Style
from utils.emoji import get_book_marker
from utils.screen import clear_screen, wait_key
from rich.console import Console
from rich.syntax import Syntax

# Command history: persisted to file so up/down arrows work across sessions
# Use prompt_toolkit styling for the prompt (ANSI codes are not interpreted inside session.prompt)
_agent_dir = os.path.dirname(os.path.abspath(__file__))
_history_file = os.path.join(_agent_dir, ".agent_history")
prompt_style = Style.from_dict({"prompt": "bold ansimagenta"})
session = PromptSession(style=prompt_style, history=FileHistory(_history_file))

# Ctrl+L clears the screen (like clear / cls command)
key_bindings = KeyBindings()


@key_bindings.add("c-l")
def _clear_screen(_event):
    # Same clear_screen() as clear/cls; run_in_terminal so prompt redraws after
    run_in_terminal(clear_screen)


def print_help():
    """Print the help message with available commands."""
    commands = [
        ("help", "Show this help message"),
        ("clear", "Clear the screen"),
        ("work", "lists the contents of the current work directory (in ASCII tree format)"),
        ("exit", "Exit the agent"),
    ]
    width = max(len(cmd) for cmd, _ in commands)
    print()
    for cmd, desc in commands:
        print(f"{ansi.DIM}{cmd:<{width}} - {desc}{ansi.RESET}")
    print()


def print_welcome():
    """Print the welcome message and command list."""
    print(f"{ansi.BOLD}{ansi.MAGENTA}Welcome to the AI Discovery Agent!{ansi.RESET}\n")
    print(
        f"{ansi.DIM}{ansi.RESET}{ansi.CYAN}This is an AI agent that will help you explore new code for discovery jobs.{ansi.RESET}\n"
    )
    print(f"{ansi.DIM}You can use the following commands:{ansi.RESET}")
    print_help()



def print_goodbye():
    """Print the goodbye message (used for exit command and Ctrl+C)."""
    print(f"\n{ansi.DIM}Thanks for using the AI Discovery Agent.{ansi.RESET}")
    print(f"{ansi.BOLD}{ansi.MAGENTA}Goodbye!{ansi.RESET}\n")

def _tree_lines(root_path, prefix=""):
    """Yield (display_name, line_prefix) for each entry in an ASCII tree."""
    try:
        entries = []
        for name in os.listdir(root_path):
            path = os.path.join(root_path, name)
            entries.append((name, path, os.path.isdir(path)))
        entries.sort(key=lambda x: (not x[2], x[0].lower()))
    except OSError:
        return
    for i, (name, path, is_dir) in enumerate(entries):
        is_last = i == len(entries) - 1
        connector = "└── " if is_last else "├── "
        yield name + ("/" if is_dir else ""), prefix + connector
        if is_dir:
            extension = "    " if is_last else "│   "
            yield from _tree_lines(path, prefix + extension)


def print_work_directory():
    """Print the contents of the current work directory (in ASCII tree format)."""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    work_dir = os.path.join(base_dir, "work")
    print(f"\n{ansi.DIM}Contents of the current work directory:\n{ansi.RESET}")
    if not os.path.isdir(work_dir):
        print(f"{ansi.DIM}(work directory not found){ansi.RESET}\n")
        return
    work_name = os.path.basename(work_dir.rstrip(os.sep)) or "work"
    print(f"{ansi.BOLD}{work_name}/{ansi.RESET}")
    for name, line_prefix in _tree_lines(work_dir):
        print(f"{ansi.DIM}{line_prefix}{ansi.RESET}{name}")
    print()

# Map file extension -> Pygments lexer name for syntax-highlighted cat/type output
_EXTENSION_LEXER = {
    ".py": "python", ".pyw": "python",
    ".json": "json", ".js": "javascript", ".mjs": "javascript", ".ts": "typescript",
    ".html": "html", ".htm": "html", ".css": "css", ".scss": "scss",
    ".md": "markdown", ".yaml": "yaml", ".yml": "yaml", ".xml": "xml",
    ".sh": "bash", ".bash": "bash", ".bat": "batch", ".ps1": "powershell",
    ".rb": "ruby", ".go": "go", ".rs": "rust", ".java": "java",
    ".c": "c", ".h": "c", ".cpp": "cpp", ".hpp": "cpp", ".cc": "cpp",
    ".sql": "sql", ".r": "r", ".R": "r",
}


def _lexer_for_display_command(cmd: str) -> str | None:
    """If cmd is cat/type with a file path, return Pygments lexer name from extension; else None."""
    parts = cmd.strip().split()
    if not parts or parts[0].lower() not in ("cat", "type"):
        return None
    for part in parts[1:]:
        if not part.startswith("-"):
            ext = os.path.splitext(part)[1].lower()
            return _EXTENSION_LEXER.get(ext, "text")
    return "text"


def _run_os_command(cmd: str) -> None:
    """Run a shell command and print stdout/stderr to the screen."""
    if not cmd:
        return
    print()
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
        )
        if result.stdout:
            lexer = _lexer_for_display_command(cmd)
            if lexer is not None:
                Console().print(
                    Syntax(result.stdout.rstrip(), lexer, theme="monokai", background_color="default")
                )
            else:
                print(result.stdout, end="" if result.stdout.endswith("\n") else "\n")
        if result.stderr:
            print(result.stderr, end="" if result.stderr.endswith("\n") else "\n")
        if result.returncode != 0 and not result.stderr:
            print(f"{ansi.DIM}(exit code {result.returncode}){ansi.RESET}")
    except Exception as e:
        print(f"{ansi.DIM}Error: {e}{ansi.RESET}")
    print()


def agent_loop():
    """Run the main command loop until the user exits."""
    while True:
        command = session.prompt(
            [("class:prompt", "Ask anything or Enter a command: ")],
            auto_suggest=AutoSuggestFromHistory(),
            key_bindings=key_bindings,
        )
        if command == "help":
            print_help()
        elif command == "work":
            print_work_directory()
        elif command == "exit":
            print_goodbye()
            break
        elif command == "clear" or command == "cls":
            clear_screen()
        elif command.startswith("!"):
            _run_os_command(command[1:].strip())
        else:
            # Unknown command or free-form input (could be a question for the agent later)
            pass


def _parse_args():
    parser = argparse.ArgumentParser(
        description="AI Discovery Agent with command history and ANSI-colored output.",
    )
    parser.add_argument(
        "-de",
        "--disable-emojis",
        action="store_true",
        help="For example: uses a bullet (•) instead of emoji for list items.",
    )
    return parser.parse_args()


def main():
    """Entry point: clear screen, show welcome, run agent loop."""
    clear_screen()
    print_welcome()
    try:
        agent_loop()
    except KeyboardInterrupt:
        print_goodbye()


if __name__ == "__main__":
    args = _parse_args()
    if args.disable_emojis:
        emoji_module.bool_use_emoji = False
    main()
