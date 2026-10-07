package com.ahadporkar.engram.core.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsAlgorithmTest {

    private val fsrs = FsrsAlgorithm()

    @Test
    fun `retrievability is 90 percent when elapsed time equals stability`() {
        listOf(0.5, 1.0, 7.0, 42.0, 365.0).forEach { s ->
            assertEquals(0.9, fsrs.retrievability(s, s), 1e-9)
        }
    }

    @Test
    fun `interval equals stability at 90 percent retention`() {
        listOf(1.0, 3.3, 77.0).forEach { s -> assertEquals(s, fsrs.interval(s, 0.9), 1e-9) }
    }

    @Test
    fun `interval is the inverse of retrievability`() {
        val s = 12.0
        val t = fsrs.interval(s, 0.85)
        assertEquals(0.85, fsrs.retrievability(t, s), 1e-9)
    }

    @Test
    fun `initial stability uses the first four weights`() {
        assertEquals(0.212, fsrs.initialStability(Rating.AGAIN), 1e-12)
        assertEquals(1.2931, fsrs.initialStability(Rating.HARD), 1e-12)
        assertEquals(2.3065, fsrs.initialStability(Rating.GOOD), 1e-12)
        assertEquals(8.2956, fsrs.initialStability(Rating.EASY), 1e-12)
    }

    @Test
    fun `initial difficulty decreases with better ratings and stays in bounds`() {
        val values = Rating.entries.map { fsrs.initialDifficulty(it) }
        assertEquals(values.sortedDescending(), values)
        values.forEach { assertTrue(it in 1.0..10.0) }
        assertEquals(2.118104, fsrs.initialDifficulty(Rating.GOOD), 1e-6)
    }

    @Test
    fun `difficulty is clamped to 1 to 10`() {
        var d = 5.0
        repeat(100) { d = fsrs.nextDifficulty(d, Rating.AGAIN) }
        assertTrue(d <= 10.0)
        repeat(200) { d = fsrs.nextDifficulty(d, Rating.EASY) }
        assertTrue(d >= 1.0)
    }

    @Test
    fun `recall at low retrievability increases stability more`() {
        val easyRecall = fsrs.nextRecallStability(5.0, 10.0, 0.95, Rating.GOOD)
        val hardEarnedRecall = fsrs.nextRecallStability(5.0, 10.0, 0.70, Rating.GOOD)
        assertTrue(hardEarnedRecall > easyRecall)
    }

    @Test
    fun `forgetting never increases stability`() {
        listOf(0.5, 3.0, 30.0, 300.0).forEach { s ->
            assertTrue(fsrs.nextForgetStability(5.0, s, 0.8) <= s)
        }
    }

    @Test
    fun `short-term good review never lowers stability`() {
        listOf(0.1, 1.0, 10.0, 100.0).forEach { s ->
            assertTrue(fsrs.shortTermStability(s, Rating.GOOD) >= s)
        }
    }

    @Test
    fun `parameters parse fsrs-6 and upgrade fsrs-5 sets`() {
        val six = FsrsParameters.parse(FsrsParameters.DEFAULT_WEIGHTS.joinToString(", "))
        assertNotNull(six)
        assertEquals(FsrsParameters.DEFAULT, six)

        val five = FsrsParameters.parse(
            "0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192, " +
                "1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621",
        )
        assertNotNull(five)
        assertEquals(0.5, five!!.weights[20], 0.0)
        assertEquals(0.0, five.weights[19], 0.0)

        assertNull(FsrsParameters.parse("1, 2, 3"))
        assertNull(FsrsParameters.parse("abc"))
    }

    @Test
    fun `factor matches the fsrs-6 definition`() {
        assertEquals(0.98034649, FsrsParameters.DEFAULT.factor, 1e-7)
    }
}
