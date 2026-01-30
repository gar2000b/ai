from .book import Book


class Ebook(Book):
    file_format: None | str = None

    def __init__(self, title: str, author: str, file_format: None | str = None):
        super().__init__(title, author)
        self.file_format = file_format

    def get_description(self) -> str:
        # Overriding the parent class method
        return f"'{self.title}' by {self.author} (format: {self.file_format})"