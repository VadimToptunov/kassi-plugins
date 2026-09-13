package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.inspect.PiiAnonymizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PiiAnonymizerTest {

    @Test
    fun `replaces card, IBAN, SSN and email with valid-synthetic equivalents`() {
        val iban = BankAccountGenerator.iban(Country.DE, Rng(1L), valid = true)
        val text = "user a.b@corp.com paid with 4111111111111111 to $iban, ssn 123-45-6789."
        val out = PiiAnonymizer.anonymize(text)
        assertEquals(4, out.count)

        // originals gone
        assertFalse(out.text.contains("a.b@corp.com"))
        assertFalse(out.text.contains("4111111111111111"))
        assertFalse(out.text.contains(iban))
        assertFalse(out.text.contains("123-45-6789"))

        // replacements are valid + reserved-shaped
        assertTrue(out.text.contains("@example.com"))
        val newPan = Regex("""\b\d{16}\b""").find(out.text)!!.value
        assertTrue(Checksums.isLuhnValid(newPan), "replacement PAN must be Luhn-valid")
        val newIban = Regex("""\bDE\d{20}\b""").find(out.text)!!.value
        assertTrue(Checksums.isValidIbanMod97(newIban), "replacement IBAN must pass mod-97")
    }

    @Test
    fun `is deterministic and coherent — same original maps to the same replacement`() {
        val text = "primary 4111111111111111 and again 4111111111111111"
        val out = PiiAnonymizer.anonymize(text)
        val pans = Regex("""\b\d{16}\b""").findAll(out.text).map { it.value }.toList()
        assertEquals(2, pans.size)
        assertEquals(pans[0], pans[1], "the same card must anonymize to the same replacement")
        assertEquals(out.text, PiiAnonymizer.anonymize(text).text, "must be deterministic across runs")
    }

    @Test
    fun `leaves clean text untouched, and does not treat an SSN as a card`() {
        val clean = "port=8080 version=1.2.3 name=orders_table"
        assertEquals(clean, PiiAnonymizer.anonymize(clean).text)
        assertEquals(0, PiiAnonymizer.anonymize(clean).count)
        // an SSN (9 digits) must not be Luhn-replaced as a card
        val ssnOut = PiiAnonymizer.anonymize("ssn 123-45-6789")
        assertFalse(ssnOut.text.contains("123-45-6789"))
        assertEquals(1, ssnOut.count) // one replacement (the SSN), not two
    }
}
