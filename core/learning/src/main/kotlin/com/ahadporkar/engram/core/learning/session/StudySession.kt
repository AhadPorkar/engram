package com.ahadporkar.engram.core.learning.session

import com.ahadporkar.engram.core.model.Card
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.LeechAction
import com.ahadporkar.engram.core.model.ReviewLog
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.FsrsScheduler
import com.ahadporkar.engram.core.srs.Rating
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

/** One card waiting in the session queue. */
data class SessionEntry(
    val item: StudyCard,
    /** New card already shown with a presentation in this session. */
    val presented: Boolean = false,
    /** How many times it was answered in this session. */
    val attempts: Int = 0,
)

data class SessionProgress(
    val answered: Int = 0,
    val correct: Int = 0,
    val firstTryCorrect: Int = 0,
    val newIntroduced: Int = 0,
    val leeches: Int = 0,
    val studyMillis: Long = 0,
) {
    val accuracy: Double? get() = if (answered == 0) null else correct.toDouble() / answered
}

data class RemainingCounts(val new: Int, val learning: Int, val review: Int) {
    val total: Int get() = new + learning + review
}

/** Result of answering the current card. */
data class AnswerApplied(
    val card: Card,
    /** `null` in cram mode — the schedule is not touched. */
    val log: ReviewLog?,
    val becameLeech: Boolean,
    val rating: Rating,
)

/** What to write back to storage when an answer is undone. */
data class UndoResult(
    val entry: SessionEntry,
    /** Card state before the answer, `null` if nothing was persisted (presentation / cram). */
    val previousCard: Card?,
    val logId: Long?,
)

/**
 * In-memory state machine of a study session. Pure Kotlin: no Android, no database —
 * the ViewModel persists [AnswerApplied] results and calls [markPersisted].
 *
 * - New cards are presented, then re-inserted [presentationGap] cards later for the first test
 *   (a short retrieval delay strengthens encoding more than immediate repetition).
 * - Cards in (re)learning steps come back when their step is due (minutes later), so the session
 *   itself is spaced. Steps due within [learnAhead] are shown early when nothing else is left.
 * - Cram mode re-asks wrong cards a few positions later until they are answered correctly.
 */
class StudySession(
    initial: List<StudyCard>,
    val mode: StudyMode,
    private val scheduler: FsrsScheduler,
    private val leechThreshold: Int = 8,
    private val leechAction: LeechAction = LeechAction.TAG_ONLY,
    private val learnAhead: Duration = Duration.ofMinutes(20),
    private val presentationGap: Int = 2,
    private val cramRetryGap: Int = 3,
) {
    private data class Waiting(val entry: SessionEntry, val due: Instant)

    private data class Snapshot(
        val queue: List<SessionEntry>,
        val waiting: List<Waiting>,
        val current: SessionEntry,
        val progress: SessionProgress,
        val previousCard: Card?,
        var logId: Long? = null,
    )

    private val queue = ArrayDeque(initial.map { SessionEntry(it) })
    private val waiting = mutableListOf<Waiting>()
    private val history = ArrayDeque<Snapshot>()

    val plannedCount: Int = initial.size

    var current: SessionEntry? = null
        private set

    var progress = SessionProgress()
        private set

    val canUndo: Boolean get() = history.isNotEmpty()

    /** Advances to the next card, or returns `null` when nothing is left for now. */
    fun next(now: Instant): SessionEntry? {
        current?.let { return it }
        val dueNow = waiting.filter { !it.due.isAfter(now) }.minByOrNull { it.due }
        val entry = when {
            dueNow != null -> waiting.remove(dueNow).let { dueNow.entry }
            queue.isNotEmpty() -> queue.removeFirst()
            else -> waiting
                .filter { !it.due.isAfter(now.plus(learnAhead)) }
                .minByOrNull { it.due }
                ?.also { waiting.remove(it) }
                ?.entry
        }
        current = entry
        return entry
    }

    /** Learning cards that will be due later (beyond the learn-ahead window). */
    fun laterLearningCount(now: Instant): Int = waiting.count { it.due.isAfter(now.plus(learnAhead)) }

    fun nextLearningDue(): Instant? = waiting.minOfOrNull { it.due }

    fun remaining(): RemainingCounts {
        val entries = queue + waiting.map { it.entry } + listOfNotNull(current)
        return RemainingCounts(
            new = entries.count { it.item.card.isNew },
            learning = entries.count { it.item.card.scheduling.isInLearningSteps },
            review = entries.count { it.item.card.phase == CardPhase.REVIEW },
        )
    }

    /** Fraction of the session done, for the progress bar. */
    fun completion(): Float {
        val left = remaining().total
        val done = progress.answered + progress.newIntroduced
        return if (done + left == 0) 1f else done.toFloat() / (done + left)
    }

    /** The current new card has been shown; it will be tested a few cards later. */
    fun completePresentation(durationMillis: Long = 0) {
        val entry = checkNotNull(current) { "No current card" }
        history.addLast(Snapshot(queue.toList(), waiting.toList(), entry, progress, previousCard = null))
        trimHistory()
        queue.add(min(presentationGap, queue.size), entry.copy(presented = true))
        progress = progress.copy(
            newIntroduced = progress.newIntroduced + 1,
            studyMillis = progress.studyMillis + durationMillis,
        )
        current = null
    }

    /** Grades the current card. */
    fun answer(rating: Rating, now: Instant, exercise: ExerciseType, durationMillis: Long): AnswerApplied {
        val entry = checkNotNull(current) { "No current card" }
        val card = entry.item.card
        val persisted = mode != StudyMode.CRAM
        history.addLast(
            Snapshot(queue.toList(), waiting.toList(), entry, progress, previousCard = card.takeIf { persisted }),
        )
        trimHistory()

        val firstTry = entry.attempts == 0
        progress = progress.copy(
            answered = progress.answered + 1,
            correct = progress.correct + if (rating.isSuccess) 1 else 0,
            firstTryCorrect = progress.firstTryCorrect + if (firstTry && rating.isSuccess) 1 else 0,
            studyMillis = progress.studyMillis + durationMillis,
        )
        current = null

        if (!persisted) {
            if (rating == Rating.AGAIN) {
                queue.add(min(cramRetryGap, queue.size), entry.copy(attempts = entry.attempts + 1))
            }
            return AnswerApplied(card, log = null, becameLeech = false, rating = rating)
        }

        val result = scheduler.review(card.scheduling, rating, now)
        val lapsed = card.phase == CardPhase.REVIEW && rating == Rating.AGAIN
        val becameLeech = lapsed && isLeechLapse(result.state.lapses)
        val updated = card.copy(
            scheduling = result.state,
            leech = card.leech || becameLeech,
            suspended = card.suspended || (becameLeech && leechAction == LeechAction.SUSPEND),
        )
        if (becameLeech) progress = progress.copy(leeches = progress.leeches + 1)

        if (result.state.isInLearningSteps && !updated.suspended) {
            waiting += Waiting(
                entry.copy(item = entry.item.copy(card = updated), attempts = entry.attempts + 1),
                result.state.due,
            )
        }

        val log = ReviewLog(
            cardId = card.id,
            rating = rating,
            phaseBefore = card.phase,
            reviewedAt = now,
            elapsedDays = result.elapsedDays,
            scheduledSeconds = result.interval.seconds,
            durationMillis = durationMillis,
            exercise = exercise,
            stabilityAfter = checkNotNull(result.state.stability),
            difficultyAfter = checkNotNull(result.state.difficulty),
            retrievabilityBefore = result.retrievabilityBefore,
        )
        return AnswerApplied(updated, log, becameLeech, rating)
    }

    /** Remembers the database id of the log written for the last answer (needed for undo). */
    fun markPersisted(logId: Long) {
        history.lastOrNull()?.logId = logId
    }

    /** Reverts the last answer or presentation. The returned entry becomes current again. */
    fun undo(): UndoResult? {
        val snapshot = history.removeLastOrNull() ?: return null
        queue.clear()
        queue.addAll(snapshot.queue)
        waiting.clear()
        waiting.addAll(snapshot.waiting)
        progress = snapshot.progress
        current = snapshot.current
        return UndoResult(snapshot.current, snapshot.previousCard, snapshot.logId)
    }

    /** Anki semantics: a leech at the threshold, then again every half-threshold lapses. */
    private fun isLeechLapse(lapses: Int): Boolean {
        if (leechThreshold <= 0 || lapses < leechThreshold) return false
        val every = max(1, leechThreshold / 2)
        return (lapses - leechThreshold) % every == 0
    }

    private fun trimHistory() {
        while (history.size > MAX_UNDO) history.removeFirst()
    }

    private companion object {
        const val MAX_UNDO = 30
    }
}
