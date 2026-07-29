package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NationalIdGeneratorTest {

    @Test
    fun `every supported national ID scheme - valid passes, invalid fails`() {
        val rng = Rng(2024L)
        for (country in NationalIdGenerator.supported.keys) {
            repeat(50) {
                val valid = NationalIdGenerator.generate(country, rng, valid = true)
                assertTrue(NationalIdGenerator.isValid(country, valid), "${country.code} valid: $valid")

                val invalid = NationalIdGenerator.generate(country, rng, valid = false)
                assertFalse(NationalIdGenerator.isValid(country, invalid), "${country.code} invalid: $invalid")
            }
        }
    }

    @Test
    fun `NINo respects prefix and suffix rules`() {
        val rng = Rng(5L)
        repeat(50) {
            val nino = NationalIdGenerator.generate(Country.GB, rng, valid = true)
            assertTrue(NationalIdGenerator.isValidNino(nino), nino)
            assertTrue(nino.last() in "ABCD", "suffix: $nino")
        }
        assertFalse(NationalIdGenerator.isValidNino("BG123456A")) // disallowed prefix BG
        assertFalse(NationalIdGenerator.isValidNino("DA123456A")) // D not allowed as first letter
    }
}
