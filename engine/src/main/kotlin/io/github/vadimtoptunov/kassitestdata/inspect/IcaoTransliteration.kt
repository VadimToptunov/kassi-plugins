package io.github.vadimtoptunov.kassitestdata.inspect

/**
 * ICAO Doc 9303 Part 3 transliteration of Latin-based national characters into the letters an MRZ
 * name field may carry (A–Z and the `<` filler). This is the mapping used when a name printed in the
 * Visual Inspection Zone (with diacritics and ligatures) is rendered into the Machine Readable Zone —
 * e.g. `MÜLLER` → `MUELLER`, `GAUSS`, `ÅSTRÖM` → `AASTROEM`, `PEÑA` → `PENA`.
 *
 * Pure and offline. Only the Latin-script transliteration table is implemented (the documented,
 * unambiguous part of Doc 9303 Part 3); transliteration of non-Latin scripts (Cyrillic, Arabic) is
 * left to the issuing authority and out of scope here.
 */
object IcaoTransliteration {

    /** Multigraph substitutions (one national letter → several MRZ letters), per Doc 9303 Part 3. */
    private val MULTI: Map<Char, String> = mapOf(
        'Ä' to "AE", 'Å' to "AA", 'Æ' to "AE", 'Ö' to "OE", 'Ø' to "OE",
        'Ü' to "UE", 'Œ' to "OE", 'Þ' to "TH", 'Ĳ' to "IJ",
    )

    /** Diacritic folds (accented/marked letter → its base MRZ letter). */
    private val FOLD: Map<Char, Char> = buildMap {
        fun add(base: Char, marked: String) = marked.forEach { put(it, base) }
        add('A', "ÀÁÂÃĀĂĄ")
        add('C', "ÇĆĈĊČ")
        add('D', "ĎĐÐ")
        add('E', "ÈÉÊËĒĔĖĘĚ")
        add('G', "ĜĞĠĢ")
        add('H', "ĤĦ")
        add('I', "ÌÍÎÏĨĪĬĮİ")
        add('J', "Ĵ")
        add('K', "Ķ")
        add('L', "ĹĻĽĿŁ")
        add('N', "ÑŃŅŇ")
        add('O', "ÒÓÔÕŌŎŐ")
        add('R', "ŔŖŘ")
        add('S', "ŚŜŞŠȘ")
        add('T', "ŢŤŦȚ")
        add('U', "ÙÚÛŨŪŬŮŰŲ")
        add('W', "Ŵ")
        add('Y', "ÝŶŸ")
        add('Z', "ŹŻŽ")
    }

    /**
     * Transliterates [name] to its ICAO 9303 MRZ form: uppercase A–Z, national letters mapped per the
     * tables above. Spaces and hyphens (name-component separators) become the filler `<`; any other
     * character that is neither a letter nor a mapped mark is dropped.
     */
    fun transliterate(name: String): String = buildString {
        for (ch in name.uppercase()) {
            when {
                ch in 'A'..'Z' -> append(ch)
                ch == ' ' || ch == '-' -> append('<')
                ch in MULTI -> append(MULTI[ch])
                ch in FOLD -> append(FOLD[ch])
                // else: combining marks, apostrophes, punctuation — dropped, per the recommendation.
            }
        }
    }

    /**
     * Builds the MRZ name field body from a [surname] and [givenNames]: the transliterated primary
     * identifier, then `<<`, then the transliterated secondary identifiers — the exact layout of the
     * name field in a TD1/TD2/TD3 MRZ (before it is padded with `<` to the field width).
     */
    fun toMrzNameField(surname: String, givenNames: String): String {
        val primary = transliterate(surname)
        val secondary = transliterate(givenNames)
        return if (secondary.isEmpty()) primary else "$primary<<$secondary"
    }
}
