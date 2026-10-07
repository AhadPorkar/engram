# Changelog

All notable changes are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/).

## [2.0.0] – 2026-10-06 – Engram
A complete rewrite of ProjectShaco under a new name and new namespaces (`com.apr.projectshaco` → `com.ahadporkar.engram`).

### Added
- FSRS-6 scheduler in pure Kotlin with learning/relearning steps, interval fuzz, desired retention and personal parameters; verified against `py-fsrs` 6.
- Adaptive exercise ladder: presentation, multiple choice, reverse multiple choice, listening, letter tiles, cloze, typing, speaking, flashcards.
- Automatic grading, smart answer checking (accents, articles, alternatives, synonyms, typo tolerance) and letter-level feedback.
- Study modes *Learn & review*, *Flashcards* and *Practice* (shuffled, oldest/newest first, starred only, weakest first).
- Notes with example sentence, memory hook, tags and star; recognition and production cards; leech detection; undo.
- Daily goal, streaks, WorkManager reminder; statistics with true retention, forecast, maturity and estimated known words.
- JSON backup/restore, CSV/TSV/Anki import, import of old ProjectShaco `word_database` files.
- Text-to-speech and speech recognition; English and German UI; dark theme and dynamic colour.
- Multi-module architecture (Compose, Hilt, Room, DataStore), 92 automated tests, GitHub Actions CI, Dependabot.

### Removed
- Android Support Library, Kotlin synthetics, storage permissions and the raw database copy to `/Downloads`.

## [1.0.0] – 2019 – ProjectShaco
- Word list with word, meaning and synonyms (Room 1.1, RecyclerView).
- Flashcard pager with random, start-to-end and end-to-start order.
- Database copy to the Downloads folder.
