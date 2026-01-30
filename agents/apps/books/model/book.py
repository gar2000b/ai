import constants.ansi as ansi


class Book:
    title: str
    author: str

    def __init__(self, title: str, author: None | str = None):
        self.title = title
        self.author = author

    def has_autor(self):
        return bool(self.author)

    def get_description(self) -> str:
        return (
            f"'{ansi.BOLD}{ansi.MAGENTA}{self.title}{ansi.RESET}' "
            f"{ansi.DIM}by{ansi.RESET} {ansi.CYAN}{self.author}{ansi.RESET} "
            f"{ansi.DIM}in {ansi.RESET}{ansi.GREEN}Book{ansi.RESET}{ansi.DIM} format{ansi.RESET}"
        )
