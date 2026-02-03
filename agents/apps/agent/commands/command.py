"""Run shell commands and display output (e.g. !ls, !cat file.py)."""
import subprocess

import constants.ansi as ansi
from rich.console import Console
from rich.syntax import Syntax

from utils.screen import lexer_for_display_command


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
