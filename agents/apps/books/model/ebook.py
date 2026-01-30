import constants.ansi as ansi
from .book import Book


class Ebook(Book):
    """A book in digital form with a specific file format (e.g. PDF, EPUB)."""

    file_format: None | str = None

    def __init__(self, title: str, author: str, file_format: None | str = None):
        """Initialize an ebook with title, author, and optional file format."""
        super().__init__(title, author)
        self.file_format = file_format

    def get_description(self) -> str:
        """Return an ANSI-colored one-line description: title, author, and format."""
        return (
            f"'{ansi.BOLD}{ansi.MAGENTA}{self.title}{ansi.RESET}' "
            f"{ansi.DIM}by{ansi.RESET} {ansi.CYAN}{self.author}{ansi.RESET} "
            f"{ansi.DIM}in {ansi.RESET}{ansi.GREEN}{self.file_format}{ansi.RESET}{ansi.DIM} file format{ansi.RESET}"
        )
