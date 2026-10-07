# 0005 – Versioned JSON backups through the Storage Access Framework

**Status:** accepted · 2026

## Context
ProjectShaco copied the raw SQLite file to `/Downloads`. That needed storage permissions, which no longer
work on Android 11+. It also killed the process with `System.exit(0)`, and the file could not be read across
schema versions.

## Decision
Export and import a versioned JSON document (`format = "engram-backup"`, `version = 1`) through the Storage
Access Framework (`CreateDocument` / `OpenDocument`). Restore offers *replace* (ids kept) or *merge*
(ids remapped). Old `word_database` files can still be imported by reading `word_table` directly.

## Consequences
- No dangerous permissions. Users choose any location (Drive, local, USB).
- The file is human-readable and diff-able, and older app versions reject newer files with a clear message.
- Android Auto Backup covers the database and settings as well.
