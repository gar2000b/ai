#!/usr/bin/env python3

import argparse
import utils.emoji as emoji_module
from platform_utils import set_process_name
from agents.core import agent_loop
import config.env  # noqa: F401 — load early so .env and API keys are available
from utils.screen import clear_screen, print_goodbye, print_welcome

set_process_name()


def _parse_args():
    parser = argparse.ArgumentParser(
        description="AI Discovery Agent with command history and ANSI-colored output.",
    )
    parser.add_argument(
        "-de",
        "--disable-emojis",
        action="store_true",
        help="For example: uses a bullet (•) instead of emoji for list items.",
    )
    return parser.parse_args()


def main():
    """Entry point: clear screen, show welcome, run agent loop."""
    clear_screen()
    print_welcome()
    try:
        agent_loop()
    except KeyboardInterrupt:
        print_goodbye()


if __name__ == "__main__":
    args = _parse_args()
    if args.disable_emojis:
        emoji_module.bool_use_emoji = False
    main()
