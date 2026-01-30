class Book:
    title: str
    author: str

    def __init__(self, title, author=None):
        self.title = title
        self.author = author

    def has_autor(self):
        return bool(self.author)
