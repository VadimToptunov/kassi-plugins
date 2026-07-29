package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.BicGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BicGeneratorTest {

    @Test
    fun `BIC is structurally valid, carries the country code, and is 8 or 11 chars`() {
        val rng = Rng(42L)
        for (country in Country.entries) {
            repeat(10) {
                val bic = BicGenerator.bic(country, rng)
                assertTrue(BicGenerator.isStructurallyValid(bic), "structure: $bic")
                assertEquals(country.code, bic.substring(4, 6), "country code in BIC: $bic")
                assertTrue(bic.length == 8 || bic.length == 11, "length: $bic")
            }
        }
    }

    @Test
    fun `invalid BIC fails the structural check`() {
        val rng = Rng(42L)
        repeat(10) {
            assertFalse(BicGenerator.isStructurallyValid(BicGenerator.invalidBic(Country.DE, rng)))
        }
    }
}
