# ANSI escape codes for terminal styling (works in Git Bash, cmd, etc.)
# https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters

# --- Reset & text style (existing) ---
RESET = "\033[0m"
BOLD = "\033[1m"
DIM = "\033[2m"
YELLOW = "\033[33m"
MAGENTA = "\033[35m"
CYAN = "\033[36m"

# --- Additional text styles ---
ITALIC = "\033[3m"
UNDERLINE = "\033[4m"
BLINK_SLOW = "\033[5m"
BLINK_FAST = "\033[6m"
REVERSE = "\033[7m"  # Swap foreground and background
HIDDEN = "\033[8m"
STRIKETHROUGH = "\033[9m"  # Not supported in all terminals

# --- Foreground colors (standard) ---
BLACK = "\033[30m"
RED = "\033[31m"
GREEN = "\033[32m"
# YELLOW = "\033[33m"  (already above)
BLUE = "\033[34m"
# MAGENTA = "\033[35m"  (already above)
# CYAN = "\033[36m"  (already above)
WHITE = "\033[37m"

# --- Foreground colors (bright) ---
BRIGHT_BLACK = "\033[90m"   # Gray
BRIGHT_RED = "\033[91m"
BRIGHT_GREEN = "\033[92m"
BRIGHT_YELLOW = "\033[93m"
BRIGHT_BLUE = "\033[94m"
BRIGHT_MAGENTA = "\033[95m"
BRIGHT_CYAN = "\033[96m"
BRIGHT_WHITE = "\033[97m"

# --- Background colors (standard) ---
BG_BLACK = "\033[40m"
BG_RED = "\033[41m"
BG_GREEN = "\033[42m"
BG_YELLOW = "\033[43m"
BG_BLUE = "\033[44m"
BG_MAGENTA = "\033[45m"
BG_CYAN = "\033[46m"
BG_WHITE = "\033[47m"

# --- Background colors (bright) ---
BG_BRIGHT_BLACK = "\033[100m"
BG_BRIGHT_RED = "\033[101m"
BG_BRIGHT_GREEN = "\033[102m"
BG_BRIGHT_YELLOW = "\033[103m"
BG_BRIGHT_BLUE = "\033[104m"
BG_BRIGHT_MAGENTA = "\033[105m"
BG_BRIGHT_CYAN = "\033[106m"
BG_BRIGHT_WHITE = "\033[107m"
