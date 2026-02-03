import os
import sys

import constants.ansi as ansi


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


def print_help():
    """Print the help message with available commands."""
    commands = [
        ("help", "Show this help message"),
        ("clear", "Clear the screen"),
        (
            "work",
            "Lists the contents of the current work directory (in ASCII tree format)",
        ),
        ("reset", "Clear the screen and show the welcome page again"),
        ("!cmd", "Run an OS command via the shell escape sequence bang (!) (e.g. !pwd, !ls -hal)"),
        ("exit", "Exit the agent"),
    ]
    width = max(len(cmd) for cmd, _ in commands)
    print()
    for cmd, desc in commands:
        print(f"{ansi.DIM}{cmd:<{width}} - {desc}{ansi.RESET}")
    print()


def print_welcome():
    """Print the welcome message and command list."""
    welcome_text = " Welcome to AI Code (the discovery agent) "
    width = len(welcome_text)
    styled_text = f"{ansi.RESET}{ansi.RED} Welcome to {ansi.BOLD}AI Code{ansi.RESET}{ansi.RED} (the discovery agent) {ansi.BOLD}"
    border_top = f"{ansi.BOLD}{ansi.RED}╭{'─' * width}╮{ansi.RESET}"
    border_mid = f"{ansi.BOLD}{ansi.RED}│{styled_text}│{ansi.RESET}"
    border_bot = f"{ansi.BOLD}{ansi.RED}╰{'─' * width}╯{ansi.RESET}"
    print(f"\n{border_top}\n{border_mid}\n{border_bot}")

    logo = """
 █████╗ ██╗     ██████╗ ██████╗ ██████╗ ███████╗
██╔══██╗██║    ██╔════╝██╔═══██╗██╔══██╗██╔════╝
███████║██║    ██║     ██║   ██║██║  ██║█████╗  
██╔══██║██║    ██║     ██║   ██║██║  ██║██╔══╝  
██║  ██║██║    ╚██████╗╚██████╔╝██████╔╝███████╗
╚═╝  ╚═╝╚═╝     ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝
"""
    print(f"{ansi.BOLD}{ansi.RED}{logo}{ansi.RESET}")

    print(
        f"{ansi.DIM}{ansi.RESET}{ansi.CYAN}This is a terminal-based AI agent that will help you explore new code for discovery.{ansi.RESET}\n"
    )
    print(f"{ansi.DIM}You can use the following commands:{ansi.RESET}")
    print_help()


def print_goodbye():
    """Print the goodbye message (used for exit command and Ctrl+C)."""
    print(f"\n{ansi.DIM}Thanks for using the AI Discovery Agent.{ansi.RESET}")
    print(f"{ansi.BOLD}{ansi.RED}Goodbye!{ansi.RESET}\n")


def print_llm_response(prompt: str) -> None:
    """Print the LLM response line (e.g. mock 'LLM called: prompt')."""
    print(f"LLM called: {prompt}")


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


def print_work_directory(agent_dir: str):
    """Print the contents of the work directory (in ASCII tree format)."""
    work_dir = os.path.join(agent_dir, "work")
    print(f"\n{ansi.DIM}Contents of the current work directory:\n{ansi.RESET}")
    if not os.path.isdir(work_dir):
        print(f"{ansi.DIM}(work directory not found){ansi.RESET}\n")
        return
    work_name = os.path.basename(work_dir.rstrip(os.sep)) or "work"
    print(f"{ansi.BOLD}./{work_name}/{ansi.RESET}")
    for name, line_prefix in _tree_lines(work_dir):
        print(f"{ansi.DIM}{line_prefix}{ansi.RESET}{name}")
    print()


# Map file extension -> Pygments lexer name for syntax-highlighted cat/type output
_EXTENSION_LEXER = {
    ".py": "python",
    ".pyw": "python",
    ".json": "json",
    ".js": "javascript",
    ".mjs": "javascript",
    ".ts": "typescript",
    ".html": "html",
    ".htm": "html",
    ".css": "css",
    ".scss": "scss",
    ".md": "markdown",
    ".yaml": "yaml",
    ".yml": "yaml",
    ".xml": "xml",
    ".sh": "bash",
    ".bash": "bash",
    ".bat": "batch",
    ".ps1": "powershell",
    ".rb": "ruby",
    ".go": "go",
    ".rs": "rust",
    ".java": "java",
    ".c": "c",
    ".h": "c",
    ".cpp": "cpp",
    ".hpp": "cpp",
    ".cc": "cpp",
    ".sql": "sql",
    ".r": "r",
    ".R": "r",
}


def lexer_for_display_command(cmd: str) -> str | None:
    """If cmd is cat/type with a file path, return Pygments lexer name from extension; else None."""
    parts = cmd.strip().split()
    if not parts or parts[0].lower() not in ("cat", "type"):
        return None
    for part in parts[1:]:
        if not part.startswith("-"):
            ext = os.path.splitext(part)[1].lower()
            return _EXTENSION_LEXER.get(ext, "text")
    return "text"
