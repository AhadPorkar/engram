package com.ahadporkar.engram.core.learning.session

import com.ahadporkar.engram.core.learning.TestData
import com.ahadporkar.engram.core.learning.TestData.NOW
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.LeechAction
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.FsrsScheduler
import com.ahadporkar.engram.core.srs.Rating
import com.ahadporkar.engram.core.srs.SchedulerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class StudySessionTest {

    private val scheduler = FsrsScheduler(SchedulerConfig(enableFuzz = false))

    @Test
    fun `a presented card returns for its first test a few cards later`() {
        val cards = (1L..4L).map { TestData.newCard(TestData.note(it)) }
        val session = StudySession(cards, StudyMode.SMART, scheduler, presentationGap = 2)

        val first = session.next(NOW)!!
        assertFalse(first.presented)
        session.completePresentation()

        // Two other cards are shown before the first test of cards[0] (presentationGap = 2).
        assertSame(cards[1], session.next(NOW)!!.item)
        session.completePresentation()
        assertSame(cards[2], session.next(NOW)!!.item)
        session.completePresentation()
        val test = session.next(NOW)!!
        assertSame(cards[0], test.item)
        assertTrue(test.presented)
        assertEquals(3, session.progress.newIntroduced)
    }

    @Test
    fun `learning steps bring a card back within the session`() {
        val card = TestData.newCard(TestData.note(1))
        val session = StudySession(listOf(card), StudyMode.FLASHCARDS, scheduler)

        session.next(NOW)
        val applied = session.answer(Rating.GOOD, NOW, ExerciseType.FLASHCARD, 3_000)
        assertEquals(CardPhase.LEARNING, applied.card.phase)
        assertNotNull(applied.log)
        assertEquals(CardPhase.NEW, applied.log!!.phaseBefore)

        // 10-minute step: shown early because nothing else is left (learn-ahead 20 min).
        val again = session.next(NOW.plus(Duration.ofMinutes(1)))
        assertNotNull(again)
        assertEquals(1, again!!.attempts)
        session.answer(Rating.GOOD, NOW.plus(Duration.ofMinutes(11)), ExerciseType.FLASHCARD, 2_000)
        assertNull(session.next(NOW.plus(Duration.ofMinutes(11))))
        assertEquals(2, session.progress.answered)
    }

    @Test
    fun `learning cards due later than the learn-ahead window wait`() {
        val card = TestData.newCard(TestData.note(1))
        val session = StudySession(listOf(card), StudyMode.FLASHCARDS, scheduler, learnAhead = Duration.ofMinutes(5))
        session.next(NOW)
        session.answer(Rating.GOOD, NOW, ExerciseType.FLASHCARD, 1_000)
        assertNull(session.next(NOW))
        assertEquals(1, session.laterLearningCount(NOW))
        assertNotNull(session.next(NOW.plus(Duration.ofMinutes(10))))
    }

    @Test
    fun `undo restores the card, the queue and the progress`() {
        val cards = (1L..2L).map { TestData.newCard(TestData.note(it)) }
        val session = StudySession(cards, StudyMode.FLASHCARDS, scheduler)
        session.next(NOW)
        session.answer(Rating.AGAIN, NOW, ExerciseType.FLASHCARD, 1_000)
        session.markPersisted(99)
        session.next(NOW)

        val undo = session.undo()
        assertNotNull(undo)
        assertEquals(99L, undo!!.logId)
        assertEquals(cards[0].card, undo.previousCard)
        assertSame(cards[0], session.current!!.item)
        assertEquals(0, session.progress.answered)
        assertEquals(2, session.remaining().new)
        assertFalse(session.canUndo)
    }

    @Test
    fun `cram mode repeats wrong cards and never schedules`() {
        val cards = (1L..3L).map { TestData.newCard(TestData.note(it)) }
        val session = StudySession(cards, StudyMode.CRAM, scheduler, cramRetryGap = 1)
        session.next(NOW)
        val applied = session.answer(Rating.AGAIN, NOW, ExerciseType.MULTIPLE_CHOICE, 1_000)
        assertNull(applied.log)
        assertEquals(cards[0].card, applied.card)

        assertSame(cards[1], session.next(NOW)!!.item)
        session.answer(Rating.GOOD, NOW, ExerciseType.MULTIPLE_CHOICE, 1_000)
        val retry = session.next(NOW)!!
        assertSame(cards[0], retry.item)
        assertEquals(1, retry.attempts)
    }

    @Test
    fun `repeated lapses mark a leech and can suspend it`() {
        val card = TestData.reviewCard(TestData.note(1), 5.0, NOW.minus(Duration.ofDays(6)), NOW, lapses = 7)
        val session = StudySession(
            listOf(card), StudyMode.FLASHCARDS, scheduler,
            leechThreshold = 8, leechAction = LeechAction.SUSPEND,
        )
        session.next(NOW)
        val applied = session.answer(Rating.AGAIN, NOW, ExerciseType.FLASHCARD, 1_000)
        assertTrue(applied.becameLeech)
        assertTrue(applied.card.leech)
        assertTrue(applied.card.suspended)
        assertEquals(8, applied.card.scheduling.lapses)
        assertNull("suspended leeches leave the session", session.next(NOW.plus(Duration.ofHours(1))))
    }

    @Test
    fun `accuracy counts successful answers`() {
        val cards = (1L..2L).map { TestData.reviewCard(TestData.note(it), 5.0, NOW.minus(Duration.ofDays(5)), NOW) }
        val session = StudySession(cards, StudyMode.FLASHCARDS, scheduler)
        session.next(NOW)
        session.answer(Rating.GOOD, NOW, ExerciseType.FLASHCARD, 1_000)
        session.next(NOW)
        session.answer(Rating.AGAIN, NOW, ExerciseType.FLASHCARD, 1_000)
        assertEquals(0.5, session.progress.accuracy!!, 1e-9)
        assertEquals(1, session.progress.firstTryCorrect)
        assertEquals(2_000L, session.progress.studyMillis)
    }
}
