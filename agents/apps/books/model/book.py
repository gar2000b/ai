import constants.ansi as ansi


class Book:
    """A physical or generic book with a title and optional author."""

    title: str
    author: str

    def __init__(self, title: str, author: None | str = None):
        """Initialize a book with title and optional author."""
        self.title = title
        self.author = author

    def has_autor(self):
        """Return True if the book has an author, False otherwise."""
        return bool(self.author)

    def get_description(self) -> str:
        """Return an ANSI-colored one-line description: title, author, and 'book format'."""
        return (
            f"'{ansi.BOLD}{ansi.MAGENTA}{self.title}{ansi.RESET}' "
            f"{ansi.DIM}by{ansi.RESET} {ansi.CYAN}{self.author}{ansi.RESET} "
            f"{ansi.DIM}in {ansi.RESET}{ansi.GREEN}Book{ansi.RESET}{ansi.DIM} format{ansi.RESET}"
        )
