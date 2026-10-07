package com.ahadporkar.engram.core.model

import java.time.Instant

/**
 * One vocabulary item. A note produces one or two [Card]s:
 * a recognition card (front → back) and optionally a production card (back → front).
 *
 * Successor of `Word(word, meaning, synonyms)` from the 2019 ProjectShaco app.
 */
data class Note(
    val id: Long = 0,
    val deckId: Long,
    /** The term being learned (old `Word.word`). */
    val front: String,
    /** Meaning / translation (old `Word.meaning`). Alternatives can be separated by `;` or `,`. */
    val back: String,
    /** Other accepted words with the same meaning (old `Word.synonyms`). */
    val synonyms: String = "",
    /** Example sentence that contains the term — enables cloze exercises. */
    val example: String = "",
    /** Personal memory hook (elaborative encoding). */
    val mnemonic: String = "",
    val tags: Set<String> = emptySet(),
    /** "Saved word" bookmark from the old app. */
    val starred: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Which way a card is asked. */
enum class CardDirection {
    /** Show the term, recall its meaning (receptive knowledge). */
    RECOGNITION,

    /** Show the meaning, produce the term (productive knowledge). */
    PRODUCTION,
}
