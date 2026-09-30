package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.nacha.Nacha
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NachaTest {

    @Test
    fun `ABA routing check digit against real published Federal Reserve routing numbers`() {
        // EXTERNAL anchor: real, publicly published U.S. routing transit numbers, all known-valid.
        // JPMorgan Chase (NY), FRB Boston, Wells Fargo, Bank of America (VA), Citibank (NY).
        for (routing in listOf("021000021", "011000015", "121000248", "051000017", "021000089")) {
            assertTrue(Nacha.isValidRoutingNumber(routing), "$routing should be valid")
            assertEquals(routing[8] - '0', Nacha.routingCheckDigit(routing.take(8)), "check digit of $routing")
        }
        // Tampered numbers must fail.
        assertFalse(Nacha.isValidRoutingNumber("021000022"))
        assertFalse(Nacha.isValidRoutingNumber("12345678"))   // too short
        assertFalse(Nacha.isValidRoutingNumber("02100002X"))  // non-digit
    }

    @Test
    fun `generated routing numbers are always valid`() {
        val rng = Rng(7L)
        repeat(200) { assertTrue(Nacha.isValidRoutingNumber(Nacha.generateRoutingNumber(rng))) }
    }

    @Test
    fun `entry hash sums the 8-digit routing prefixes`() {
        // From the published routing numbers above: 02100002 + 01100001 = 3200003.
        assertEquals(3_200_003L, Nacha.entryHash(listOf("021000021", "011000015")))
    }

    @Test
    fun `a generated file round-trips through the validator`() {
        val file = Nacha.sampleFile(Rng(42L), valid = true)
        val lines = file.split("\n")
        assertTrue(lines.all { it.length == 94 }, "every record is 94 chars")
        assertEquals(0, lines.size % 10, "blocked to a multiple of 10")
        assertTrue(Nacha.validate(file).isEmpty(), "valid file has no problems: ${Nacha.validate(file)}")
    }

    @Test
    fun `the invalid variant is flagged on the total-credit control`() {
        val problems = Nacha.validate(Nacha.sampleFile(Rng(42L), valid = false))
        assertTrue(problems.any { it.message.contains("total credit") }, "expected a total-credit problem, got $problems")
    }

    @Test
    fun `a corrupted entry routing number is caught`() {
        val file = Nacha.sampleFile(Rng(1L), valid = true)
        val lines = file.split("\n").toMutableList()
        val entryIdx = lines.indexOfFirst { it[0] == '6' }
        // Break the check digit (position 12, 1-based) of the first entry's routing number.
        val bad = lines[entryIdx].toCharArray()
        bad[11] = if (bad[11] == '9') '0' else (bad[11] + 1)
        lines[entryIdx] = String(bad)
        val problems = Nacha.validate(lines.joinToString("\n"))
        assertTrue(problems.any { it.message.contains("ABA check digit") }, "expected a routing problem, got $problems")
    }
}
