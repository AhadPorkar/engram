# The learning science behind Engram

English · [Deutsch](learning-science.de.md)

Engram combines two traditions:

1. **Spaced-repetition scheduling** (Anki, SuperMemo) decides *when* a word is practised.
2. **Exercise design** (Memrise, Babbel, Duolingo) decides *how* it is practised.

This document explains both and points to the code that implements them.

---

## 1. When to review: FSRS-6

### The forgetting curve
Memory decays over time, and every successful review makes it decay more slowly
(Ebbinghaus, 1885). FSRS describes each card with three numbers, the **DSR model**:

| Symbol | Name | Meaning |
|---|---|---|
| **R** | Retrievability | Probability of recalling the card *right now* (0–1) |
| **S** | Stability | Days until R falls from 100 % to 90 % |
| **D** | Difficulty | How hard the card is for you, 1–10 |

FSRS-6 uses a power-law forgetting curve with a trainable decay $w_{20}$:

$$R(t, S) = \left(1 + F \cdot \frac{t}{S}\right)^{-w_{20}}, \qquad F = 0.9^{-1/w_{20}} - 1$$

$F$ is chosen so that $R(S, S) = 0.9$. Inverting the curve gives the **next interval** for a chosen
desired retention $r$:

$$I(r, S) = \frac{S}{F}\left(r^{-1/w_{20}} - 1\right)$$

At $r = 0.9$ the interval equals the stability. Raising $r$ to 0.95 shortens intervals. Lowering it
to 0.8 lengthens them, at the cost of more forgetting. → `FsrsAlgorithm.retrievability`, `FsrsAlgorithm.interval`

### Updating memory after an answer
Grades are $G \in \{1{:}\text{Again}, 2{:}\text{Hard}, 3{:}\text{Good}, 4{:}\text{Easy}\}$.

**First review**: $S_0 = w_{G-1}$, $\;D_0 = w_4 - e^{w_5 (G-1)} + 1$

**Difficulty** (with linear damping and mean reversion towards $D_0(4)$):

$$D' = w_7 \, D_0(4) + (1 - w_7)\left(D - w_6 (G-3) \cdot \frac{10 - D}{9}\right)$$

**Successful recall** ($G \ge 2$):

$$S' = S\left(1 + e^{w_8}\,(11 - D)\,S^{-w_9}\,\left(e^{w_{10}(1-R)} - 1\right)\cdot h \cdot b\right)$$

with the hard penalty $h = w_{15}$ (only for Hard) and the easy bonus $b = w_{16}$ (only for Easy).
Two properties matter for learning:

- **The lower R was, the bigger the gain.** Remembering something you had almost forgotten
  strengthens memory more than re-confirming something you know well. Effortful retrieval works
  better than easy retrieval.
- **Stable memories grow more slowly** ($S^{-w_9}$), and **hard items grow more slowly** ($11 - D$).

**Lapse** ($G = 1$):

$$S'_f = \min\left(w_{11}\,D^{-w_{12}}\,\left((S+1)^{w_{13}} - 1\right)e^{w_{14}(1-R)},\; \frac{S}{e^{w_{17} w_{18}}}\right)$$

**Same-day review** (learning steps): $S' = S \cdot e^{w_{17}(G - 3 + w_{18})} \cdot S^{-w_{19}}$, never lower than $S$ for Good or Easy.

The 21 default weights are the published FSRS-6 defaults, trained on review logs from about 10,000 Anki users.
→ `core/srs/FsrsAlgorithm.kt`

### Learning steps, graduation and fuzz
New and lapsed cards first go through short **learning steps** (default 1 min → 10 min; relearning 10 min),
like in Anki. They come back *within the same session*, which uses short-term spacing. After the last step
the card *graduates* to day-based intervals. Long intervals get a small random **fuzz**
(±15 % for 2.5–7 days, ±10 % up to 20 days, ±5 % beyond), so words learned together drift apart over time.
→ `core/srs/FsrsScheduler.kt`

### Verification
`FsrsSchedulerTest` reproduces the interval sequence of the official `py-fsrs` 6 test-suite
(`0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21` days for Good×6, Again×2, Good×5). This shows the
Kotlin port is numerically equivalent to the reference implementation.

### Session planning
→ `core/learning/session/SessionPlanner.kt`
- **Learning cards first.** Short-term memory is the most fragile.
- **Reviews sorted by lowest retrievability.** If the daily limit is hit, the words closest to being
  forgotten are the ones that get reviewed.
- **Siblings are buried.** At most one card per word per session. Otherwise the recognition card
  (*Hund → dog*) would give away the production card (*dog → Hund*) minutes later.
- **New cards are interleaved** with reviews (one new card after every four reviews), and decks are
  mixed round-robin. Interleaving improves discrimination between similar items (Kornell & Bjork, 2008).
- **A study day starts at 4 am.** Late-night sessions still count for the previous day, as in Anki.

---

## 2. How to practise: the exercise ladder

### Retrieval practice instead of re-reading
Every Engram exercise asks you to *retrieve* information. Testing yourself improves long-term retention
much more than studying the same material again: the **testing effect** (Roediger & Karpicke, 2006).

### Desirable difficulties
Retrieval should be **effortful but successful** (Bjork, 1994). If it is too easy, little is learned.
If it is too hard, it fails and frustrates. Engram picks the exercise format from the card's current
**stability S**, so the difficulty grows with the memory:

| Memory stability S | Recognition card (word → meaning) | Production card (meaning → word) |
|---|---|---|
| new | *Exposure*: presentation (word, meaning, example, audio, memory hook) | not yet introduced |
| < 1 day | *Recognition*: multiple choice | *Recognition*: reverse multiple choice |
| 1–4 days | multiple choice | *Cued recall*: letter tiles · reverse multiple choice |
| 4–14 days | multiple choice · listening | letter tiles · cloze (gap in the example) · *free recall*: typing |
| ≥ 14 days | listening · self-graded flashcard | typing · cloze · speaking |

→ `core/learning/exercise/ExerciseSelector.kt`

A new word is **presented first** and then **tested a few cards later**, not immediately. A short delay
before the first retrieval strengthens encoding more than an immediate repeat (expanding retrieval practice).
→ `StudySession.completePresentation`

### Receptive before productive
Understanding a word (recognition) usually comes before using it (production) (Nation, 2001). Each note
therefore has two cards. The **production card** (meaning → word) is introduced only *after* the
recognition card has left the "new" state. It also climbs further up the ladder, up to typing and speaking.

### The generation effect
Information you produce yourself is remembered better than information you only read
(Slamecka & Graf, 1978). Typing, cloze and speaking exercises make you generate the word.

### Plausible distractors
Multiple-choice options are chosen to *look like* the answer: similar length, same first letter.
Learners then have to recall the word instead of ruling options out by their surface.
→ `DistractorPicker`

### Automatic grading
Learners are poor judges of their own memory. Engram therefore derives the FSRS grade from behaviour
(→ `RatingPolicy`):

| Outcome | Grade |
|---|---|
| Wrong | Again |
| Typo, wrong/missing article, synonym, hint used, or very slow | Hard |
| Correct recognition or cued recall | Good (never Easy: recognising is easier than recalling) |
| Correct, fluent free recall | Easy |
| Otherwise correct | Good |

The *Flashcards* mode keeps classic self-grading for learners who prefer it. Each of the four buttons
shows the interval it would produce.

### Feedback that teaches
Feedback is immediate and **specific** (Hattie & Timperley, 2007). A character-level diff shows
exactly which letters were missing or extra. Missing articles and typos are named as such. Synonyms are
accepted as "almost" and the target word is shown. When a word is forgotten, its memory hook is shown.
"I was right" lets learners overrule the grader for valid alternatives.

### Forgiving about form, strict about substance
Case, punctuation, extra spaces, bracketed notes and, optionally, accents are ignored
(`strasse` = `Straße`). English fillers ("to go", "the house") are optional. The **article of a German
noun is not optional**: grammatical gender is part of knowing the noun, so *die Hund* is graded "almost".
→ `TextNormalizer`, `AnswerEvaluator`

### Dual coding and elaboration
Words are shown *and* spoken (text-to-speech, normal or slow), and listening exercises train the
spoken form (dual coding, Paivio). Example sentences give context, and memory hooks support
elaborative encoding. When a word becomes a **leech** (8 lapses), Engram suggests adding a hook
instead of more repetition, because repetition alone has stopped working for that word.

### Motivation without manipulation
A daily goal, streaks and a reminder help build a habit. The reminder is sent only if there is something
to review **and** the goal is not reached yet. Statistics show **true retention** and an estimate of
**how many words you know right now** (Σ R). This keeps the focus on real progress rather than points.

---

## References
- Bjork, R. A. (1994). Memory and metamemory considerations in the training of human beings. In *Metacognition: Knowing about Knowing*. MIT Press.
- Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). Distributed practice in verbal recall tasks: A review and quantitative synthesis. *Psychological Bulletin, 132*(3).
- Ebbinghaus, H. (1885). *Über das Gedächtnis*. Duncker & Humblot.
- Hattie, J., & Timperley, H. (2007). The power of feedback. *Review of Educational Research, 77*(1).
- Kornell, N., & Bjork, R. A. (2008). Learning concepts and categories: Is spacing the "enemy of induction"? *Psychological Science, 19*(6).
- Nation, I. S. P. (2001). *Learning Vocabulary in Another Language*. Cambridge University Press.
- Paivio, A. (1986). *Mental Representations: A Dual Coding Approach*. Oxford University Press.
- Roediger, H. L., & Karpicke, J. D. (2006). Test-enhanced learning. *Psychological Science, 17*(3).
- Slamecka, N. J., & Graf, P. (1978). The generation effect. *Journal of Experimental Psychology: Human Learning and Memory, 4*(6).
- Ye, J., Su, J., & Cao, Y. (2022). A stochastic shortest path algorithm for optimizing spaced repetition scheduling. *KDD '22*.
- open-spaced-repetition: *The Algorithm* (FSRS wiki) and `py-fsrs`, https://github.com/open-spaced-repetition
