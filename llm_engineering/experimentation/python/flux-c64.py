#!/scripts/.venv/bin/python

# Run the following first to ensure all dependencies are available:
#
# python3 -m venv .venv
# . .venv/bin/activate
# pip install transformers -U
# pip install -e
# pip install torch
# pip install IPython
# pip install psutil
# pip install protobuf
# pip install sentencepiece
# pip install -U "diffusers>=0.30" "transformers>=4.44" accelerate safetensors
# pip install -U xformers
# deactivate

from pathlib import Path

from dotenv import load_dotenv
from huggingface_hub import login
from diffusers import FluxPipeline
import torch
import sys
import os
import re
import glob
import time
import shlex

# Load .env from llm_engineering root (two levels up from this script)
_env_path = Path(__file__).resolve().parent.parent.parent / ".env"
load_dotenv(_env_path)


def get_next_sequential_filename():
    """Find the next sequential number for PNG files in the images subdirectory."""
    current_dir = os.getcwd()
    images_dir = os.path.join(current_dir, "images")
    
    # Create images directory if it doesn't exist
    os.makedirs(images_dir, exist_ok=True)
    
    png_files = glob.glob(os.path.join(images_dir, "*.png"))
    
    # Extract numbers from filenames (e.g., "1.png", "133.png")
    max_number = 0
    for file in png_files:
        filename = os.path.basename(file)
        # Match files that are just a number followed by .png
        match = re.match(r'^(\d+)\.png$', filename)
        if match:
            number = int(match.group(1))
            max_number = max(max_number, number)
    
    # Next sequential number
    next_number = max_number + 1
    output_filename = f"{next_number}.png"
    return os.path.join(images_dir, output_filename)


def save_image_sequentially(image):
    """Save image with the next sequential number in the images subdirectory."""
    output_path = get_next_sequential_filename()
    image.save(output_path)
    print(f"Saved: {output_path}")
    return output_path


def parse_requirement_line(line):
    """Parse a requirement line into num_images, height, width, prompt_detail."""
    line = line.strip()
    if not line or line.startswith('#') or line.endswith(" [x]") or line.endswith(" [-]"):
        return None
    
    # Remove markers if present (shouldn't be, but just in case)
    if line.endswith(" [x]"):
        line = line[:-4].strip()
    elif line.endswith(" [-]"):
        line = line[:-4].strip()
    
    # Use shlex to properly parse quoted strings
    try:
        parts = shlex.split(line)
        if len(parts) < 4:
            print(f"Warning: Invalid line format (need 4 parts): {line}")
            return None
        
        num_images = int(parts[0])
        height = int(parts[1])
        width = int(parts[2])
        prompt_detail = " ".join(parts[3:])  # Join remaining parts in case prompt has spaces
        
        return {
            'num_images': num_images,
            'height': height,
            'width': width,
            'prompt_detail': prompt_detail,
            'original_line': line
        }
    except (ValueError, IndexError) as e:
        print(f"Warning: Error parsing line '{line}': {e}")
        return None


def read_requirements_file(filepath):
    """Read requirements file and return unprocessed lines (skips header lines starting with #)."""
    if not os.path.exists(filepath):
        return []
    
    unprocessed = []
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n\r')
            # Skip empty lines, header lines (starting with #), already processed lines, and currently processing lines
            if (line.strip() and not line.strip().startswith('#') 
                and not line.strip().endswith(" [x]") 
                and not line.strip().endswith(" [-]")):
                unprocessed.append(line)
    
    return unprocessed


def mark_line_as_processing(filepath, original_line):
    """Mark a line as currently processing by appending ' [-]' to it."""
    if not os.path.exists(filepath):
        return
    
    # Read all lines
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Find and update the matching line
    with open(filepath, 'w', encoding='utf-8') as f:
        for line in lines:
            line_stripped = line.rstrip('\n\r')
            # Match the original line (without any marker)
            if (line_stripped == original_line or 
                line_stripped == original_line + " [x]" or 
                line_stripped == original_line + " [-]"):
                f.write(original_line + " [-]\n")
            else:
                f.write(line)


def mark_line_as_processed(filepath, original_line):
    """Mark a line as processed by replacing ' [-]' with ' [x]' or appending ' [x]' if no marker."""
    if not os.path.exists(filepath):
        return
    
    # Read all lines
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # Find and update the matching line
    with open(filepath, 'w', encoding='utf-8') as f:
        for line in lines:
            line_stripped = line.rstrip('\n\r')
            # Match the original line (with or without markers)
            if (line_stripped == original_line or 
                line_stripped == original_line + " [x]" or 
                line_stripped == original_line + " [-]"):
                f.write(original_line + " [x]\n")
            else:
                f.write(line)


hf_token = os.getenv("HF_TOKEN")
if not hf_token:
    sys.exit("HF_TOKEN not set. Add HF_TOKEN=your_token to the .env file in the llm_engineering root.")
login(hf_token, add_to_git_credential=True)

# Force CPU-only execution (prevents any CUDA allocation)
torch.set_default_device("cpu")

pipe = FluxPipeline.from_pretrained(
    "black-forest-labs/FLUX.1-dev",
    torch_dtype=torch.float32,   # CPU-friendly; fp16 on CPU is often slower/unsupported
)

# Ensure everything stays on CPU
pipe.to("cpu")

pipe.load_lora_weights(
    "Omarito2412/Flux-Commodore-64",
    weight_name="C64_Flux.safetensors"
)

# Fixed beginning part of the prompt
prompt_base = "The image is a C64 pixel style illustration of "

# Requirements file path - look in script directory, fallback to current directory
try:
    script_dir = os.path.dirname(os.path.abspath(__file__))
    requirements_file = os.path.join(script_dir, "flux-c64-requirements.txt")
except NameError:
    # __file__ not available, use current directory
    requirements_file = os.path.join(os.getcwd(), "flux-c64-requirements.txt")

# Main processing loop
print("Starting flux-c64 image generator...")
print(f"Monitoring requirements file: {requirements_file}")
print("Press Ctrl+C to stop\n")

no_jobs_message_printed = False

try:
    while True:
        # Read unprocessed lines from requirements file
        unprocessed_lines = read_requirements_file(requirements_file)
        
        if not unprocessed_lines:
            if not no_jobs_message_printed:
                print("No unprocessed jobs found. Will poll every 5 seconds until a new job requirement has been detected...")
                no_jobs_message_printed = True
            time.sleep(5)
            continue
        
        # Reset flag when we have jobs to process
        if no_jobs_message_printed:
            no_jobs_message_printed = False
        
        # Process each unprocessed line (top to bottom)
        for line in unprocessed_lines:
            # Parse the requirement line
            req = parse_requirement_line(line)
            if req is None:
                # Mark as processed even if invalid to avoid reprocessing
                mark_line_as_processed(requirements_file, line)
                continue
            
            # Mark line as currently processing
            mark_line_as_processing(requirements_file, req['original_line'])
            
            num_images = req['num_images']
            height = req['height']
            width = req['width']
            prompt_detail = req['prompt_detail']
            
            # Assemble prompt
            prompt = prompt_base + prompt_detail
            
            print(f"\n{'='*60}")
            print(f"Processing: {num_images} image(s) | {width}x{height} | {prompt_detail}")
            print(f"{'='*60}")
            
            # Generate the specified number of images
            for i in range(num_images):
                print(f"Generating image {i + 1} of {num_images}...")
                image = pipe(
                    prompt=prompt,
                    num_inference_steps=12,
                    guidance_scale=3.5,
                    height=height,
                    width=width,
                ).images[0]
                
                # Save output with sequential numbering
                save_image_sequentially(image)
            
            print(f"Completed: Generated {num_images} image(s) for '{prompt_detail}'")
            
            # Mark line as processed
            mark_line_as_processed(requirements_file, req['original_line'])
            print(f"Marked job as completed.\n")
        
        # After processing all available jobs, wait before next scan
        print("All current jobs processed. Waiting 5 seconds before next scan...")
        time.sleep(5)

except KeyboardInterrupt:
    print("\n\nStopped by user. Exiting...")
    sys.exit(0)

