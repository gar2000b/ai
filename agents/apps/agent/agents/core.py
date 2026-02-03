"""Main command loop for the agent REPL."""

from commands.command import _run_os_command
from prompt_toolkit.auto_suggest import AutoSuggestFromHistory

from agents.llm import llm
from utils.bindings import key_bindings
from utils.prompy import agent_dir, session
from utils.screen import (
    clear_screen,
    print_goodbye,
    print_help,
    print_welcome,
    print_work_directory,
)


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
            print_work_directory(agent_dir)
        elif command == "exit":
            print_goodbye()
            break
        elif command == "clear" or command == "cls":
            clear_screen()
        elif command == "reset":
            clear_screen()
            print_welcome()
        elif command.startswith("!"):
            _run_os_command(command[1:].strip())
        else:
            # Free-form input: pass through to the LLM
            llm(command)
