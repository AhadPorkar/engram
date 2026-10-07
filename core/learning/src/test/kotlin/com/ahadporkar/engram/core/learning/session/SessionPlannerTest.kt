package com.ahadporkar.engram.core.learning.session

import com.ahadporkar.engram.core.learning.TestData
import com.ahadporkar.engram.core.learning.TestData.NOW
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.srs.FsrsScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import kotlin.random.Random

class SessionPlannerTest {

    private val planner = SessionPlanner(FsrsScheduler())
    private val dayEnd = NOW.plus(Duration.ofHours(10))

    @Test
    fun `learning first, then most-at-risk reviews, new cards interleaved`() {
        val learning = TestData.learningCard(TestData.note(1), NOW.plus(Duration.ofMinutes(5)))
        val atRisk = TestData.reviewCard(TestData.note(2), 2.0, NOW.minus(Duration.ofDays(10)), NOW.minus(Duration.ofDays(8)))
        val safe = TestData.reviewCard(TestData.note(3), 30.0, NOW.minus(Duration.ofDays(30)), NOW)
        val newCard = TestData.newCard(TestData.note(4))
        val notDue = TestData.reviewCard(TestData.note(5), 30.0, NOW, NOW.plus(Duration.ofDays(20)))

        val plan = planner.plan(
            listOf(newCard, safe, notDue, atRisk, learning),
            NOW,
            dayEnd,
            SessionLimits(newCards = 5, reviews = 50, interleaveEvery = 1),
        )
        assertEquals(listOf(learning, atRisk, newCard, safe), plan)
    }

    @Test
    fun `daily limits are respected`() {
        val reviews = (1L..10L).map { TestData.reviewCard(TestData.note(it), 5.0, NOW.minus(Duration.ofDays(5)), NOW) }
        val news = (11L..20L).map { TestData.newCard(TestData.note(it)) }
        val plan = planner.plan(reviews + news, NOW, dayEnd, SessionLimits(newCards = 3, reviews = 4))
        assertEquals(4, plan.count { it.card.phase == com.ahadporkar.engram.core.srs.CardPhase.REVIEW })
        assertEquals(3, plan.count { it.card.isNew })
    }

    @Test
    fun `siblings are buried and recognition comes before production`() {
        val note = TestData.note(1)
        val recognition = TestData.newCard(note, CardDirection.RECOGNITION)
        val production = TestData.newCard(note, CardDirection.PRODUCTION)
        val plan = planner.plan(listOf(production, recognition), NOW, dayEnd, SessionLimits(10, 10))
        assertEquals(listOf(recognition), plan)

        val learnedNote = TestData.note(2)
        val reviewA = TestData.reviewCard(learnedNote, 3.0, NOW.minus(Duration.ofDays(4)), NOW, CardDirection.RECOGNITION)
        val reviewB = TestData.reviewCard(learnedNote, 3.0, NOW.minus(Duration.ofDays(4)), NOW, CardDirection.PRODUCTION)
        assertEquals(1, planner.plan(listOf(reviewA, reviewB), NOW, dayEnd, SessionLimits(10, 10)).size)
    }

    @Test
    fun `production card is introduced once the recognition card is no longer new`() {
        val note = TestData.note(1)
        val recognition = TestData.reviewCard(note, 3.0, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(2)))
        val production = TestData.newCard(note, CardDirection.PRODUCTION)
        val plan = planner.plan(listOf(recognition, production), NOW, dayEnd, SessionLimits(10, 10))
        assertEquals(listOf(production), plan)
    }

    @Test
    fun `suspended cards never appear`() {
        val suspended = TestData.newCard(TestData.note(1)).let { it.copy(card = it.card.copy(suspended = true)) }
        assertTrue(planner.plan(listOf(suspended), NOW, dayEnd, SessionLimits(10, 10)).isEmpty())
    }

    @Test
    fun `cram orders mirror the five modes of the old app`() {
        val older = TestData.note(1, front = "alt", createdAt = NOW.minus(Duration.ofDays(9)))
        val newer = TestData.note(2, front = "neu", createdAt = NOW.minus(Duration.ofDays(1)), starred = true)
        val cards = listOf(
            TestData.newCard(newer),
            TestData.newCard(newer, CardDirection.PRODUCTION),
            TestData.newCard(older),
        )
        val oldest = planner.planCram(cards, CramOrder.OLDEST_FIRST, NOW, 50)
        assertEquals(listOf("alt", "neu"), oldest.map { it.note.front })
        assertTrue(oldest.all { it.card.direction == CardDirection.RECOGNITION })

        val newest = planner.planCram(cards, CramOrder.NEWEST_FIRST, NOW, 50)
        assertEquals(listOf("neu", "alt"), newest.map { it.note.front })

        val starred = planner.planCram(cards, CramOrder.STARRED, NOW, 50, Random(1))
        assertEquals(listOf("neu"), starred.map { it.note.front })

        assertEquals(1, planner.planCram(cards, CramOrder.SHUFFLE, NOW, 1).size)
    }
}
