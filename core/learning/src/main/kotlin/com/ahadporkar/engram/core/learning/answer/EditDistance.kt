package com.ahadporkar.engram.core.learning.answer

import kotlin.math.abs
import kotlin.math.min

object EditDistance {

    /**
     * Optimal-string-alignment Damerau–Levenshtein distance: insertions, deletions, substitutions
     * and transpositions of adjacent characters ("teh" → "the") all cost 1.
     *
     * Stops early and returns `limit + 1` once the distance is known to exceed [limit].
     */
    fun damerauLevenshtein(a: String, b: String, limit: Int = Int.MAX_VALUE - 1): Int {
        if (a == b) return 0
        if (a.isEmpty()) return min(b.length, limit + 1)
        if (b.isEmpty()) return min(a.length, limit + 1)
        if (abs(a.length - b.length) > limit) return limit + 1

        val m = b.length
        var twoBack = IntArray(m + 1)
        var previous = IntArray(m + 1) { it }
        var current = IntArray(m + 1)

        for (i in 1..a.length) {
            current[0] = i
            var rowMin = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = min(value, twoBack[j - 2] + 1)
                }
                current[j] = value
                rowMin = min(rowMin, value)
            }
            if (rowMin > limit) return limit + 1
            val recycled = twoBack
            twoBack = previous
            previous = current
            current = recycled
        }
        return min(previous[m], limit + 1)
    }
}
