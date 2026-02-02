#!/usr/bin/env python3
"""Books app: displays a shelf of books and ebooks with ANSI-colored output."""

import argparse
import os
import sys
from model import Book, Bookshelf, Ebook
import constants.ansi as ansi
from utils.emoji import get_book_marker


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


def main():
    """Run the app: clear screen, show books on shelf with colored descriptions, then wait for key and clear again."""
    # Emit UTF-8 so Unicode (bullet •, etc.) renders in Git Bash
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    book_marker = get_book_marker()
    clear_screen()

    # create Book instances (not dicts)
    book1 = Book("Great Expectations", "Charles Dickens")
    book2 = Book("Bleak House", "Charles Dickens")
    book3 = Book("An Book By No Author")
    book4 = Book("Moby Dick", "Herman Melville")
    ebook1 = Ebook("Great Expectations", "Charles Dickens", "PDF")
    ebook2 = Ebook("Bleak House", "Charles Dickens", "PDF")

    books = [book1, book2, book3, book4, ebook1, ebook2]

    # create bookshelf
    shelf = Bookshelf(books)

    # iterate unique authors
    # for author in shelf.unique_authors():
    #     print(author)

    # iterate books (raw ANSI so Git Bash shows colors)
    # flush=True so output appears immediately in Git Bash (avoids buffering)
    print(f"{ansi.BOLD}{ansi.YELLOW}Books on shelf:{ansi.RESET}\n", flush=True)
    for description in shelf.get_book_details():
        print(f"{book_marker} {description}", flush=True)

    print(f"\n{ansi.DIM}Press any key to exit...{ansi.RESET}", flush=True)
    wait_key()
    clear_screen()

def _parse_args():
    parser = argparse.ArgumentParser(
        description="Display a shelf of books and ebooks with ANSI-colored output.",
    )
    parser.add_argument(
        "-de", "--disable-emojis",
        action="store_true",
        help="For example: uses a bullet (•) instead of the book emoji (📚) for list items.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = _parse_args()
    if args.disable_emojis:
        import utils.emoji as emoji_module
        emoji_module.bool_use_emoji = False
    main()
