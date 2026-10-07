package com.ahadporkar.engram.core.learning.session

import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.model.StudyCard
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.FsrsScheduler
import java.time.Instant
import kotlin.random.Random

data class SessionLimits(
    val newCards: Int,
    val reviews: Int,
    /** Insert one new card after every N reviews (interleaving). */
    val interleaveEvery: Int = 4,
)

/**
 * Builds the initial queue of a study session.
 *
 * 1. Learning cards first, by due time (short-term memory is the most fragile).
 * 2. Due reviews, **lowest retrievability first** — the cards closest to being forgotten.
 * 3. New cards, oldest first, recognition before production of the same word
 *    (receptive knowledge precedes productive knowledge).
 * 4. Siblings are buried: one card per note per session, so the recognition card does not
 *    give away the production card minutes later.
 * 5. New cards are interleaved with reviews instead of being blocked together.
 */
class SessionPlanner(private val scheduler: FsrsScheduler) {

    fun plan(candidates: List<StudyCard>, now: Instant, dayEnd: Instant, limits: SessionLimits): List<StudyCard> {
        val active = candidates.filterNot { it.card.suspended }

        val learning = active
            .filter { it.card.scheduling.isInLearningSteps && it.card.scheduling.due.isBefore(dayEnd) }
            .sortedBy { it.card.scheduling.due }
        val usedNotes = learning.mapTo(mutableSetOf()) { it.note.id }

        val reviews = active
            .filter { it.card.phase == CardPhase.REVIEW && it.card.scheduling.due.isBefore(dayEnd) }
            .map { it to (scheduler.retrievability(it.card.scheduling, now) ?: 1.0) }
            .sortedWith(compareBy<Pair<StudyCard, Double>> { it.second }.thenBy { it.first.card.scheduling.due })
            .map { it.first }
        val pickedReviews = takeUniqueNotes(reviews, limits.reviews, usedNotes)

        val newCards = active.filter { it.card.isNew }
        val notesWithNewRecognition = newCards
            .filter { it.card.direction == CardDirection.RECOGNITION }
            .mapTo(mutableSetOf()) { it.note.id }
        val orderedNew = newCards
            .filter { it.card.direction == CardDirection.RECOGNITION || it.note.id !in notesWithNewRecognition }
            .sortedWith(compareBy({ it.note.createdAt }, { it.note.id }, { it.card.direction.ordinal }))
        val pickedNew = takeUniqueNotes(orderedNew, limits.newCards, usedNotes)

        return learning + interleave(pickedReviews, pickedNew, limits.interleaveEvery)
    }

    /**
     * Practice queue that does not touch the schedule. Uses one card per note
     * (the recognition card when it exists), like the word list of the original app.
     */
    fun planCram(
        candidates: List<StudyCard>,
        order: CramOrder,
        now: Instant,
        limit: Int,
        random: Random = Random.Default,
    ): List<StudyCard> {
        val perNote = candidates
            .filterNot { it.card.suspended }
            .groupBy { it.note.id }
            .values
            .map { cards -> cards.minBy { it.card.direction.ordinal } }
        val ordered = when (order) {
            CramOrder.SHUFFLE -> perNote.shuffled(random)
            CramOrder.OLDEST_FIRST -> perNote.sortedWith(compareBy({ it.note.createdAt }, { it.note.id }))
            CramOrder.NEWEST_FIRST -> perNote.sortedWith(compareByDescending<StudyCard> { it.note.createdAt }.thenByDescending { it.note.id })
            CramOrder.STARRED -> perNote.filter { it.note.starred }.shuffled(random)
            CramOrder.WEAKEST_FIRST -> perNote
                .map { it to (scheduler.retrievability(it.card.scheduling, now) ?: 0.0) }
                .sortedBy { it.second }
                .map { it.first }
        }
        return ordered.take(limit.coerceAtLeast(0))
    }

    private fun takeUniqueNotes(cards: List<StudyCard>, limit: Int, usedNotes: MutableSet<Long>): List<StudyCard> {
        val picked = mutableListOf<StudyCard>()
        for (card in cards) {
            if (picked.size >= limit) break
            if (usedNotes.add(card.note.id)) picked += card
        }
        return picked
    }

    private fun interleave(reviews: List<StudyCard>, newCards: List<StudyCard>, every: Int): List<StudyCard> {
        if (every <= 0 || reviews.isEmpty()) return reviews + newCards
        val result = ArrayList<StudyCard>(reviews.size + newCards.size)
        var nextNew = 0
        reviews.forEachIndexed { index, review ->
            result += review
            if ((index + 1) % every == 0 && nextNew < newCards.size) result += newCards[nextNew++]
        }
        while (nextNew < newCards.size) result += newCards[nextNew++]
        return result
    }
}
