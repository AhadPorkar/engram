package com.ahadporkar.engram.core.data.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class DelimitedTextParserTest {

    @Test
    fun `parses anki plain text exports`() {
        val text = """
            #separator:tab
            #html:true
            der Hund	the dog
            die Katze	<b>the</b>&nbsp;cat
        """.trimIndent()
        val result = DelimitedTextParser.parse(text)
        assertEquals(2, result.drafts.size)
        assertEquals("the cat", result.drafts[1].back)
    }

    @Test
    fun `maps columns by header name in any order`() {
        val text = "meaning;word;example;tags\nthe house;das Haus;Das Haus ist alt.;a1 noun\n"
        val draft = DelimitedTextParser.parse(text).drafts.single()
        assertEquals("das Haus", draft.front)
        assertEquals("the house", draft.back)
        assertEquals("Das Haus ist alt.", draft.example)
        assertEquals(setOf("a1", "noun"), draft.tags)
    }

    @Test
    fun `handles quotes, embedded commas and line breaks`() {
        val text = "front,back,synonyms\n\"to go, to walk\",gehen,\"laufen\"\n\"say \"\"hi\"\"\",\"line one\nline two\",\n"
        val drafts = DelimitedTextParser.parse(text).drafts
        assertEquals(2, drafts.size)
        assertEquals("to go, to walk", drafts[0].front)
        assertEquals("say \"hi\"", drafts[1].front)
        assertEquals("line one\nline two", drafts[1].back)
    }

    @Test
    fun `rows without front or back are skipped and counted`() {
        val result = DelimitedTextParser.parse("a\tb\n\tonly back\nonly front\t\n")
        assertEquals(1, result.drafts.size)
        assertEquals(2, result.skippedRows)
    }

    @Test
    fun `positional columns are used without a header`() {
        val draft = DelimitedTextParser.parse("big;groß;large, huge;Das Haus ist groß.").drafts.single()
        assertEquals("big", draft.front)
        assertEquals("large, huge", draft.synonyms)
        assertEquals("Das Haus ist groß.", draft.example)
    }

    @Test
    fun `byte order mark is ignored`() {
        assertEquals("Haus", DelimitedTextParser.parse("\uFEFFHaus\thouse").drafts.single().front)
    }
}
