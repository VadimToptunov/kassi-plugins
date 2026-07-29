package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonaGeneratorTest {

    @Test
    fun `same seed reproduces the same persona, different seed differs`() {
        val a = PersonaGenerator.generate(Country.DE, seed = 12345L)
        val b = PersonaGenerator.generate(Country.DE, seed = 12345L)
        val c = PersonaGenerator.generate(Country.DE, seed = 99999L)
        assertEquals(a.formatted(), b.formatted())
        assertTrue(a.formatted() != c.formatted())
    }

    @Test
    fun `persona identifiers are internally valid and country-consistent`() {
        for (country in Country.entries) {
            val p = PersonaGenerator.generate(country, seed = 777L)
            assertEquals(country, p.country)

            // IBAN countries: the persona's IBAN must actually pass mod-97.
            if (io.github.vadimtoptunov.kassitestdata.data.IbanRegistry.specFor(country) != null) {
                val iban = p.bankValue.replace(" ", "")
                assertTrue(Checksums.isValidIbanMod97(iban), "${country.code} persona IBAN invalid: $iban")
            }

            // Where the persona carries a national ID / VAT, it must be valid too.
            if (p.nationalId != null) {
                assertTrue(NationalIdGenerator.isValid(country, p.nationalId!!), "${country.code} persona national ID")
            }
            assertTrue(BicIsForCountry(p.bic, country))
        }
    }

    @Test
    fun `Australian persona uses BSB, not IBAN`() {
        val p = PersonaGenerator.generate(Country.AU, seed = 1L)
        assertTrue(p.bankValue.startsWith("BSB "), p.bankValue)
        assertNotNull(p.taxId) // ABN
    }

    private fun BicIsForCountry(bic: String, country: Country): Boolean =
        bic.length >= 6 && bic.substring(4, 6) == country.code
}
