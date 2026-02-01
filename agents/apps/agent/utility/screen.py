import os
import sys

def clear_screen():
    """Clear the terminal (uses 'clear' on Unix/Git Bash, 'cls' on cmd/PowerShell)."""
    if sys.platform != "win32":
        os.system("clear")
    elif os.environ.get("TERM") or os.environ.get("MSYSTEM"):
        os.system("clear")  # Git Bash / MSYS
    else:
        os.system("cls")


def wait_key():
    """Wait for a keypress (any key on Windows, Enter elsewhere)."""
    if sys.platform == "win32":
        try:
            import msvcrt
            msvcrt.getch()
        except Exception:
            input()
    else:
        input()
