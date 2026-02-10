"""Run shell commands and display output (e.g. !ls, !cat file.py)."""
import os
import re
import subprocess

import constants.ansi as ansi
from rich.console import Console
from rich.syntax import Syntax

from utils.screen import lexer_for_display_command, lexer_for_file_path


def _run_os_command(cmd: str, cwd: str | None = None, stdout: bool = True) -> dict:
    """Run a shell command, optionally print stdout/stderr to the screen, and return {stdout, stderr, returncode}."""
    empty = {"stdout": "", "stderr": "", "returncode": 0}
    if not cmd:
        return empty
    if stdout:
        print()
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            cwd=cwd,
        )
        out = {"stdout": result.stdout or "", "stderr": result.stderr or "", "returncode": result.returncode}
        if stdout:
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
        return out
    except Exception as e:
        err_msg = str(e)
        if stdout:
            print(f"{ansi.DIM}Error: {err_msg}{ansi.RESET}")
            print()
        return {"stdout": "", "stderr": err_msg, "returncode": -1}


def _find_program_file(work_dir: str, arg: str) -> str | None:
    """Search work_dir recursively for arg, arg.py, or arg.java; prefer .py when no extension given."""
    if arg.endswith(".py") or arg.endswith(".java"):
        for root, _, files in os.walk(work_dir):
            if os.path.basename(arg) in files:
                return os.path.join(root, os.path.basename(arg))
        return None
    java_path = None
    for root, _, files in os.walk(work_dir):
        for name in files:
            if name == f"{arg}.py":
                return os.path.join(root, name)
            if name == f"{arg}.java":
                java_path = java_path or os.path.join(root, name)
    return java_path


def _find_program_by_path(work_dir: str, arg: str) -> str | None:
    """Resolve path (relative to work_dir or absolute) and find .py or .java; prefer .py."""
    if os.path.isabs(arg):
        full = os.path.normpath(arg)
    else:
        full = os.path.normpath(os.path.join(work_dir, arg))
    search_dir = os.path.dirname(full)
    base_name = os.path.basename(full)
    if base_name.endswith(".py") or base_name.endswith(".java"):
        path = os.path.join(search_dir, base_name)
        return path if os.path.isfile(path) else None
    for ext in (".py", ".java"):
        path = os.path.join(search_dir, base_name + ext)
        if os.path.isfile(path):
            return path
    return None


def run_program(work_dir: str, arg: str) -> dict:
    """Compile (if Java) and run a program by name or path. Returns {success, stdout, stderr, returncode, error?}."""
    if os.path.sep in arg or (os.path.altsep and os.path.altsep in arg) or os.path.isabs(arg):
        file_path = _find_program_by_path(work_dir, arg)
    else:
        file_path = _find_program_file(work_dir, arg)
    if not file_path:
        err = f"No file found: {arg}"
        print(f"\n{ansi.DIM}{err}{ansi.RESET}\n")
        return {"success": False, "error": err, "stdout": "", "stderr": "", "returncode": -1}
    file_dir = os.path.dirname(file_path)
    file_name = os.path.basename(file_path)
    base_name = os.path.splitext(file_name)[0]
    if file_path.endswith(".py"):
        r = _run_os_command(f"python {file_name}", cwd=file_dir)
        return {"success": True, "arg": arg, "stdout": r["stdout"], "stderr": r["stderr"], "returncode": r["returncode"]}
    elif file_path.endswith(".java"):
        javac_r = _run_os_command(f"javac {file_name}", cwd=file_dir, stdout=False)
        if javac_r["returncode"] != 0:
            return {"success": False, "arg": arg, "stdout": javac_r["stdout"], "stderr": javac_r["stderr"], "returncode": javac_r["returncode"]}
        java_r = _run_os_command(f"java -cp . {base_name}", cwd=file_dir)
        return {"success": True, "arg": arg, "stdout": java_r["stdout"], "stderr": java_r["stderr"], "returncode": java_r["returncode"]}
    else:
        err = f"Unsupported extension: {file_path}"
        print(f"\n{ansi.DIM}{err}{ansi.RESET}\n")
        return {"success": False, "error": err, "stdout": "", "stderr": "", "returncode": -1}


def clone_java(source_file_name: str, target_file_name: str, path: str = "") -> dict:
    """Clone a Java file and replace class name occurrences.
    Copies source_file_name to target_file_name, then replaces all occurrences of the original
    class name (filename minus .java) with the new class name (target filename minus .java)."""
    from utils.prompy import agent_dir
    
    work_dir = os.path.join(agent_dir, "work")
    if not path or not path.strip():
        target_dir = work_dir
    else:
        normalized_path = path.strip().replace("\\", "/")
        target_dir = os.path.normpath(os.path.join(work_dir, normalized_path))
    
    source_path = os.path.join(target_dir, source_file_name)
    target_path = os.path.join(target_dir, target_file_name)
    
    if not os.path.isfile(source_path):
        return {"success": False, "error": f"Source file not found: {source_path}"}
    
    if not source_file_name.endswith(".java"):
        return {"success": False, "error": f"Source file must be a .java file: {source_file_name}"}
    
    if not target_file_name.endswith(".java"):
        return {"success": False, "error": f"Target file must be a .java file: {target_file_name}"}
    
    # Extract class names (basename without .java extension)
    source_class_name = os.path.splitext(os.path.basename(source_file_name))[0]
    target_class_name = os.path.splitext(os.path.basename(target_file_name))[0]
    
    try:
        # Read source file
        with open(source_path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()
        
        # Replace class name occurrences (whole word match)
        # Match whole word boundaries to avoid partial replacements
        pattern = r"\b" + re.escape(source_class_name) + r"\b"
        new_content = re.sub(pattern, target_class_name, content)
        
        # Write to target file
        with open(target_path, "w", encoding="utf-8") as f:
            f.write(new_content)
        
        print(f"\n{ansi.DIM}Cloned {source_file_name} to {target_file_name}{ansi.RESET}")
        print(f"{ansi.DIM}Replaced class name: {source_class_name} → {target_class_name}{ansi.RESET}\n")
        
        return {"success": True, "source": source_path, "target": target_path, "source_class": source_class_name, "target_class": target_class_name}
    except Exception as e:
        return {"success": False, "error": str(e)}


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
