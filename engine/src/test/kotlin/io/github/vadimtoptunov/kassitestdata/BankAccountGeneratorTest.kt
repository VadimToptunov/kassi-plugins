package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BankAccountGeneratorTest {

    /** Independent IBAN mod-97 check via BigInteger — deliberately a different implementation
     *  than the production digit-by-digit routine, so the test is not circular. */
    private fun referenceIbanValid(iban: String): Boolean {
        val s = iban.replace(" ", "").uppercase()
        val rearranged = s.substring(4) + s.substring(0, 4)
        val numeric = buildString {
            for (c in rearranged) {
                if (c in '0'..'9') append(c) else append((c - 'A' + 10).toString())
            }
        }
        return BigInteger(numeric).mod(BigInteger.valueOf(97)) == BigInteger.ONE
    }

    @Test
    fun `every IBAN country produces valid IBANs with correct length, and invalid variants fail`() {
        val rng = Rng(20240726L)
        for (country in IbanRegistry.supportedCountries) {
            val spec = IbanRegistry.specFor(country)!!
            repeat(50) {
                val valid = BankAccountGenerator.iban(country, rng, valid = true)
                assertEquals(spec.length, valid.length, "${country.code} IBAN length")
                assertTrue(valid.startsWith(country.code), "${country.code} IBAN prefix")
                assertTrue(referenceIbanValid(valid), "${country.code} IBAN should pass mod-97: $valid")

                val invalid = BankAccountGenerator.iban(country, rng, valid = false)
                assertEquals(spec.length, invalid.length, "${country.code} invalid IBAN length preserved")
                assertFalse(referenceIbanValid(invalid), "${country.code} invalid IBAN should fail mod-97: $invalid")
            }
        }
    }

    @Test
    fun `GB domestic identifier is well formed and derives from a valid IBAN`() {
        val rng = Rng(1L)
        repeat(20) {
            val value = BankAccountGenerator.gbSortCodeAndAccount(rng)
            assertTrue(Regex("Sort code \\d{2}-\\d{2}-\\d{2} · Account \\d{8}").matches(value), value)
        }
    }

    @Test
    fun `AU BSB identifier is well formed`() {
        val rng = Rng(1L)
        repeat(20) {
            val value = BankAccountGenerator.auBsbAndAccount(rng)
            assertTrue(Regex("BSB \\d{3}-\\d{3} · Account \\d{6,9}").matches(value), value)
        }
    }

    @Test
    fun `Australia has no IBAN spec (forces the abstraction)`() {
        assertFalse(IbanRegistry.supportedCountries.contains(Country.AU))
    }
}
