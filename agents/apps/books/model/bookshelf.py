from .book import Book


class Bookshelf:
    """A collection of books that can be listed and queried by author."""

    books: list[Book]

    def __init__(self, books=None):
        """Initialize a bookshelf with an optional list of books."""
        self.books = books or []

    def unique_authors(self):
        """Yield each unique author among books on the shelf (books with no author are skipped)."""
        yield from {book.author for book in self.books if book.author}

    def get_book_details(self):
        """Yield the ANSI-colored description string for each book that has an author."""
        for book in self.books:
            if book.author:
                yield book.get_description()
