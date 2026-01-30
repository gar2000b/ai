class Book:
    title: str
    author: str

    def __init__(self, title: str, author: None | str = None):
        self.title = title
        self.author = author

    def has_autor(self):
        return bool(self.author)

    def get_description(self) -> str:
        return f"'{self.title}' by {self.author} in {self.file_format} format"
