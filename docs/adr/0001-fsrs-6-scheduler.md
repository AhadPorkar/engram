# 0001 – Schedule with FSRS-6, not SM-2

**Status:** accepted · 2026

## Context
The 2019 app listed “Spaced repetition” as a mode but never implemented it. The classic choice is SM-2
(SuperMemo 1987, Anki before 23.10). SM-2 uses a fixed ease factor and no explicit model of forgetting.
FSRS (Ye et al., KDD 2022) models stability, difficulty and retrievability. On large benchmark datasets
it predicts recall clearly better than SM-2, and it has been Anki's built-in scheduler since 23.10.

## Decision
Implement FSRS-6 (21 parameters, trainable forgetting-curve decay, short-term stability) in Kotlin in
`:core:srs`. Follow the semantics of the reference implementation `py-fsrs` 6: learning steps, relearning
steps and fuzz ranges.

## Consequences
- Users choose a **desired retention**, not opaque ease factors, and intervals follow from it.
- Retrievability is available everywhere: for sorting due cards, for “words you know now” and for the
  memory bar on each word.
- A golden-vector test against the official `py-fsrs` test-suite protects the port from regressions.
- Personal parameters optimised in Anki can be pasted in. An on-device optimiser is on the roadmap,
  and review logs are already stored in the shape it needs.
