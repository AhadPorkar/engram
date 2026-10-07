package com.ahadporkar.engram.core.learning.answer

import java.text.Normalizer
import java.util.Locale

/**
 * Normalises typed answers so that only meaningful differences count:
 * case, punctuation, extra spaces, bracketed notes and (optionally) Latin/Greek/Cyrillic accents.
 * Marks in scripts where they carry meaning (Arabic, Persian, Devanagari, ...) are kept.
 */
object TextNormalizer {
    private val bracketed = Regex("""\([^)]*\)|\[[^\]]*]""")
    private val accentMarks = Regex("""(?<=[\p{IsLatin}\p{IsGreek}\p{IsCyrillic}])\p{Mn}+""")
    private val apostrophes = Regex("""['’‘`´]""")
    private val punctuation = Regex("""[\p{P}\p{S}]""")
    private val whitespace = Regex("""\s+""")

    private val ligatures = mapOf(
        "ß" to "ss",
        "æ" to "ae",
        "œ" to "oe",
        "ø" to "o",
        "đ" to "d",
        "ł" to "l",
    )

    fun normalize(text: String, ignoreAccents: Boolean = true): String {
        var s = text.replace(bracketed, " ").lowercase(Locale.ROOT)
        if (ignoreAccents) {
            s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(accentMarks, "")
            ligatures.forEach { (from, to) -> s = s.replace(from, to) }
        }
        s = Normalizer.normalize(s, Normalizer.Form.NFC)
        return s.replace(apostrophes, " ")
            .replace(punctuation, " ")
            .replace(whitespace, " ")
            .trim()
    }
}

/**
 * Leading words that learners often add or drop: English fillers ("to go", "the house") and
 * gendered articles ("der Hund", "la maison"). Fillers are ignored; a missing or wrong
 * gendered article is reported, because grammatical gender is part of knowing a noun.
 */
object LeadingWords {
    val fillers: Set<String> = setOf("to", "the", "a", "an")

    val genderedArticles: Set<String> = setOf(
        // German
        "der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer", "eines",
        // French
        "le", "la", "les", "l", "un", "une",
        // Spanish / Italian / Portuguese
        "el", "los", "las", "il", "lo", "gli", "uno", "una", "o", "os", "as",
        // Dutch
        "de", "het", "een",
    )

    data class Split(val leading: String?, val rest: String)

    /** Splits an already normalised string into an optional leading word and the rest. */
    fun split(normalized: String): Split {
        val space = normalized.indexOf(' ')
        if (space <= 0) return Split(null, normalized)
        val first = normalized.substring(0, space)
        val rest = normalized.substring(space + 1).trim()
        return if (rest.isNotEmpty() && (first in fillers || first in genderedArticles)) {
            Split(first, rest)
        } else {
            Split(null, normalized)
        }
    }

    /** Removes a leading filler/article from raw (not normalised) text, keeping the original casing. */
    fun stripRaw(text: String): String {
        val trimmed = text.trim()
        val space = trimmed.indexOfFirst { it.isWhitespace() }
        if (space <= 0) return trimmed
        val first = trimmed.substring(0, space).lowercase(Locale.ROOT).trimEnd('\'', '’')
        val rest = trimmed.substring(space).trim()
        return if (rest.isNotEmpty() && (first in fillers || first in genderedArticles)) rest else trimmed
    }
}

/** Splits a field such as "to go; to walk / to travel" into accepted alternatives. */
object AnswerAlternatives {
    private val separators = Regex("""\s*[;,/|\n]\s*""")

    fun split(text: String): List<String> {
        val parts = text.split(separators).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return parts.ifEmpty { listOfNotNull(text.trim().takeIf { it.isNotEmpty() }) }
    }
}
