"""Key bindings for the prompt session (e.g. Ctrl+L to clear screen, Ctrl+R to reset)."""
from prompt_toolkit.application.run_in_terminal import run_in_terminal
from prompt_toolkit.key_binding import KeyBindings

from utils.screen import clear_screen, print_welcome

key_bindings = KeyBindings()


@key_bindings.add("c-l")
def _clear_screen(_event):
    # Same clear_screen() as clear/cls; run_in_terminal so prompt redraws after
    run_in_terminal(clear_screen)


def _reset():
    clear_screen()
    print_welcome()


@key_bindings.add("c-r")
def _reset_binding(_event):
    # Same as reset command; run_in_terminal so prompt redraws after
    run_in_terminal(_reset)

