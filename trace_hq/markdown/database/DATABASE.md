# Database connection (Trace HQ)

This document describes how to connect to the Trace HQ MySQL database so agents and tooling can interact with it.

## Connection details

- **Engine:** MySQL
- **Database name:** `trace-hq`
- **User:** Stored in `.env` as `DB_USER` (e.g. `trace-hq-admin`)
- **Password:** Stored in `.env` as `DB_PASSWORD` (never commit real values)
- **Host:** Stored in `.env` as `DB_HOST` (default `localhost`)

Sensitive values live in the project root `.env` file: `trace_hq/.env`. **Never put the password on the command line** (process lists can expose it). Use the script or a config file instead.

## CLI connection (secure)

From the `trace_hq` directory, use the helper script so credentials are read from `.env` and the password is passed via a temporary config file (never on the command line):

```bash
# From trace_hq directory
./scripts/mysql.sh -e "SHOW DATABASES;"
./scripts/mysql.sh   # interactive session
```

The script loads `trace_hq/.env`, writes a temporary MySQL `[client]` config file with `user`, `password`, and `host`, runs `mysql --defaults-extra-file=...`, then deletes the temp file. The password never appears in process listings.

## For agents / tooling

1. **Read credentials from:** `trace_hq/.env` (or the repo root if your runner loads it from there).
2. **Variables to use:**
   - `DB_HOST` – MySQL host (e.g. `localhost`)
   - `DB_USER` – MySQL user (e.g. `trace-hq-admin`)
   - `DB_PASSWORD` – MySQL password
   - `DB_NAME` – Database name (e.g. `trace-hq`)
3. **Connection string (typical):**  
   `mysql://${DB_USER}:${DB_PASSWORD}@${DB_HOST}/${DB_NAME}`  
   (Use this in code only; never log it or pass the password on a shell command line.)
4. **Security:** Never log or expose `DB_PASSWORD`; never commit real credentials; keep `.env` in `.gitignore`. For CLI, use `scripts/mysql.sh` so the password is not on the command line.

## Schema

Table definitions (DDL only, no data) live in **`trace_hq/database/schema/`** as numbered `.sql` files. Apply in order (`01_projects.sql` through `06_story_history_audit.sql`). See `database/schema/README.md` for a short index and requirements references. Do not execute against the database until schemas have been reviewed.

## Quick reference

| Item        | Env var     | Example        |
|------------|-------------|----------------|
| Host       | `DB_HOST`   | `localhost`    |
| User       | `DB_USER`   | `trace-hq-admin` |
| Password   | `DB_PASSWORD` | (from .env)  |
| Database   | `DB_NAME`   | `trace-hq`     |
