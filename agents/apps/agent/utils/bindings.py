"""Key bindings for the prompt session (e.g. Ctrl+L to clear screen, Ctrl+E to reset)."""
import os

# Remap Shift+Enter: prompt_toolkit maps \x1b[27;2;13~ to Enter; we remap to ControlJ
# so our c-j binding catches it and inserts newline (Enter stays as accept).
from prompt_toolkit.input import ansi_escape_sequences
from prompt_toolkit.keys import Keys

ansi_escape_sequences.ANSI_SEQUENCES["\x1b[27;2;13~"] = Keys.ControlJ

from prompt_toolkit.application.current import get_app
from prompt_toolkit.application.run_in_terminal import run_in_terminal
from prompt_toolkit.filters import Condition
from prompt_toolkit.key_binding import KeyBindings

from utils.screen import clear_screen, print_goodbye, print_welcome

key_bindings = KeyBindings()


@Condition
def _suggestion_available() -> bool:
    app = get_app()
    return (
        app.current_buffer.suggestion is not None
        and len(app.current_buffer.suggestion.text) > 0
        and app.current_buffer.document.is_cursor_at_the_end
    )


@key_bindings.add("c-l")
def _clear_screen(_event):
    # Same clear_screen() as clear/cls; run_in_terminal so prompt redraws after
    run_in_terminal(clear_screen)


def _reset():
    clear_screen()
    print_welcome()


@key_bindings.add("c-e", filter=~_suggestion_available)
def _reset_binding(_event):
    # Same as reset command; run_in_terminal so prompt redraws after
    run_in_terminal(_reset)
    # """Ctrl+X exits the app (same as typing exit and Enter)."""
    # _event.current_buffer.text = "exit"
    # _event.current_buffer.validate_and_handle()


@key_bindings.add("tab", filter=_suggestion_available)
def _accept_suggestion(event):
    """Accept auto-suggestion on Tab (same as right arrow)."""
    b = event.current_buffer
    if b.suggestion:
        b.insert_text(b.suggestion.text)


@key_bindings.add("c-c")
def _clear_prompt(event):
    """Ctrl+C clears the current input (does not exit)."""
    event.current_buffer.reset()


@key_bindings.add("c-z")
def _exit_app(_event):
    """Ctrl+X exits the app immediately (works at prompt; use Ctrl+C when blocked)."""
    print_goodbye()
    os._exit(0)


@key_bindings.add("c-n")
def _newline(event):
    """Ctrl+N inserts a newline."""
    event.current_buffer.insert_text("\n")


@key_bindings.add("enter")
def _enter_accept(event):
    """Enter accepts/submits the input (overrides multiline default of newline)."""
    event.current_buffer.validate_and_handle()
