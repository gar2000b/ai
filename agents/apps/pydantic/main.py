from pydantic import BaseModel
import os
import sys
import json
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

class Book(BaseModel):
    title: str
    author: str
    pages: int
    published: bool = True

clear_screen()
book = Book(title="1984", author="George Orwell", pages=328)
print(f"{book}\n")

fields = {
    "title": "The Hobbit",
    "author": "J.R.R. Tolkien",
    "pages": "310"
}

book = Book(**fields)
print(f"{book}\n")

json_str = json.dumps(book.model_dump(), indent=2)
Console().print(Syntax(json_str, "json", theme="monokai", background_color="default"))
print()