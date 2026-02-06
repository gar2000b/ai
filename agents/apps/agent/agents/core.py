"""Main command loop for the agent REPL."""

import os
import sys

import constants.ansi as ansi
from commands.command import _run_os_command, list_file, run_program
from prompt_toolkit.auto_suggest import AutoSuggestFromHistory
from prompt_toolkit.validation import Validator

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


def _prompt_continuation(width, line_number, *args):
    """Continuation prefix for multiline input (Ctrl+N for newline)."""
    return " " * width


def agent_loop():
    """Run the main command loop until the user exits."""
    # Enable modifyOtherKeys so Shift+Enter sends a distinct escape sequence (not same as Enter)
    sys.stdout.write("\x1b[>4;1m")
    sys.stdout.flush()
    non_empty = Validator.from_callable(
        lambda t: bool(t.strip()),
        error_message="",
        move_cursor_to_end=True,
    )
    while True:
        command = session.prompt(
            [("class:prompt", "Prompt: ")],
            auto_suggest=AutoSuggestFromHistory(),
            key_bindings=key_bindings,
            validator=non_empty,
            multiline=True,
            prompt_continuation=_prompt_continuation,
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
            # Examples (relative to images/): batman.png, subdir/photo.png, exports/2024/logo.png
            # Examples (absolute): /home/user/pics/x.png  |  C:\Users\me\Pictures\x.png
            print()
            parts = command.split(maxsplit=1)
            arg = parts[1].strip() if len(parts) > 1 else "batman.png"
            # Absolute path: use as-is. Else: relative to images/
            images_dir = os.path.join(agent_dir, "images")
            image_path = (
                os.path.abspath(arg)
                if os.path.isabs(arg)
                else os.path.normpath(os.path.join(images_dir, arg))
            )
            if os.path.exists(image_path):
                print_image_sixel(image_path)
            else:
                print(f"Image does not exist: {image_path}")
            print()
        elif command == "run" or command.startswith("run "):
            parts = command.split(maxsplit=1)
            if len(parts) < 2 or not parts[1].strip():
                print(f"\n{ansi.DIM}Usage: run <name|path> (e.g. run fruits, run work/Fibonacci){ansi.RESET}\n")
            else:
                base_name = parts[1].strip()
                work_dir = os.path.join(agent_dir, "work")
                run_program(work_dir, base_name)
        elif command == "list" or command.startswith("list "):
            # Examples (relative to work/): main.py, src/utils.py, pkg/sub/module.py
            # Examples (absolute): /home/user/code/app.py  |  C:\dev\project\main.py
            parts = command.split(maxsplit=1)
            if len(parts) < 2 or not parts[1].strip():
                print(f"\n{ansi.DIM}Usage: list <filename>{ansi.RESET}\n")
            else:
                arg = parts[1].strip()
                # Absolute path: use as-is. Else: relative to work/
                work_dir = os.path.join(agent_dir, "work")
                if os.path.isabs(arg):
                    file_path = os.path.abspath(arg)
                    list_file(os.path.dirname(file_path), os.path.basename(file_path))
                else:
                    full_path = os.path.normpath(os.path.join(work_dir, arg))
                    list_file(os.path.dirname(full_path), os.path.basename(full_path))
        elif command.startswith("!"):
            _run_os_command(command[1:].strip())
        else:
            # Free-form input: pass through to the LLM
            llm(command)
