---
name: to-uppercase
description: Converts text to uppercase (ALL CAPS). Use when the user asks to uppercase text, capitalize letters, convert to caps, or make text uppercase.
---

# To Uppercase

## When to use

Apply this skill when the user wants:
- Text converted to uppercase / ALL CAPS
- Letters capitalized (in the sense of “all caps”)
- Existing text changed to uppercase

## Instructions

1. **Identify the text** to convert: selected text in the editor, a quoted string, or text the user provides.
2. **Convert** every alphabetic character to its uppercase form. Preserve numbers, punctuation, and whitespace.
3. **Output or replace**:
   - If the user gave a string or selection: return or replace with the uppercased result.
   - If they asked for a general rule: explain that you uppercase by converting each letter to its uppercase equivalent (e.g. in JS/TS use `.toUpperCase()`, in Python use `.upper()`).

## Additional resources

- For usage examples, see [examples.md](examples.md).

## Notes

- Preserve line breaks and structure; only change letter case.
- For code: prefer the language’s built-in method (e.g. `string.toUpperCase()`, `str.upper()`) unless the user specifies otherwise.
- For locale-specific behavior (e.g. Turkish dotted I), use the appropriate locale-aware API if the user mentions it; otherwise use the default built-in method.
