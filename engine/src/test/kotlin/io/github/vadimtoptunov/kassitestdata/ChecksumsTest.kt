package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Anchors each algorithm to published reference values, so the checksum code is verified
 * against the real world and not just against itself.
 */
class ChecksumsTest {

    @Test
    fun `IBAN mod-97 accepts known-valid and rejects corrupted`() {
        assertTrue(Checksums.isValidIbanMod97("GB82 WEST 1234 5698 7654 32"))
        assertTrue(Checksums.isValidIbanMod97("DE89370400440532013000"))
        assertTrue(Checksums.isValidIbanMod97("NL91ABNA0417164300"))
        assertFalse(Checksums.isValidIbanMod97("GB82WEST12345698765433")) // last digit changed
        assertFalse(Checksums.isValidIbanMod97("DE90370400440532013000")) // check digits changed
    }

    @Test
    fun `Luhn accepts known test PANs and rejects off-by-one`() {
        assertTrue(Checksums.isLuhnValid("4242424242424242"))
        assertTrue(Checksums.isLuhnValid("378282246310005"))
        assertFalse(Checksums.isLuhnValid("4242424242424241"))
    }

    @Test
    fun `Dutch 11-proef reference`() {
        assertTrue(Checksums.isValidElevenProof("123456782"))
        assertFalse(Checksums.isValidElevenProof("123456789"))
        assertFalse(Checksums.isValidElevenProof("000000000"))
    }

    @Test
    fun `Australian TFN reference`() {
        assertTrue(Checksums.isValidTfn("123456782"))
        assertFalse(Checksums.isValidTfn("123456781"))
    }

    @Test
    fun `Australian ABN reference (ABR sample)`() {
        assertTrue(Checksums.isValidAbn("51824753556"))
        assertFalse(Checksums.isValidAbn("51824753557"))
    }

    @Test
    fun `German VAT (USt-IdNr) reference`() {
        assertTrue(Checksums.isValidGermanVat("136695976"))
        assertFalse(Checksums.isValidGermanVat("136695975"))
        assertEquals(6, Checksums.mod1110CheckDigit("13669597"))
    }

    @Test
    fun `UK VAT modulo-97 reference`() {
        assertTrue(Checksums.isValidUkVat("980780684"))
        assertFalse(Checksums.isValidUkVat("980780685"))
    }

    @Test
    fun `Cyprus VAT check-letter reference`() {
        assertEquals('P', Checksums.cyprusVatCheckLetter("10259033"))
        assertTrue(Checksums.isValidCyprusVat("10259033P"))
        assertFalse(Checksums.isValidCyprusVat("10259033Q"))
    }

    @Test
    fun `ICAO 7-3-1 check digit reference`() {
        // Classic ICAO 9303 passport-number example: "L898902C" → check digit 3.
        assertEquals(3, Checksums.icao731CheckDigit("L898902C"))
    }

    @Test
    fun `ISO 6346 container reference`() {
        // Canonical ISO 6346 example CSQU3054383 → check digit 3.
        assertTrue(Checksums.isValidIso6346("CSQU3054383"))
        assertFalse(Checksums.isValidIso6346("CSQU3054384"))
    }

    @Test
    fun `Finnish ALV VAT reference`() {
        // python-stdnum reference: 20774740 is a valid Finnish VAT.
        assertTrue(Checksums.isValidFinnishVat("20774740"))
        assertFalse(Checksums.isValidFinnishVat("20774741"))
    }

    @Test
    fun `Danish CVR VAT reference`() {
        // python-stdnum reference: 13585628 is a valid Danish CVR.
        assertTrue(Checksums.isValidDanishVat("13585628"))
        assertFalse(Checksums.isValidDanishVat("13585629"))
    }

    @Test
    fun `Norwegian MVA orgnr reference`() {
        // python-stdnum reference: 988077917 is a valid Norwegian orgnr.
        assertTrue(Checksums.isValidNorwegianVat("988077917"))
        assertFalse(Checksums.isValidNorwegianVat("988077918"))
    }

    @Test
    fun `Spanish IBAN national BBAN control reference`() {
        // Well-known valid ES IBAN example.
        assertTrue(Checksums.isValidSpanishIbanBban("ES9121000418450200051332"))
        assertFalse(Checksums.isValidSpanishIbanBban("ES9121000418440200051332"))
    }
}
