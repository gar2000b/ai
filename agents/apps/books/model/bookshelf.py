from time import sleep
from .book import Book

class Bookshelf:
    books: list[Book]

    def __init__(self, books=[]):
        self.books = books or []

    def unique_authors(self):
        yield from {book.author for book in self.books if book.author}

    def get_book_details(self):
        for book in self.books:
            if book.author:
                sleep(0.2)
                yield (book.title, book.author)
