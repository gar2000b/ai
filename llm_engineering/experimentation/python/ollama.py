#!/home/ulysses/scripts/.venv/bin/python
"""
Project - experimentation

This is used for experimenting with ollama running in the on-prem data center on ulysses.local
"""

# Install required packages:
# pip install python-dotenv openai gradio
# Or: pip install -r requirements.txt

# On the target machine to prevent breaking the system tooling:
# Create and move this script into /scripts directory and make it executable
# cd /scripts
# python3 -m venv .venv
# . .venv/bin/activate
# pip install python-dotenv openai gradio
# deactivate
# chmod +x ollama.py

import os
import logging
import signal
import sys
import time
from dotenv import load_dotenv
from openai import OpenAI
import gradio as gr

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialization
load_dotenv(override=True)

# Configuration from environment variables with defaults
OLLAMA_BASE_URL = os.getenv('OLLAMA_BASE_URL', 'http://ulysses.local:11434/v1')
OLLAMA_API_KEY = os.getenv('OLLAMA_API_KEY', 'ollama')
SERVER_PORT = int(os.getenv('SERVER_PORT', '7882'))
SERVER_NAME = os.getenv('SERVER_NAME', '0.0.0.0')
MAX_RETRIES = int(os.getenv('MAX_RETRIES', '3'))
RETRY_DELAY_BASE = float(os.getenv('RETRY_DELAY_BASE', '2.0'))  # Base delay in seconds

# MODEL = "gpt-4.1-mini"
# openai = OpenAI()

# As an alternative, if you'd like to use Ollama instead of OpenAI
# Check that Ollama is running for you locally (see week1/day2 exercise) then uncomment these next 2 lines
# MODEL = "llama3.2"                 # Best lightweight general chat
# MODEL = "llama3.2:latest"          # Best small all-rounder (default)
# MODEL = "llama3.1:8b"              # Solid general reasoning, older gen
# MODEL = "deepseek-r1:8b"           # Fast reasoning + logic experiments
# MODEL = "deepseek-r1:14b"          # Smartest local reasoning model
# MODEL = "deepseek-coder-v2:16b"    # Strong logic-heavy coding
MODEL = "deepseek-coder-v2:latest" # Best for complex debugging + reasoning
# MODEL = "llama2:13b"               # Legacy large model, slower
# MODEL = "llama3.2:3b"              # Fast lightweight assistant
# MODEL = "llama3:8b"                # Balanced chat + reasoning
# MODEL = "llama3.2:1b"              # Ultra-fast low-quality tests
# MODEL = "qwen2.5-coder:7b"         # Fast coding iteration
# MODEL = "qwen2.5-coder:14b"        # Best overall coding model

# Optional: OpenAI API key (for fallback or validation)
openai_api_key = os.getenv('OPENAI_API_KEY')
if openai_api_key:
    logger.info(f"OpenAI API Key exists and begins {openai_api_key[:8]}")
else:
    logger.info("OpenAI API Key not set (using Ollama)")

logger.info(f"Connecting to Ollama at: {OLLAMA_BASE_URL}")
logger.info(f"Using model: {MODEL}")
logger.info(f"Server will bind to: {SERVER_NAME}:{SERVER_PORT}")

# Initialize OpenAI client (configured for Ollama)
try:
    openai = OpenAI(base_url=OLLAMA_BASE_URL, api_key=OLLAMA_API_KEY)
    logger.info("OpenAI client initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize OpenAI client: {e}")
    sys.exit(1)

system_message = """
You are an expert on writing code in assembly for the 6510 microprocessor, targetting the commodore 64 computer.
You are also an expert on the Commodore 64 computer and its peripherals.
You have to ensure that the code is correct and will run on the Commodore 64 computer.
You have to ensure that code is properly de-composed into sub-routines and functions and that they are named appropriately.
You have to ensure that the code is well-documented and that it is easy to understand.
C64 is synonomous with the Commodore 64 computer.
When assembly code or a program is requested, we are talking about assembly language code for the 6510 microprocessor to run on the Commodore 64 computer.
Programs should always run with the Commodore 64 BASIC interpreter, so you don't have to worry about that. So load "*",8,1 should work etc.
"""


def chat(message, history):
    """Handle chat messages with retry logic and error handling"""
    history = [{"role":h["role"], "content":h["content"]} for h in history]
    messages = [{"role": "system", "content": system_message}] + history + [{"role": "user", "content": message}]
    
    last_error = None
    for attempt in range(MAX_RETRIES):
        try:
            logger.debug(f"Sending message to model {MODEL} (attempt {attempt + 1}/{MAX_RETRIES})")
            response = openai.chat.completions.create(model=MODEL, messages=messages)
            # If we had previous failures but now succeeded, log recovery
            if attempt > 0:
                logger.info(f"Ollama connection recovered after {attempt} failed attempt(s)")
            return response.choices[0].message.content
        except Exception as e:
            last_error = e
            error_str = str(e)
            
            # Check if it's a connection error (server down)
            is_connection_error = any(keyword in error_str.lower() for keyword in [
                'connection', 'connect', 'refused', 'timeout', 'unreachable', 
                'network', 'failed to establish'
            ])
            
            if attempt < MAX_RETRIES - 1:  # Don't sleep on last attempt
                # Exponential backoff: 2s, 4s, 8s, etc.
                delay = RETRY_DELAY_BASE * (2 ** attempt)
                if is_connection_error:
                    logger.warning(f"Ollama connection error (attempt {attempt + 1}/{MAX_RETRIES}): {error_str}")
                    logger.info(f"Retrying in {delay:.1f} seconds...")
                else:
                    logger.warning(f"Error in chat function (attempt {attempt + 1}/{MAX_RETRIES}): {error_str}")
                    logger.info(f"Retrying in {delay:.1f} seconds...")
                time.sleep(delay)
            else:
                # Last attempt failed
                logger.error(f"Error in chat function after {MAX_RETRIES} attempts: {error_str}")
                if is_connection_error:
                    return f"Error: Cannot connect to Ollama server at {OLLAMA_BASE_URL}. The server may be down or unreachable. Please check the connection and try again."
                else:
                    return f"Error: Failed to get response from model after {MAX_RETRIES} attempts. Details: {error_str}"
    
    # Should never reach here, but just in case
    return f"Error: Unexpected failure. Last error: {str(last_error)}"


def test_ollama_connection():
    """Test if Ollama is reachable and the model is available"""
    try:
        logger.info("Testing Ollama connection...")
        test_response = openai.chat.completions.create(
            model=MODEL,
            messages=[{"role": "user", "content": "test"}],
            max_tokens=5
        )
        logger.info("Ollama connection test successful")
        return True
    except Exception as e:
        logger.error(f"Ollama connection test failed: {e}")
        logger.error(f"Please ensure Ollama is running at {OLLAMA_BASE_URL}")
        logger.error(f"and that the model '{MODEL}' is available")
        return False


def signal_handler(sig, frame):
    """Handle graceful shutdown"""
    logger.info("Shutdown signal received, closing server...")
    sys.exit(0)


if __name__ == "__main__":
    # Register signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Test Ollama connection before starting server
    if not test_ollama_connection():
        logger.error("Cannot start server: Ollama connection failed")
        sys.exit(1)
    
    # Launch the Gradio chat interface
    logger.info(f"Starting Gradio server on {SERVER_NAME}:{SERVER_PORT}")
    try:
        gr.ChatInterface(
            fn=chat,
            title="Commodore 64 Assembly Expert",
            description="Chat with an AI expert on 6510 assembly language for the Commodore 64"
        ).launch(
            server_name=SERVER_NAME,
            server_port=SERVER_PORT,
            share=False  # Set to True if you want a public Gradio link
        )
    except Exception as e:
        logger.error(f"Failed to start Gradio server: {e}")
        sys.exit(1)
