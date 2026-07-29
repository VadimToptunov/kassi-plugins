package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.TaxIdGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaxIdGeneratorTest {

    private val rng = Rng(31337L)

    @Test
    fun `Cyprus VAT valid passes and invalid fails its check letter`() {
        repeat(50) {
            assertTrue(Checksums.isValidCyprusVat(TaxIdGenerator.cyprusVatBody(rng, valid = true)))
            assertFalse(Checksums.isValidCyprusVat(TaxIdGenerator.cyprusVatBody(rng, valid = false)))
        }
    }

    @Test
    fun `German VAT valid passes and invalid fails MOD 11,10`() {
        repeat(50) {
            assertTrue(Checksums.isValidGermanVat(TaxIdGenerator.germanVatBody(rng, valid = true)))
            assertFalse(Checksums.isValidGermanVat(TaxIdGenerator.germanVatBody(rng, valid = false)))
        }
    }

    @Test
    fun `Netherlands VAT body carries an 11-proef 9-digit block`() {
        repeat(50) {
            val nine = TaxIdGenerator.netherlandsVatNineDigits(rng)
            assertTrue(Checksums.isValidElevenProof(nine), nine)
        }
    }

    @Test
    fun `UK VAT valid passes and invalid fails modulo-97`() {
        repeat(50) {
            assertTrue(Checksums.isValidUkVat(TaxIdGenerator.ukVatBody(rng, valid = true)))
            assertFalse(Checksums.isValidUkVat(TaxIdGenerator.ukVatBody(rng, valid = false)))
        }
    }

    @Test
    fun `Australian ABN valid passes and invalid fails modulo-89`() {
        repeat(50) {
            assertTrue(Checksums.isValidAbn(TaxIdGenerator.abn(rng, valid = true)))
            assertFalse(Checksums.isValidAbn(TaxIdGenerator.abn(rng, valid = false)))
        }
    }

    @Test
    fun `presented VAT identifiers carry the country prefix`() {
        assertTrue(TaxIdGenerator.generate(io.github.vadimtoptunov.kassitestdata.core.Country.DE, rng).startsWith("DE"))
        assertTrue(TaxIdGenerator.generate(io.github.vadimtoptunov.kassitestdata.core.Country.CY, rng).startsWith("CY"))
    }
}
