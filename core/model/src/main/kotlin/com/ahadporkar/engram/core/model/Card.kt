package com.ahadporkar.engram.core.model

import com.ahadporkar.engram.core.srs.CardPhase
import com.ahadporkar.engram.core.srs.SchedulingState

/** A schedulable question generated from a [Note]. */
data class Card(
    val id: Long = 0,
    val noteId: Long,
    val deckId: Long,
    val direction: CardDirection,
    val scheduling: SchedulingState,
    val suspended: Boolean = false,
    /** Forgotten so often that it needs a better mnemonic rather than more repetitions. */
    val leech: Boolean = false,
) {
    val phase: CardPhase get() = scheduling.phase
    val isNew: Boolean get() = scheduling.phase == CardPhase.NEW
}

/** Card + note + deck languages: everything a study screen needs. */
data class StudyCard(
    val card: Card,
    val note: Note,
    val frontLanguage: String = "en-US",
    val backLanguage: String = "en-US",
) {
    /** Text shown as the question. */
    val prompt: String
        get() = if (card.direction == CardDirection.RECOGNITION) note.front else note.back

    /** Text expected as the answer. */
    val answer: String
        get() = if (card.direction == CardDirection.RECOGNITION) note.back else note.front

    val promptLanguage: String
        get() = if (card.direction == CardDirection.RECOGNITION) frontLanguage else backLanguage

    val answerLanguage: String
        get() = if (card.direction == CardDirection.RECOGNITION) backLanguage else frontLanguage
}
