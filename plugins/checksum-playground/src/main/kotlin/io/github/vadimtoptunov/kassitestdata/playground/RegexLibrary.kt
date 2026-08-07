package io.github.vadimtoptunov.kassitestdata.playground

/**
 * Ready-made regex patterns offered in the Regex tab's picker. Kept as a separate object so a unit
 * test can prove every shipped pattern compiles (a bad escape would otherwise only surface at runtime).
 */
object RegexLibrary {

    /** Label shown to the user → the pattern loaded into the field when picked. */
    val PATTERNS: List<Pair<String, String>> = listOf(
        "Email" to "[\\w.+-]+@[\\w-]+\\.[\\w.-]+",
        "IPv4 address" to "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b",
        "URL (http/https)" to "https?://[^\\s]+",
        "UUID" to "[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}",
        "ISO date (YYYY-MM-DD)" to "\\d{4}-\\d{2}-\\d{2}",
        "Time (HH:MM[:SS])" to "\\d{2}:\\d{2}(?::\\d{2})?",
        "Hex color" to "#[0-9a-fA-F]{6}\\b",
        "IBAN (rough shape)" to "[A-Z]{2}\\d{2}[A-Z0-9]{11,30}",
        "Digits" to "\\d+",
        "Word" to "\\w+",
    )
}
