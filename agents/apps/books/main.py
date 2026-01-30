import os
import sys
from model import Book, Bookshelf
import constants.ansi as ansi

def clear_screen():
    # Git Bash on Windows uses Unix "clear"; cmd/PowerShell use "cls"
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
    # Emit UTF-8 so Unicode (bullet •, etc.) renders in Git Bash
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    clear_screen()

    # create Book instances (not dicts)
    book1 = Book("Great Expectations", "Charles Dickens")
    book2 = Book("Bleak House", "Charles Dickens")
    book3 = Book("An Book By No Author")
    book4 = Book("Moby Dick", "Herman Melville")

    books = [book1, book2, book3, book4]

    # create bookshelf
    shelf = Bookshelf(books)

    # iterate unique authors
    # for author in shelf.unique_authors():
    #     print(author)

    # iterate books (raw ANSI so Git Bash shows colors)
    # flush=True so output appears immediately in Git Bash (avoids buffering)
    print(f"{ansi.BOLD}{ansi.YELLOW}Books on shelf:{ansi.RESET}\n", flush=True)
    for title, author in shelf.get_book_details():
        print(f"• {ansi.BOLD}{ansi.MAGENTA}{title}{ansi.RESET} {ansi.DIM}by{ansi.RESET} {ansi.CYAN}{author}{ansi.RESET}", flush=True)

    print(f"\n{ansi.DIM}Press any key to exit...{ansi.RESET}", flush=True)
    wait_key()
    clear_screen()

if __name__ == "__main__":
    main()
