package com.ahadporkar.engram.core.data.importer

import com.ahadporkar.engram.core.data.repository.NoteDraft

/**
 * Parses CSV / TSV word lists, including Anki "Notes in Plain Text" exports.
 *
 * - Delimiter is detected (tab, semicolon or comma).
 * - Quoted fields with embedded delimiters, quotes ("") and line breaks are supported (RFC 4180).
 * - Lines starting with `#` (Anki headers such as `#separator:tab`) are skipped.
 * - An optional header row maps columns by name; otherwise columns are
 *   front, back, synonyms, example, tags.
 */
object DelimitedTextParser {

    private val htmlTag = Regex("<[^>]+>")

    private val columnAliases = mapOf(
        Column.FRONT to setOf("front", "word", "term", "question", "vorderseite", "wort", "begriff"),
        Column.BACK to setOf("back", "meaning", "definition", "translation", "answer", "rückseite", "bedeutung", "übersetzung"),
        Column.SYNONYMS to setOf("synonyms", "synonym", "synonyme"),
        Column.EXAMPLE to setOf("example", "sentence", "beispiel", "beispielsatz"),
        Column.MNEMONIC to setOf("mnemonic", "notes", "note", "eselsbrücke", "notiz"),
        Column.TAGS to setOf("tags", "tag", "schlagwörter"),
    )

    private enum class Column { FRONT, BACK, SYNONYMS, EXAMPLE, MNEMONIC, TAGS }

    data class Result(val drafts: List<NoteDraft>, val skippedRows: Int)

    fun parse(text: String): Result {
        val content = text.removePrefix("\uFEFF")
        val dataLines = content.lineSequence().filterNot { it.startsWith("#") }.joinToString("\n")
        val delimiter = detectDelimiter(dataLines)
        val rows = splitRows(dataLines, delimiter).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return Result(emptyList(), 0)

        val header = headerMapping(rows.first())
        val mapping = header ?: listOf(Column.FRONT, Column.BACK, Column.SYNONYMS, Column.EXAMPLE, Column.TAGS)
            .mapIndexed { index, column -> column to index }
            .toMap()
        val body = if (header != null) rows.drop(1) else rows

        var skipped = 0
        val drafts = body.mapNotNull { row ->
            fun field(column: Column) = mapping[column]?.let { row.getOrNull(it) }?.let(::clean).orEmpty()
            val front = field(Column.FRONT)
            val back = field(Column.BACK)
            if (front.isEmpty() || back.isEmpty()) {
                skipped++
                null
            } else {
                NoteDraft(
                    front = front,
                    back = back,
                    synonyms = field(Column.SYNONYMS),
                    example = field(Column.EXAMPLE),
                    mnemonic = field(Column.MNEMONIC),
                    tags = field(Column.TAGS).split(' ', ',').filter { it.isNotBlank() }.toSet(),
                )
            }
        }
        return Result(drafts, skipped)
    }

    fun detectDelimiter(text: String): Char {
        val sample = text.lineSequence().take(SAMPLE_LINES).toList()
        return listOf('\t', ';', ',').maxBy { delimiter -> sample.sumOf { line -> line.count { it == delimiter } } }
            .takeIf { delimiter -> sample.any { delimiter in it } }
            ?: '\t'
    }

    /** RFC 4180 style splitter that handles quotes and quoted line breaks. */
    fun splitRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' && (inQuotes || field.isEmpty()) -> inQuotes = !inQuotes
                !inQuotes && c == delimiter -> {
                    row += field.toString()
                    field.clear()
                }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += field.toString()
                    field.clear()
                    rows += row
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }

    private fun headerMapping(firstRow: List<String>): Map<Column, Int>? {
        val mapping = mutableMapOf<Column, Int>()
        firstRow.forEachIndexed { index, raw ->
            val name = raw.trim().lowercase()
            columnAliases.entries.firstOrNull { name in it.value }?.let { mapping.putIfAbsent(it.key, index) }
        }
        return mapping.takeIf { Column.FRONT in it && Column.BACK in it }
    }

    private fun clean(value: String): String = value
        .replace("<br>", " ")
        .replace("<br/>", " ")
        .replace("<br />", " ")
        .replace(htmlTag, "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()

    private const val SAMPLE_LINES = 20
}
