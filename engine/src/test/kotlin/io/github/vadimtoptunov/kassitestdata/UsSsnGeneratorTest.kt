package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.UsSsnGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UsSsnGeneratorTest {

    @Test
    fun `generated SSNs are shaped correctly, sit in the 900 to 999 area, and are never-issued`() {
        val rng = Rng(42L)
        repeat(200) {
            val ssn = UsSsnGenerator.generate(rng)
            assertTrue(Regex("""\d{3}-\d{2}-\d{4}""").matches(ssn), ssn)
            val area = ssn.substring(0, 3).toInt()
            assertTrue(area in 900..999, "area should be in the never-issued 900-999 range: $ssn")
            assertTrue(UsSsnGenerator.isReservedNeverIssued(ssn), "generated SSN should be reserved: $ssn")
            assertFalse(UsSsnGenerator.isPlausiblyReal(ssn), "generated SSN should not look plausibly real: $ssn")
        }
    }

    @Test
    fun `a structurally issuable SSN is plausibly real`() {
        assertTrue(UsSsnGenerator.isPlausiblyReal("123-45-6789"))
        assertFalse(UsSsnGenerator.isReservedNeverIssued("123-45-6789"))
    }

    @Test
    fun `never-issued areas, group 00 and serial 0000 are all reserved, never plausibly real`() {
        for (reserved in listOf("000-12-3456", "666-12-3456", "912-12-3456", "123-00-4567", "123-45-0000")) {
            assertTrue(UsSsnGenerator.isReservedNeverIssued(reserved), reserved)
            assertFalse(UsSsnGenerator.isPlausiblyReal(reserved), reserved)
        }
    }

    @Test
    fun `malformed values are neither reserved nor plausibly real`() {
        for (malformed in listOf("12345-6789", "123-456-789", "not-an-ssn", "")) {
            assertFalse(UsSsnGenerator.isReservedNeverIssued(malformed), malformed)
            assertFalse(UsSsnGenerator.isPlausiblyReal(malformed), malformed)
        }
    }
}
