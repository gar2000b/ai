import os
import sys
import time
from io import StringIO
from itertools import chain

import constants.ansi as ansi
from rich.console import Console
from rich.syntax import Syntax


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


def print_image_sixel(path: str, max_width: int = 200) -> None:
    """Render an image in the terminal using Sixel (real pixels; requires Sixel-capable terminal).
    Resizes large images to avoid Windows Terminal buffer limits."""
    if sys.platform == "win32":
        # sixel.cellsize imports termios (Unix-only); mock it so sixel can load on Windows
        import types

        _termios = types.ModuleType("termios")
        _termios.tcgetattr = lambda fd: None
        _termios.tcsetattr = lambda fd, opt, attr: None
        _termios.TCSANOW = 0
        _termios.TCSAFLUSH = 1
        _termios.ECHO = 1
        _termios.ICANON = 2
        sys.modules["termios"] = _termios

    from io import BytesIO

    from PIL import Image
    from sixel.sixel import SixelWriter

    img = Image.open(path).convert("RGB")
    if img.width > max_width:
        ratio = max_width / img.width
        new_size = (max_width, int(img.height * ratio))
        img = img.resize(new_size, Image.Resampling.LANCZOS)
    buf = BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)

    # Use body_only=False for full Sixel, but disable save/restore cursor (DECSC/DECRC)
    # which can cause Windows Terminal to clear the image after display
    writer = SixelWriter(body_only=False)
    writer.save_position = lambda out: None
    writer.restore_position = lambda out: None
    writer.draw(buf, output=sys.stdout)
    print()  # newline so prompt appears below the image
    sys.stdout.flush()


def print_help():
    """Print the help message with available commands."""
    commands = [
        ("help", "Show this help message"),
        ("clear", "Clear the screen"),
        (
            "image [file|path]",
            "Display an image (default: batman.png; paths relative to images/ or absolute; Sixel)",
        ),
        (
            "list <filename>",
            "Display a file with syntax highlighting (paths relative to work/ or absolute)",
        ),
        (
            "run <name|path>",
            "Compile (Java) and run a program; paths relative to work/ or absolute (e.g. run fruits, run work/Fibonacci)",
        ),
        ("Ctrl+N", "Newline in prompt (multiline); Enter submits"),
        (
            "work",
            "Lists the contents of the current work directory (in ASCII tree format)",
        ),
        ("images", "Lists the contents of the images directory (in ASCII tree format)"),
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
    print(
        f"{ansi.DIM}{ansi.RESET}{ansi.CYAN}You can ask anything, enter a command or type help to get started.{ansi.RESET}\n"
    )


def print_goodbye():
    """Print the goodbye message (used for exit command and Ctrl+C)."""
    print(f"\n{ansi.DIM}Thanks for using the AI Discovery Agent.{ansi.RESET}")
    print(f"{ansi.BOLD}{ansi.RED}Goodbye!{ansi.RESET}\n")


def print_llm_response(prompt: str) -> None:
    """Print the LLM response line (e.g. mock 'LLM called: prompt')."""
    print(f"\n{ansi.BOLD}{ansi.RED}LLM Response: {ansi.RESET}{ansi.WHITE}{prompt}{ansi.RESET}\n")


# Markdown fence language -> Pygments lexer name for syntax-highlighted code blocks
_MARKDOWN_LANG_LEXER = {
    "python": "python",
    "py": "python",
    "java": "java",
    "javascript": "javascript",
    "js": "javascript",
    "typescript": "typescript",
    "ts": "typescript",
    "html": "html",
    "css": "css",
    "json": "json",
    "yaml": "yaml",
    "yml": "yaml",
    "xml": "xml",
    "bash": "bash",
    "sh": "bash",
    "powershell": "powershell",
    "ps1": "powershell",
    "ruby": "ruby",
    "rb": "ruby",
    "go": "go",
    "rust": "rust",
    "rs": "rust",
    "c": "c",
    "cpp": "cpp",
    "sql": "sql",
    "r": "r",
    "python3": "python",
    "c++": "cpp",
}


def _parse_stream_for_code_blocks(stream):
    """Yield ('text', str) or ('code', str, str) from a stream, detecting markdown fenced code blocks."""
    buffer = ""
    state = "normal"  # normal | code_fence | code_block
    code_lang = "text"

    for chunk in stream:
        if chunk:
            buffer += chunk

        while buffer:
            if state == "normal":
                idx = buffer.find("```")
                if idx == -1:
                    # Emit all but last 2 chars (might be start of ```)
                    emit_len = max(0, len(buffer) - 2)
                    if emit_len > 0:
                        yield ("text", buffer[:emit_len])
                        buffer = buffer[emit_len:]
                    break
                yield ("text", buffer[:idx])
                buffer = buffer[idx + 3 :]
                state = "code_fence"  # wait for lang + newline before code_block

            elif state == "code_fence":
                # Must see newline to get language; hold back until we have it
                newline = buffer.find("\n")
                if newline == -1:
                    break  # need more chunks
                lang_part = buffer[:newline].strip().lower() or "text"
                code_lang = _MARKDOWN_LANG_LEXER.get(lang_part, "text")
                buffer = buffer[newline + 1 :]
                state = "code_block"

            elif state == "code_block":
                idx = buffer.find("```")
                if idx == -1:
                    break  # need more chunks to find closing fence
                yield ("code", buffer[:idx], code_lang)
                buffer = buffer[idx + 3 :]
                state = "normal"

    if buffer and state == "code_block":
        yield ("code", buffer, code_lang)
    elif buffer:
        yield ("text", buffer)


def print_llm_response_stream(stream, delay: float = 0.02) -> None:
    """Consume a stream; typewrite text, render markdown code blocks with syntax highlighting."""
    parsed = _parse_stream_for_code_blocks(stream)
    try:
        first_segment = next(parsed)
    except StopIteration:
        return
    print(f"\n{ansi.BOLD}{ansi.RED}LLM Response: {ansi.RESET}{ansi.WHITE}", end="", flush=True)
    last_was_text = True
    for segment in chain([first_segment], parsed):
        if segment[0] == "text":
            last_was_text = True
            for char in segment[1]:
                print(char, end="", flush=True)
                time.sleep(delay)
        else:
            last_was_text = False
            _, code, lexer = segment
            if code.strip():
                print("\n\n", end="")
                try:
                    buf = StringIO()
                    Console(file=buf, force_terminal=True).print(
                        Syntax(
                            code.rstrip(),
                            lexer,
                            theme="monokai",
                            background_color="default",
                        )
                    )
                    out = buf.getvalue().rstrip() + "\n"
                    for char in out:
                        print(char, end="", flush=True)
                        time.sleep(0.001)
                except Exception:
                    for char in code:
                        print(char, end="", flush=True)
                        time.sleep(0.001)
    print(f"{ansi.RESET}\n" if last_was_text else f"{ansi.RESET}")


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


def print_images_directory(agent_dir: str):
    """Print the contents of the images directory (in ASCII tree format)."""
    images_dir = os.path.join(agent_dir, "images")
    print(f"\n{ansi.DIM}Contents of the images directory:\n{ansi.RESET}")
    if not os.path.isdir(images_dir):
        print(f"{ansi.DIM}(images directory not found){ansi.RESET}\n")
        return
    dir_name = os.path.basename(images_dir.rstrip(os.sep)) or "images"
    print(f"{ansi.BOLD}./{dir_name}/{ansi.RESET}")
    for name, line_prefix in _tree_lines(images_dir):
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


def lexer_for_file_path(file_path: str) -> str:
    """Return Pygments lexer name from file extension; default to 'text'."""
    ext = os.path.splitext(file_path)[1].lower()
    return _EXTENSION_LEXER.get(ext, "text")


def lexer_for_display_command(cmd: str) -> str | None:
    """If cmd is cat/type with a file path, return Pygments lexer name from extension; else None."""
    parts = cmd.strip().split()
    if not parts or parts[0].lower() not in ("cat", "type"):
        return None
    for part in parts[1:]:
        if not part.startswith("-"):
            return lexer_for_file_path(part)
    return "text"
