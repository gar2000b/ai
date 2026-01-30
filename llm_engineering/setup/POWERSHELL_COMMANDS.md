# PowerShell Commands Reference (0-9)

This document lists the PowerShell commands from the setup instructions for Windows PC users.

## Commands:

**0. Check current directory:**
```powershell
pwd
```

**1. Check if Git is installed:**
```powershell
git
```

**2. Navigate to projects directory:**
```powershell
cd projects
```

**3. Create projects directory (if it doesn't exist):**
```powershell
mkdir projects
```

**4. Navigate into projects directory:**
```powershell
cd projects
```

**5. Configure Git for long filenames (if needed):**
```powershell
git config --system core.longpaths true
```

**6. Clone the repository:**
```powershell
git clone https://github.com/ed-donner/llm_engineering.git
```

**7. Navigate into the project directory:**
```powershell
cd llm_engineering
```

**8. Check if uv is installed:**
```powershell
uv --version
```

**9. Update uv to latest version:**
```powershell
uv self update
```

**10. Sync dependencies with uv:**
```powershell
uv sync
```

## Additional uv Commands (for reference):

- Add a package: `uv add xxx` (instead of `pip install xxx`)
- Run a Python script: `uv run xxx` (instead of `python xxx`)
