# 0004 – Split notes, cards and review logs

**Status:** accepted · 2026

## Context
The old schema was a single table `word_table(id, word, meaning, synonyms)`. It could not hold a schedule,
a direction, or a history.

## Decision
Four tables, like Anki's model: `decks` → `notes` (content) → `cards` (one per direction, with FSRS state)
→ `review_logs` (immutable history). Foreign keys cascade on delete. The schema is exported to
`core/database/schemas` for migration tests.

## Consequences
- Recognition and production of the same word are scheduled independently but buried together.
- Statistics (true retention, streaks, forecast) are computed from logs instead of being stored redundantly.
- Undo deletes exactly one log row and restores one card row.
