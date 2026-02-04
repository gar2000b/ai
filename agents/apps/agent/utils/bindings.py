"""Key bindings for the prompt session (e.g. Ctrl+L to clear screen, Ctrl+R to reset)."""
from prompt_toolkit.application.current import get_app
from prompt_toolkit.application.run_in_terminal import run_in_terminal
from prompt_toolkit.filters import Condition
from prompt_toolkit.key_binding import KeyBindings

from utils.screen import clear_screen, print_welcome

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


@key_bindings.add("tab", filter=_suggestion_available)
def _accept_suggestion(event):
    """Accept auto-suggestion on Tab (same as right arrow)."""
    b = event.current_buffer
    if b.suggestion:
        b.insert_text(b.suggestion.text)

