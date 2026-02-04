"""Main command loop for the agent REPL."""

import os

from commands.command import _run_os_command
from prompt_toolkit.auto_suggest import AutoSuggestFromHistory

from agents.llm import llm
from utils.bindings import key_bindings
from utils.prompy import agent_dir, session
from utils.screen import (
    clear_screen,
    print_goodbye,
    print_help,
    print_image_sixel,
    print_images_directory,
    print_welcome,
    print_work_directory,
)


def agent_loop():
    """Run the main command loop until the user exits."""
    while True:
        command = session.prompt(
            [("class:prompt", "Prompt: ")],
            auto_suggest=AutoSuggestFromHistory(),
            key_bindings=key_bindings,
        )
        if command == "help":
            print_help()
        elif command == "work":
            print(f"\nWork directory: {os.path.join(agent_dir, 'work')}")
            print_work_directory(agent_dir)
        elif command == "images":
            print(f"\nImages directory: {os.path.join(agent_dir, 'images')}")
            print_images_directory(agent_dir)
        elif command == "exit":
            print_goodbye()
            break
        elif command == "clear" or command == "cls":
            clear_screen()
        elif command == "reset":
            clear_screen()
            print_welcome()
        elif command == "image" or command.startswith("image "):
            print()
            parts = command.split(maxsplit=1)
            arg = parts[1].strip() if len(parts) > 1 else "batman.png"
            # Path if absolute or contains directory separators; else filename in images/
            is_path = os.path.isabs(arg) or "/" in arg or os.sep in arg
            image_path = os.path.abspath(arg) if is_path else os.path.join(agent_dir, "images", arg)
            if os.path.exists(image_path):
                print_image_sixel(image_path)
            else:
                print(f"Image does not exist: {image_path}")
            print()
        elif command.startswith("!"):
            _run_os_command(command[1:].strip())
        else:
            # Free-form input: pass through to the LLM
            llm(command)
