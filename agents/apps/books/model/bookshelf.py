from .book import Book

class Bookshelf:

    books: list[Book]

    def __init__(self, books=None):
        self.books = books or []

    def unique_authors(self):
        yield from {book.author for book in self.books if book.author}

    def get_book_details(self):
        for book in self.books:
            if book.author:
                yield book.get_description()
