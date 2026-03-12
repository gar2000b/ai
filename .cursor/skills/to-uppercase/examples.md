# To Uppercase — Usage Examples

Concrete examples of applying this skill.

---

## Example 1: Simple phrase

**User:** "Uppercase this: hello world"

**Input:** `hello world`

**Output:** `HELLO WORLD`

---

## Example 2: Preserve numbers and punctuation

**User:** "Make it all caps: see you at 5:30 p.m.!"

**Input:** `see you at 5:30 p.m.!`

**Output:** `SEE YOU AT 5:30 P.M.!`

Numbers, punctuation, and spaces are unchanged; only letters are uppercased.

---

## Example 3: Multiline text (preserve structure)

**User:** "Convert this to uppercase:"

**Input:**
```
line one
line two
  indented line
```

**Output:**
```
LINE ONE
LINE TWO
  INDENTED LINE
```

Line breaks and indentation are preserved.

---

## Example 4: Code-style (built-in method)

**User:** "How do I uppercase a string in JavaScript?"

**Skill applies:** Suggest the built-in method.

**Example snippet:** `text.toUpperCase()` → e.g. `"hello".toUpperCase()` → `"HELLO"`

**User:** "And in Python?"

**Example snippet:** `text.upper()` → e.g. `"hello".upper()` → `"HELLO"`

---

## Example 5: Selection in editor

**User:** Selects the text `the quick brown fox` in a file and says "uppercase this"

**Action:** Replace the selection with `THE QUICK BROWN FOX`.

---

## What not to do

- **Don’t** change numbers or punctuation (e.g. `5:30` stays `5:30`).
- **Don’t** collapse or alter line breaks or indentation.
- **Don’t** use a custom loop to uppercase when the language has `.toUpperCase()` / `.upper()` — prefer the built-in method unless the user asks otherwise.
