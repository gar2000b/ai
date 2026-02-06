"""Run shell commands and display output (e.g. !ls, !cat file.py)."""
import os
import subprocess

import constants.ansi as ansi
from rich.console import Console
from rich.syntax import Syntax

from utils.screen import lexer_for_display_command, lexer_for_file_path


def _run_os_command(cmd: str, cwd: str | None = None) -> int:
    """Run a shell command and print stdout/stderr to the screen. Returns exit code."""
    if not cmd:
        return 0
    print()
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            cwd=cwd,
        )
        if result.stdout:
            lexer = lexer_for_display_command(cmd)
            if lexer is not None:
                Console().print(
                    Syntax(
                        result.stdout.rstrip(),
                        lexer,
                        theme="monokai",
                        background_color="default",
                    )
                )
            else:
                print(result.stdout, end="" if result.stdout.endswith("\n") else "\n")
        if result.stderr:
            print(result.stderr, end="" if result.stderr.endswith("\n") else "\n")
        if result.returncode != 0 and not result.stderr:
            print(f"{ansi.DIM}(exit code {result.returncode}){ansi.RESET}")
        print()
        return result.returncode
    except Exception as e:
        print(f"{ansi.DIM}Error: {e}{ansi.RESET}")
        print()
        return -1


def _find_program_file(work_dir: str, base_name: str) -> str | None:
    """Search work_dir recursively for base_name.py or base_name.java; prefer .py."""
    java_path = None
    for root, _, files in os.walk(work_dir):
        for name in files:
            if name == f"{base_name}.py":
                return os.path.join(root, name)
            if name == f"{base_name}.java":
                java_path = java_path or os.path.join(root, name)
    return java_path


def _find_program_by_path(work_dir: str, arg: str) -> str | None:
    """Resolve path (relative to work_dir or absolute) and find .py or .java; prefer .py."""
    if os.path.isabs(arg):
        search_dir = os.path.dirname(arg)
        base_name = os.path.basename(arg)
    else:
        full = os.path.normpath(os.path.join(work_dir, arg))
        search_dir = os.path.dirname(full)
        base_name = os.path.basename(full)
    for ext in (".py", ".java"):
        path = os.path.join(search_dir, base_name + ext)
        if os.path.isfile(path):
            return path
    return None


def run_program(work_dir: str, arg: str) -> None:
    """Compile (if Java) and run a program by name or path (e.g. fruits, work/Fibonacci)."""
    if os.path.sep in arg or (os.path.altsep and os.path.altsep in arg) or os.path.isabs(arg):
        file_path = _find_program_by_path(work_dir, arg)
    else:
        file_path = _find_program_file(work_dir, arg)
    if not file_path:
        print(f"\n{ansi.DIM}No file found: {arg}.py or {arg}.java{ansi.RESET}\n")
        return
    file_dir = os.path.dirname(file_path)
    file_name = os.path.basename(file_path)
    base_name = os.path.splitext(file_name)[0]
    if file_path.endswith(".py"):
        _run_os_command(f"python {file_name}", cwd=file_dir)
    elif file_path.endswith(".java"):
        if _run_os_command(f"javac {file_name}", cwd=file_dir) == 0:
            _run_os_command(f"java -cp . {base_name}", cwd=file_dir)
    else:
        print(f"\n{ansi.DIM}Unsupported extension: {file_path}{ansi.RESET}\n")


def list_file(path: str, file_name: str) -> None:
    """Read and print a file with syntax highlighting based on file extension."""
    file_path = os.path.join(path, file_name)
    if not os.path.exists(file_path):
        print(f"\n{ansi.DIM}File not found: {file_path}{ansi.RESET}\n")
        return
    if not os.path.isfile(file_path):
        print(f"\n{ansi.DIM}Not a file: {file_path}{ansi.RESET}\n")
        return
    print()
    try:
        with open(file_path, encoding="utf-8", errors="replace") as f:
            content = f.read()
        lexer = lexer_for_file_path(file_path)
        Console().print(
            Syntax(
                content.rstrip(),
                lexer,
                theme="monokai",
                background_color="default",
            )
        )
    except Exception as e:
        print(f"{ansi.DIM}Error: {e}{ansi.RESET}")
    print()
