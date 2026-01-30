"""Detect whether the terminal is likely to render emoji and provide a book list marker."""

import os

bool_use_emoji = True

def terminal_likely_supports_emoji() -> bool:
    """Guess if the terminal can render emoji (Cursor, VS Code, Windows Terminal, modern xterm)."""
    if not bool_use_emoji:
        return False
    env = os.environ
    explicit = env.get("BOOKS_USE_EMOJI", "").strip().lower()
    if explicit in ("0", "false", "no"):
        return False
    if explicit in ("1", "true", "yes"):
        return True
    if env.get("CONEMU") or env.get("CONEMU_BUILD"):
        return False
    if env.get("TERM_PROGRAM", "").lower() in ("cursor", "vscode"):
        return True
    if env.get("WT_SESSION"):
        return True
    term = env.get("TERM", "").lower()
    if "xterm" in term or "tmux" in term or "screen" in term:
        return True
    return False


def get_book_marker() -> str:
    """Return 📚 if emoji are enabled and supported, else •."""
    return "📚" if terminal_likely_supports_emoji() else "•"
