#!/usr/bin/env bash
# Test prompt hook: runs before each prompt is sent. Sends test text back to stdout.
# Cursor passes JSON context via stdin (conversation_id, generation_id, etc.).

now=$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "unknown")
echo "[test-prompt-hook] Ran at $now — beforeSubmitPrompt"
echo "[test-prompt-hook] Test text: Hook is working. Prompt is about to be sent."

# Optional: log to file so you can confirm it ran (.cursor/hook-test.log)
logfile="$(dirname "$0")/../hook-test.log"
echo "$now — beforeSubmitPrompt ran" >> "$logfile" 2>/dev/null || true

# Pass through stdin so Cursor still receives the original context
cat
