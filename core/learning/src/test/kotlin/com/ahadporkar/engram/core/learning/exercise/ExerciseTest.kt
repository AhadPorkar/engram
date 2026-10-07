package com.ahadporkar.engram.core.learning.exercise

import com.ahadporkar.engram.core.learning.TestData
import com.ahadporkar.engram.core.model.CardDirection
import com.ahadporkar.engram.core.model.ExerciseType
import com.ahadporkar.engram.core.model.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import kotlin.random.Random

class ExerciseTest {

    private val pool = DistractorPool(
        fronts = listOf("die Katze", "das Haus", "der Baum", "die Blume", "das Auto"),
        backs = listOf("the cat", "the house", "the tree", "the flower", "the car"),
    )
    private val context = SelectionContext(
        mode = StudyMode.SMART,
        presentedThisSession = false,
        audioAvailable = true,
        speakingEnabled = true,
        pool = pool,
    )
    private val selector = ExerciseSelector(Random(7))
    private val factory = ExerciseFactory(Random(7))

    @Test
    fun `new cards are presented before they are tested`() {
        val card = TestData.newCard(TestData.note(1))
        assertEquals(ExerciseType.PRESENTATION, selector.select(card, context))
        assertEquals(
            ExerciseType.MULTIPLE_CHOICE,
            selector.select(card, context.copy(presentedThisSession = true)),
        )
    }

    @Test
    fun `flashcard mode always uses self-graded flashcards`() {
        val card = TestData.newCard(TestData.note(1))
        assertEquals(ExerciseType.FLASHCARD, selector.select(card, context.copy(mode = StudyMode.FLASHCARDS)))
    }

    @Test
    fun `production cards climb from recognition to free recall as stability grows`() {
        val note = TestData.note(1, example = "Der Hund bellt laut.")
        fun typeAt(stability: Double) = selector.select(
            TestData.reviewCard(note, stability, TestData.NOW, TestData.NOW, CardDirection.PRODUCTION),
            context,
        )
        assertEquals(ExerciseType.REVERSE_MULTIPLE_CHOICE, typeAt(0.5))
        repeat(20) {
            assertTrue(typeAt(2.0) in setOf(ExerciseType.LETTER_TILES, ExerciseType.REVERSE_MULTIPLE_CHOICE))
            assertTrue(typeAt(30.0) in setOf(ExerciseType.TYPING, ExerciseType.CLOZE, ExerciseType.SPEAKING))
        }
    }

    @Test
    fun `listening needs audio and speaking needs the setting`() {
        val note = TestData.note(1)
        val strong = TestData.reviewCard(note, 50.0, TestData.NOW, TestData.NOW)
        val noAudio = context.copy(audioAvailable = false)
        repeat(20) { assertEquals(ExerciseType.FLASHCARD, selector.select(strong, noAudio)) }

        val production = TestData.reviewCard(note, 50.0, TestData.NOW, TestData.NOW, CardDirection.PRODUCTION)
        val noSpeaking = context.copy(speakingEnabled = false)
        repeat(20) { assertNotEquals(ExerciseType.SPEAKING, selector.select(production, noSpeaking)) }
    }

    @Test
    fun `multiple choice falls back without enough distractors`() {
        val card = TestData.reviewCard(TestData.note(1), 0.5, TestData.NOW, TestData.NOW)
        val empty = context.copy(pool = DistractorPool())
        assertEquals(ExerciseType.FLASHCARD, selector.select(card, empty))
    }

    @Test
    fun `choice exercises contain the answer exactly once`() {
        val card = TestData.reviewCard(TestData.note(1), 0.5, TestData.NOW, TestData.NOW)
        val exercise = factory.create(ExerciseType.MULTIPLE_CHOICE, card, pool) as Exercise.Choice
        assertEquals(4, exercise.options.size)
        assertEquals("the dog", exercise.correctOption)
        assertEquals(1, exercise.options.count { it == "the dog" })
        assertEquals(exercise.options.toSet().size, exercise.options.size)
        assertFalse(exercise.audioOnly)

        val listening = factory.create(ExerciseType.LISTENING, card, pool) as Exercise.Choice
        assertTrue(listening.audioOnly)
        assertEquals("de-DE", listening.promptLanguage)
    }

    @Test
    fun `distractors exclude duplicates of the answer and prefer look-alikes`() {
        val picked = DistractorPicker.pick(
            correct = "Haus",
            pool = listOf(
                "haus", "Haus ", "Maus", "Hand", "Laus", "Raus", "Haut", "Bau",
                "Elefantenrüssel", "Kaktusblütenstaub",
            ),
            count = 3,
            random = Random(3),
        )
        assertEquals(3, picked.size)
        assertFalse(picked.any { it.equals("haus", ignoreCase = true) })
        assertFalse("Kaktusblütenstaub" in picked)
        assertFalse("Elefantenrüssel" in picked)
    }

    @Test
    fun `cloze cuts the term out of the example sentence`() {
        val note = TestData.note(1, front = "der Hund", example = "Der Hund bellt laut.")
        val gap = ClozeBuilder.find(note)
        assertNotNull(gap)
        assertEquals("", gap!!.before)
        assertEquals("Der Hund", gap.surface)
        assertEquals(" bellt laut.", gap.after)

        val inflected = TestData.note(2, front = "gehen", example = "Wir gehen nach Hause.")
        assertEquals("gehen", ClozeBuilder.find(inflected)!!.surface)

        val wordStartOnly = TestData.note(3, front = "go", example = "Long ago.")
        assertNull(ClozeBuilder.find(wordStartOnly))
    }

    @Test
    fun `cloze accepts the term without its article`() {
        val note = TestData.note(1, front = "der Hund", example = "Ich sehe den Hund.")
        val item = TestData.reviewCard(note, 20.0, TestData.NOW, TestData.NOW, CardDirection.PRODUCTION)
        val cloze = factory.create(ExerciseType.CLOZE, item, pool) as Exercise.Cloze
        assertEquals("Ich sehe den ", cloze.before)
        assertTrue("Hund" in cloze.accepted)
        assertEquals("H___", cloze.hint)
    }

    @Test
    fun `letter tiles are a real permutation`() {
        val built = LetterTilesBuilder.build("Schmetterling", Random(1))
        assertNotNull(built)
        val (answer, tiles) = built!!
        assertEquals("Schmetterling", answer)
        assertEquals(answer.map { it.toString() }.sorted(), tiles.sorted())
        assertNotEquals(answer.map { it.toString() }, tiles)
        assertFalse(LetterTilesBuilder.canBuild("a"))
    }

    @Test
    fun `typing hints show the first letter of each word`() {
        assertEquals("d__ H___", ExerciseFactory.hintFor("der Hund"))
        assertEquals("t-s____", ExerciseFactory.hintFor("t-shirt"))
    }

    @Test
    fun `impossible formats fall back to typing`() {
        val note = TestData.note(1, example = "")
        val item = TestData.reviewCard(note, 20.0, TestData.NOW, TestData.NOW.plus(Duration.ofDays(1)), CardDirection.PRODUCTION)
        assertTrue(factory.create(ExerciseType.CLOZE, item, pool) is Exercise.Typing)
        assertTrue(factory.create(ExerciseType.REVERSE_MULTIPLE_CHOICE, item, DistractorPool()) is Exercise.Typing)
    }
}
