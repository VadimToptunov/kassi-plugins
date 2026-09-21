package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.inspect.IcaoTransliteration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Anchored to the transliteration examples published in ICAO Doc 9303 Part 3 (transliteration of
 * Latin-based national characters into the MRZ) — the canonical outputs `MUELLER`, `GAUSS`,
 * `AASTROEM`, `PENA`, etc. — not values recomputed by this code.
 */
class IcaoTransliterationTest {

    @Test
    fun `German umlauts and eszett expand per ICAO 9303`() {
        assertEquals("MUELLER", IcaoTransliteration.transliterate("Müller"))
        assertEquals("GAUSS", IcaoTransliteration.transliterate("Gauß"))
        assertEquals("STRASSE", IcaoTransliteration.transliterate("Straße"))
        assertEquals("GOEDEL", IcaoTransliteration.transliterate("Gödel"))
    }

    @Test
    fun `Nordic ligatures and rings expand`() {
        assertEquals("AASTROEM", IcaoTransliteration.transliterate("Åström"))
        assertEquals("BJOERN", IcaoTransliteration.transliterate("Bjørn"))
        assertEquals("AEBLE", IcaoTransliteration.transliterate("Æble"))
    }

    @Test
    fun `diacritics are folded to the base letter`() {
        assertEquals("PENA", IcaoTransliteration.transliterate("Peña"))
        assertEquals("CURACAO", IcaoTransliteration.transliterate("Curaçao"))
        assertEquals("WALESA", IcaoTransliteration.transliterate("Wałęsa"))
        assertEquals("DVORAK", IcaoTransliteration.transliterate("Dvořák"))
    }

    @Test
    fun `spaces and hyphens become the filler, other punctuation is dropped`() {
        assertEquals("ANNA<MARIA", IcaoTransliteration.transliterate("Anna Maria"))
        assertEquals("ANNE<MARIE", IcaoTransliteration.transliterate("Anne-Marie"))
        assertEquals("ONEILL", IcaoTransliteration.transliterate("O'Neill"))
    }

    @Test
    fun `an already-Latin name is unchanged apart from case`() {
        assertEquals("ERIKSSON", IcaoTransliteration.transliterate("Eriksson"))
    }

    @Test
    fun `name field lays out primary and secondary identifiers with double filler`() {
        assertEquals("ERIKSSON<<ANNA<MARIA", IcaoTransliteration.toMrzNameField("Eriksson", "Anna Maria"))
        assertEquals("MUELLER<<HANS", IcaoTransliteration.toMrzNameField("Müller", "Hans"))
        assertEquals("SMITH", IcaoTransliteration.toMrzNameField("Smith", ""))
    }
}
