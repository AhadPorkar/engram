package com.ahadporkar.engram.core.data.repository

/** Fields of a note to import — no ids, no timestamps. */
data class NoteDraft(
    val front: String,
    val back: String,
    val synonyms: String = "",
    val example: String = "",
    val mnemonic: String = "",
    val tags: Set<String> = emptySet(),
)
