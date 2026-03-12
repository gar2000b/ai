# Prompt hook example (word check in prompt)

## What it does

A **prompt-based** hook on `beforeSubmitPrompt`: before your message is sent to the model, Cursor sends the prompt (and context) to a fast LLM and asks it to check whether the prompt contains a specific word.

**Current example:** The hook checks for the word **`please`** (case-insensitive). If the user's message contains "please", the prompt is allowed. If not, submission is blocked and the user sees: *"Your message must include the word 'please'."*

## How it works

1. **When** – Every time you send a message in the agent/composer, Cursor runs this hook first (after the command hook that writes to `hook-test.log`).
2. **Input** – Cursor injects the hook input (your prompt text, attachments, etc.) into the prompt. The placeholder `$ARGUMENTS` in the hook prompt is replaced with that JSON so the LLM can see the actual message.
3. **Your prompt** – The hook prompt tells the LLM: "Check if the user's prompt contains the word 'please'. If yes → ok: true. If no → ok: false with a reason."
4. **LLM response** – The model returns `{ "ok": true }` to allow, or `{ "ok": false, "reason": "..." }` to block. Cursor then allows or blocks the submission and can show the reason to you.

## Try it

- **Allowed:** "Please list the files in the project" or "Can you please explain hooks?"
- **Blocked:** "List the files" or "Explain hooks" (no "please" → submission blocked with the reason message)

## Customize the word

Edit the `prompt` in `.cursor/hooks.json` and change `'please'` to any word you want to require (e.g. `'review'`, `'approved'`, a ticket ID pattern). Keep the same JSON response format: `{"ok": true}` or `{"ok": false, "reason": "..."}`.
