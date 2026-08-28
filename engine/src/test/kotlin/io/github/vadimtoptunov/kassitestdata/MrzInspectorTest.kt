package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.inspect.MrzInspector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
        assertEquals("120415", r.expiryDate!!.value)

        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertTrue(r.expiryDate!!.valid)
        assertTrue(r.personalNumber!!.valid)
        assertTrue(r.composite!!.valid)
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
        assertTrue(r.expiryDate!!.valid)
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
        assertTrue(r.expiryDate!!.valid)
        assertTrue(r.composite!!.valid)
        assertEquals("ERIKSSON", r.surname)
        assertEquals("ANNA MARIA", r.givenNames)
    }

    // ICAO Doc 9303 Part 7 specimen machine-readable visas (Anna Maria Eriksson, fictitious), as
    // reproduced by the Arg0s1080/mrz reference library (visa_mrva_uto / visa_mrvb_uto). Verified
    // by hand against ICAO 7-3-1: doc L8988901C->4, DOB 400907->8, expiry 961210->9.
    private val mrvaLine1 = "V<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
    private val mrvaLine2 = "L8988901C4XXX4009078F96121096ZE184226B<<<<<<"
    private val mrvbLine1 = "V<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<"
    private val mrvbLine2 = "L8988901C4XXX4009078F9612109<<<<<<<<"

    @Test
    fun `MRV-A visa - reference specimen parses, every per-field check digit valid, no composite`() {
        val outcome = MrzInspector.inspect(listOf(mrvaLine1, mrvaLine2)) as MrzInspector.Outcome.Success
        val r = outcome.result

        assertEquals(MrzInspector.Format.MRVA, r.format) // 2x44 + 'V' -> visa, not TD3
        assertEquals("V", r.documentType)
        assertEquals("UTO", r.issuingCountry)
        assertEquals("XXX", r.nationality)
        assertEquals("F", r.sex)
        assertEquals("ERIKSSON", r.surname)
        assertEquals("ANNA MARIA", r.givenNames)
        assertEquals("L8988901C", r.documentNumber.value)
        assertEquals("400907", r.dateOfBirth.value)
        assertEquals("961210", r.expiryDate!!.value)

        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertTrue(r.expiryDate!!.valid)
        assertNull(r.personalNumber)
        assertNull(r.composite) // MRV has no composite check digit
        assertTrue(r.allChecksValid)
    }

    @Test
    fun `MRV-B visa - reference specimen parses, every per-field check digit valid, no composite`() {
        val r = (MrzInspector.inspect(listOf(mrvbLine1, mrvbLine2)) as MrzInspector.Outcome.Success).result

        assertEquals(MrzInspector.Format.MRVB, r.format) // 2x36 + 'V' -> visa, not TD2
        assertEquals("V", r.documentType)
        assertEquals("L8988901C", r.documentNumber.value)
        assertEquals("400907", r.dateOfBirth.value)
        assertEquals("961210", r.expiryDate!!.value)
        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertTrue(r.expiryDate!!.valid)
        assertNull(r.composite)
        assertTrue(r.allChecksValid)
    }

    @Test
    fun `MRV-A - a corrupted expiry check digit is caught`() {
        val corrupted = mrvaLine2.substring(0, 27) + "0" + mrvaLine2.substring(28) // expiry check 9 -> 0
        val r = (MrzInspector.inspect(listOf(mrvaLine1, corrupted)) as MrzInspector.Outcome.Success).result
        assertFalse(r.expiryDate!!.valid)
        assertEquals('9', r.expiryDate!!.expectedCheckDigit)
        assertTrue(r.documentNumber.valid)
        assertFalse(r.allChecksValid)
    }

    // Pre-2021 French national identity card specimens, with every parsed field and check digit as
    // published by the established npm `mrz` package (github.com/cheminfo/mrz), which ships a
    // dedicated French-national-ID parser: src/parse/__tests__/frenchNationalId.test.ts.
    //   Specimen A (with administrative code): doc number 1710GVA12345 -> check 1, DOB 911231 -> 1, composite 2.
    //   Specimen B (no administrative code):   doc number 940992310285 -> check 4, DOB 651206 -> 8, composite 4.
    // The three check digits are the ICAO 7-3-1 routine over, respectively, line-2 [0..12), the
    // birth date, and (line 1) + (line-2 [0..35)) — verified against the fixtures above.
    private val frIdA = listOf(
        "IDFRATEST<NAME<<<<<<<<<<<<<<<<0CHE02",
        "1710GVA123451ROBERTA<<<<<<<9112311F2",
    )
    private val frIdB = listOf(
        "IDFRABERTHIER<<<<<<<<<<<<<<<<<<<<<<<",
        "9409923102854CORINNE<<<<<<<6512068F4",
    )

    @Test
    fun `French national ID - reference specimen A parses with every check digit valid`() {
        val r = (MrzInspector.inspect(frIdA) as MrzInspector.Outcome.Success).result

        assertEquals(MrzInspector.Format.FRENCH_ID, r.format) // "IDFRA" -> French ID, not TD2
        assertEquals("ID", r.documentType)
        assertEquals("FRA", r.issuingCountry)
        assertEquals("TEST NAME", r.surname)
        assertEquals("ROBERTA", r.givenNames)
        assertEquals("F", r.sex)
        assertEquals("1710GVA12345", r.documentNumber.value)
        assertEquals('1', r.documentNumber.checkDigit)
        assertEquals("911231", r.dateOfBirth.value)
        assertEquals('1', r.dateOfBirth.checkDigit)

        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertNull(r.expiryDate)      // the French ID MRZ has no expiry date
        assertNull(r.personalNumber)
        assertEquals('2', r.composite!!.checkDigit)
        assertTrue(r.composite!!.valid)
        assertTrue(r.allChecksValid)
    }

    @Test
    fun `French national ID - reference specimen B (no administrative code) is fully valid`() {
        val r = (MrzInspector.inspect(frIdB) as MrzInspector.Outcome.Success).result

        assertEquals(MrzInspector.Format.FRENCH_ID, r.format)
        assertEquals("BERTHIER", r.surname)
        assertEquals("CORINNE", r.givenNames)
        assertEquals("940992310285", r.documentNumber.value)
        assertEquals("651206", r.dateOfBirth.value)
        assertTrue(r.documentNumber.valid)  // published check digit 4
        assertTrue(r.dateOfBirth.valid)     // published check digit 8
        assertTrue(r.composite!!.valid)     // published composite 4
        assertTrue(r.allChecksValid)
    }

    @Test
    fun `French national ID - a corrupted composite check digit is caught`() {
        val badLine2 = frIdB[1].substring(0, 35) + "5" // composite 4 -> 5
        val r = (MrzInspector.inspect(listOf(frIdB[0], badLine2)) as MrzInspector.Outcome.Success).result
        assertFalse(r.composite!!.valid)
        assertEquals('4', r.composite!!.expectedCheckDigit) // inspector reports the correct digit
        assertTrue(r.documentNumber.valid)
        assertTrue(r.dateOfBirth.valid)
        assertFalse(r.allChecksValid)
    }
}
