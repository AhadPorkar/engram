package com.ahadporkar.engram.core.learning.answer

enum class DiffKind {
    /** Typed correctly. */
    MATCH,

    /** Expected but not typed. */
    MISSING,

    /** Typed but not expected. */
    EXTRA,
}

data class DiffSegment(val text: String, val kind: DiffKind)

/**
 * Character-level diff between the typed answer and the expected one, used to show *exactly*
 * where a spelling went wrong (error-specific feedback beats a plain "wrong").
 */
object AnswerDiff {
    private const val MAX_LENGTH = 200

    fun diff(typed: String, expected: String): List<DiffSegment> {
        if (typed.length > MAX_LENGTH || expected.length > MAX_LENGTH) {
            return listOfNotNull(
                typed.takeIf { it.isNotEmpty() }?.let { DiffSegment(it, DiffKind.EXTRA) },
                expected.takeIf { it.isNotEmpty() }?.let { DiffSegment(it, DiffKind.MISSING) },
            )
        }
        val a = typed
        val b = expected
        // lcs[i][j] = length of the longest common subsequence of a[i..] and b[j..]
        val lcs = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in a.length - 1 downTo 0) {
            for (j in b.length - 1 downTo 0) {
                lcs[i][j] = if (same(a[i], b[j])) {
                    lcs[i + 1][j + 1] + 1
                } else {
                    maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }

        val segments = mutableListOf<DiffSegment>()
        fun emit(char: Char, kind: DiffKind) {
            val last = segments.lastOrNull()
            if (last != null && last.kind == kind) {
                segments[segments.lastIndex] = last.copy(text = last.text + char)
            } else {
                segments += DiffSegment(char.toString(), kind)
            }
        }

        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            when {
                same(a[i], b[j]) -> {
                    emit(b[j], DiffKind.MATCH)
                    i++
                    j++
                }
                lcs[i + 1][j] >= lcs[i][j + 1] -> emit(a[i++], DiffKind.EXTRA)
                else -> emit(b[j++], DiffKind.MISSING)
            }
        }
        while (i < a.length) emit(a[i++], DiffKind.EXTRA)
        while (j < b.length) emit(b[j++], DiffKind.MISSING)
        return segments
    }

    private fun same(x: Char, y: Char) = x.lowercaseChar() == y.lowercaseChar()
}
