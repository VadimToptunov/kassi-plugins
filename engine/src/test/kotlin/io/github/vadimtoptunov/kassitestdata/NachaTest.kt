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

    @Test
    fun `an entry with an addenda produces a type-7 record and counts it in the control totals`() {
        val file = Nacha.generateFile(
            entries = listOf(
                Nacha.Entry("22", "021000021", "12345678", 100_00, "ALICE", addenda = "INV 7 PAYMENT"),
                Nacha.Entry("22", "011000015", "98765432", 200_00, "BOB"),
            ),
            originRouting = "121000248",
        )
        val lines = file.split("\n")
        val addenda = lines.filter { it[0] == '7' }
        assertEquals(1, addenda.size, "one addenda record")
        assertEquals("05", addenda.first().substring(1, 3), "payment-related addenda type code")
        // The entry carrying the addenda has its addenda-record indicator (position 79, 0-based 78) = '1'.
        assertEquals('1', lines.first { it[0] == '6' }[78])
        // Entry/addenda count in the File Control (positions 14-21) counts 2 entries + 1 addenda = 3.
        val fileControl = lines.last { it[0] == '9' && it.substring(0, 2) != "99" }
        assertEquals(3L, fileControl.substring(13, 21).toLong())
        assertTrue(Nacha.validate(file).isEmpty(), "addenda file is valid: ${Nacha.validate(file)}")
    }

    @Test
    fun `the seeded sample file includes a type-7 addenda and still validates`() {
        val file = Nacha.sampleFile(Rng(3L), valid = true)
        assertTrue(file.split("\n").any { it.startsWith("705") }, "sample has a payment-related addenda")
        assertTrue(Nacha.validate(file).isEmpty())
    }

    @Test
    fun `an unrecognised addenda type code is flagged`() {
        val file = Nacha.sampleFile(Rng(3L), valid = true)
        val lines = file.split("\n").toMutableList()
        val addendaIdx = lines.indexOfFirst { it[0] == '7' }
        lines[addendaIdx] = "7" + "07" + lines[addendaIdx].substring(3) // 07 is not a valid addenda type code
        val problems = Nacha.validate(lines.joinToString("\n"))
        assertTrue(problems.any { it.message.contains("Addenda type code") }, "expected an addenda problem, got $problems")
    }
}
