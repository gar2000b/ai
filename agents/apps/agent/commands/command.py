"""Run shell commands and display output (e.g. !ls, !cat file.py)."""
import os
import subprocess

import constants.ansi as ansi
from rich.console import Console
from rich.syntax import Syntax

from utils.screen import lexer_for_display_command, lexer_for_file_path


def _run_os_command(cmd: str) -> None:
    """Run a shell command and print stdout/stderr to the screen."""
    if not cmd:
        return
    print()
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
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
    except Exception as e:
        print(f"{ansi.DIM}Error: {e}{ansi.RESET}")
    print()


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
