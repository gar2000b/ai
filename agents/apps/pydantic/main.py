#!/usr/bin/env python3
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

# Pydantic to JSON
json_str = json.dumps(book.model_dump(), indent=2)
Console().print(Syntax(json_str, "json", theme="monokai", background_color="default"))
print()

# JSON to Pydantic
json_string = '{"title": "Frankenstein", "author": "Mary Shelley", "pages": 280}'
data = json.loads(json_string)
book = Book(**data)
print(f"{book}\n")

# Nested Pydantic
class Author(BaseModel):
    name: str
    birth_year: int

class Book(BaseModel):
    title: str
    author: Author
    pages: int

a = Author(name="Isaac Asimov", birth_year=1920)
b = Book(title="Foundation", author=a, pages=255)
print(f"{b}\n")

# Nested Pydantic with dicts
data = {
    "title": "Neuromancer",
    "author": {
        "name": "William Gibson",
        "birth_year": 1948
    },
    "pages": 271
}

b = Book(**data)
print(f"{b}\n")

# Pydantic to JSON
json_str = json.dumps(b.model_dump(), indent=2)
Console().print(Syntax(json_str, "json", theme="monokai", background_color="default"))
print()