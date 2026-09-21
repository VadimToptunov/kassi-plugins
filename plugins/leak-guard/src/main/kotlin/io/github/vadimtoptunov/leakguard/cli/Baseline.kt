package io.github.vadimtoptunov.leakguard.cli

import java.io.File

/**
 * The baseline snapshot: `leak-guard-baseline.json` = `{ "schemaVersion": 1, "fingerprints": [...] }`.
 * It is just the *set* of accepted fingerprints — a finding is **NEW** iff its fingerprint is not in
 * this set, and only NEW findings fail the build. This lets the guard go onto a repo with existing,
 * known-and-triaged matches without a wall of red. Only fingerprints (hashes) are stored — never the
 * matched secret itself.
 */
data class Baseline(val fingerprints: Set<String>) {

    fun isNew(fingerprint: String): Boolean = fingerprint !in fingerprints

    companion object {
        const val SCHEMA_VERSION = 1

        val EMPTY = Baseline(emptySet())

        fun read(file: File): Baseline = parse(file.readText())

        /** Parses the baseline document. Only the `fingerprints` string array is read; the parse is
         *  deliberately forgiving (the schema is a flat, known shape). */
        fun parse(json: String): Baseline {
            val key = "\"fingerprints\""
            val keyAt = json.indexOf(key)
            if (keyAt < 0) return EMPTY
            val open = json.indexOf('[', keyAt)
            if (open < 0) return EMPTY
            val close = json.indexOf(']', open)
            if (close < 0) return EMPTY
            val body = json.substring(open + 1, close)
            val fps = Regex("\"([^\"]*)\"").findAll(body).map { it.groupValues[1] }.toCollection(LinkedHashSet())
            return Baseline(fps)
        }

        /** Serializes a fresh baseline from the given fingerprints (order preserved, de-duplicated). */
        fun serialize(fingerprints: Collection<String>): String {
            val unique = fingerprints.toCollection(LinkedHashSet())
            return buildString {
                append("{\n")
                append("  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n")
                append("  \"fingerprints\": [")
                if (unique.isEmpty()) {
                    append("]\n")
                } else {
                    append('\n')
                    val items = unique.toList()
                    for ((i, fp) in items.withIndex()) {
                        append("    \"").append(fp).append('"')
                        if (i < items.size - 1) append(',')
                        append('\n')
                    }
                    append("  ]\n")
                }
                append("}\n")
            }
        }

        fun write(file: File, fingerprints: Collection<String>) {
            file.writeText(serialize(fingerprints))
        }
    }
}
