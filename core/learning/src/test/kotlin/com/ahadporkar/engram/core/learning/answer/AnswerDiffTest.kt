package com.ahadporkar.engram.core.learning.answer

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerDiffTest {

    @Test
    fun `identical answers are one match segment`() {
        assertEquals(listOf(DiffSegment("Haus", DiffKind.MATCH)), AnswerDiff.diff("haus", "Haus"))
    }

    @Test
    fun `missing and extra letters are highlighted`() {
        val diff = AnswerDiff.diff("Schmeterlingg", "Schmetterling")
        val rebuiltExpected = diff.filter { it.kind != DiffKind.EXTRA }.joinToString("") { it.text }
        val rebuiltTyped = diff.filter { it.kind != DiffKind.MISSING }.joinToString("") { it.text.lowercase() }
        assertEquals("Schmetterling", rebuiltExpected)
        assertEquals("schmeterlingg", rebuiltTyped)
        assertEquals(1, diff.count { it.kind == DiffKind.MISSING })
        assertEquals(1, diff.count { it.kind == DiffKind.EXTRA })
    }

    @Test
    fun `empty input means everything is missing`() {
        assertEquals(listOf(DiffSegment("Hund", DiffKind.MISSING)), AnswerDiff.diff("", "Hund"))
    }
}
