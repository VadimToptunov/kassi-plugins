package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.inspect.MrzInspector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MrzInspectorTest {

    // The standard ICAO Doc 9303 Part 4 sample passport MRZ (Anna Maria Eriksson, fictitious).
    private val td3Line1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
    private val td3Line2 = "L898902C36UTO7408122F1204159ZE184226B<<<<<10"

    @Test
    fun `TD3 passport - reference sample parses with every check digit valid`() {
        val outcome = MrzInspector.inspect(listOf(td3Line1, td3Line2)) as MrzInspector.Outcome.Success
        val r = outcome.result

        assertEquals(MrzInspector.Format.TD3, r.format)
        assertEquals("P", r.documentType)
        assertEquals("UTO", r.issuingCountry)
        assertEquals("UTO", r.nationality)
        assertEquals("F", r.sex)
        assertEquals("ERIKSSON", r.surname)
        assertEquals("ANNA MARIA", r.givenNames)
        assertEquals("L898902C3", r.documentNumber.value)
        assertEquals("740812", r.dateOfBirth.value)
        assertEquals("120415", r.expiryDate.value)

        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertTrue(r.expiryDate.valid)
        assertTrue(r.personalNumber!!.valid)
        assertTrue(r.composite.valid)
        assertTrue(r.allChecksValid)
    }

    @Test
    fun `TD3 - a single corrupted check digit is caught without failing the others`() {
        val corruptedLine2 = td3Line2.substring(0, 9) + "5" + td3Line2.substring(10) // doc-number check digit 6 -> 5
        val outcome = MrzInspector.inspect(listOf(td3Line1, corruptedLine2)) as MrzInspector.Outcome.Success
        val r = outcome.result

        assertFalse(r.documentNumber.valid)
        assertEquals('6', r.documentNumber.expectedCheckDigit) // the inspector reports the correct digit
        assertTrue(r.dateOfBirth.valid)
        assertTrue(r.expiryDate.valid)
        assertFalse(r.allChecksValid)
    }

    @Test
    fun `unrecognized line shape reports what was expected`() {
        val outcome = MrzInspector.inspect(listOf("TOO SHORT")) as MrzInspector.Outcome.Invalid
        assertTrue(outcome.reason.contains("TD3"))
    }

    @Test
    fun `TD1 ID card - well-formed sample round-trips through the composite check`() {
        // Built by construction: document number + its check digit feed the same composite formula
        // validated against the TD3 reference above, just re-laid-out to the TD1 field positions.
        val docNumber = "D23145890"
        val docCheck = io.github.vadimtoptunov.kassitestdata.algo.Checksums.icao731CheckDigit(docNumber)
        val optional1 = "<".repeat(15)
        val dob = "740812"
        val dobCheck = io.github.vadimtoptunov.kassitestdata.algo.Checksums.icao731CheckDigit(dob)
        val expiry = "120415"
        val expiryCheck = io.github.vadimtoptunov.kassitestdata.algo.Checksums.icao731CheckDigit(expiry)
        val nationality = "UTO"
        val optional2 = "<".repeat(11)
        val compositeInput = docNumber + docCheck + optional1 + dob + dobCheck + expiry + expiryCheck + optional2
        val compositeCheck = io.github.vadimtoptunov.kassitestdata.algo.Checksums.icao731CheckDigit(compositeInput)

        val line1 = "I<UTO" + docNumber + docCheck + optional1
        val line2 = dob + dobCheck + "F" + expiry + expiryCheck + nationality + optional2 + compositeCheck
        val line3 = "ERIKSSON<<ANNA<MARIA" + "<".repeat(10)

        val outcome = MrzInspector.inspect(listOf(line1, line2, line3)) as MrzInspector.Outcome.Success
        val r = outcome.result

        assertEquals(MrzInspector.Format.TD1, r.format)
        assertEquals("I", r.documentType)
        assertEquals("UTO", r.issuingCountry)
        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertTrue(r.expiryDate.valid)
        assertTrue(r.composite.valid)
        assertEquals("ERIKSSON", r.surname)
        assertEquals("ANNA MARIA", r.givenNames)
    }
}
