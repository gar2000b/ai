import os


def write_code(code: str, write_path: str, file_name: str) -> None:
    """Write code to a new file at the given path with the given name."""
    os.makedirs(write_path, exist_ok=True)
    file_path = os.path.join(write_path, file_name)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(code)
