# 0003 – Derive FSRS grades from exercise outcomes

**Status:** accepted · 2026

## Context
FSRS needs one of four grades per review. Anki asks learners to grade themselves, which they do
inconsistently, and beginners find it confusing. Memrise and Babbel grade automatically but do not use
a memory model.

## Decision
`RatingPolicy` maps an `ExerciseOutcome` (verdict, hint, response time, retrieval level) to a grade:
wrong → Again; typo, article, synonym, hint or very slow → Hard; correct recognition or cued recall → Good;
fast, exact free recall → Easy. Self-grading stays available in *Flashcards* mode, and “I was right”
lets learners overrule the grader.

## Consequences
- Recognition success never produces Easy, which avoids inflated intervals from easy formats.
- Thresholds are explicit constants with unit tests and can be tuned from review-log data later.
- Exercise type and response time are stored in every `ReviewLog` for later analysis.
