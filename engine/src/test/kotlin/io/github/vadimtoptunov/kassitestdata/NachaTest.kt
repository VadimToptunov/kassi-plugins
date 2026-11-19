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
    fun `each supported SEC code produces a valid file carrying that code in the batch header`() {
        // The SEC code occupies positions 51-53 (0-indexed 50..52) of the type-5 batch header record,
        // per the NACHA Operating Rules / file-format spec.
        for (sec in Nacha.SEC_CODES) {
            val file = Nacha.sampleFile(Rng(7L), valid = true, secCode = sec)
            val batchHeader = file.split("\n").first { it[0] == '5' }
            assertEquals(sec, batchHeader.substring(50, 53), "batch header carries the $sec SEC code")
            assertTrue(Nacha.validate(file).isEmpty(), "$sec file validates: ${Nacha.validate(file)}")
        }
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

    @Test
    fun `return reason codes map to their published Nacha meanings`() {
        // EXTERNAL anchor: the administrative Return Reason Code block (R01–R33) from the Nacha Operating
        // Rules & Guidelines — code and official description as published by Nacha.
        assertEquals("Insufficient Funds", Nacha.RETURN_REASON_CODES["R01"])
        assertEquals("Account Closed", Nacha.RETURN_REASON_CODES["R02"])
        assertEquals("No Account/Unable to Locate Account", Nacha.RETURN_REASON_CODES["R03"])
        assertEquals("Account Frozen/Entry Returned per OFAC Instruction", Nacha.RETURN_REASON_CODES["R16"])
        assertEquals("Return of XCK Entry", Nacha.RETURN_REASON_CODES["R33"])
        assertEquals(33, Nacha.RETURN_REASON_CODES.size, "the R01–R33 administrative block")
    }

    /** Rewrite [line]'s [range] (0-based) with [value], keeping the record exactly 94 chars. */
    private fun splice(line: String, range: IntRange, value: String): String =
        line.substring(0, range.first) + value + line.substring(range.last + 1)

    @Test
    fun `returns() recovers each return addenda's reason code and meaning`() {
        val file = Nacha.sampleFile(Rng(3L), valid = true)
        val lines = file.split("\n").toMutableList()
        val addendaIdx = lines.indexOfFirst { it[0] == '7' }
        // Turn the sample's type-05 addenda into a type-99 return carrying reason code R01.
        lines[addendaIdx] = splice(splice(lines[addendaIdx], 1..2, "99"), 3..5, "R01")
        val returns = Nacha.returns(lines.joinToString("\n"))
        assertEquals(1, returns.size)
        assertEquals("R01", returns[0].code)
        assertEquals("Insufficient Funds", returns[0].meaning)
        assertEquals(addendaIdx + 1, returns[0].line)
        // A well-formed return reason code is NOT a validation problem.
        assertTrue(Nacha.validate(lines.joinToString("\n")).none { it.message.contains("reason code") })
    }

    @Test
    fun `a malformed return reason code is flagged`() {
        val file = Nacha.sampleFile(Rng(3L), valid = true)
        val lines = file.split("\n").toMutableList()
        val addendaIdx = lines.indexOfFirst { it[0] == '7' }
        // Type-99 return addenda whose reason code "RX1" is not R + two digits.
        lines[addendaIdx] = splice(splice(lines[addendaIdx], 1..2, "99"), 3..5, "RX1")
        val problems = Nacha.validate(lines.joinToString("\n"))
        assertTrue(problems.any { it.message.contains("reason code 'RX1' is malformed") }, "got $problems")
    }

    @Test
    fun `NOC change codes map to their published Nacha meanings`() {
        // EXTERNAL anchor: the Notification of Change / COR Change Code block (C01–C14) from the Nacha
        // Operating Rules & Guidelines — code and official description as published by Nacha.
        assertEquals("Incorrect DFI Account Number", Nacha.CHANGE_CODES["C01"])
        assertEquals("Incorrect Routing Number", Nacha.CHANGE_CODES["C02"])
        assertEquals("Incorrect Transaction Code", Nacha.CHANGE_CODES["C05"])
        assertEquals("Addenda Format Error", Nacha.CHANGE_CODES["C13"])
        assertEquals("Incorrect SEC Code for Outbound International Payment", Nacha.CHANGE_CODES["C14"])
        assertEquals(14, Nacha.CHANGE_CODES.size, "the C01–C14 change-code block")
    }

    @Test
    fun `changeNotifications() recovers each NOC addenda's change code and meaning`() {
        val file = Nacha.sampleFile(Rng(3L), valid = true)
        val lines = file.split("\n").toMutableList()
        val addendaIdx = lines.indexOfFirst { it[0] == '7' }
        // Turn the sample's type-05 addenda into a type-98 NOC carrying change code C02.
        lines[addendaIdx] = splice(splice(lines[addendaIdx], 1..2, "98"), 3..5, "C02")
        val nocs = Nacha.changeNotifications(lines.joinToString("\n"))
        assertEquals(1, nocs.size)
        assertEquals("C02", nocs[0].code)
        assertEquals("Incorrect Routing Number", nocs[0].meaning)
        assertEquals(addendaIdx + 1, nocs[0].line)
        assertTrue(Nacha.validate(lines.joinToString("\n")).none { it.message.contains("change code") })
    }

    @Test
    fun `a malformed NOC change code is flagged`() {
        val file = Nacha.sampleFile(Rng(3L), valid = true)
        val lines = file.split("\n").toMutableList()
        val addendaIdx = lines.indexOfFirst { it[0] == '7' }
        // Type-98 NOC addenda whose change code "CX2" is not C + two digits.
        lines[addendaIdx] = splice(splice(lines[addendaIdx], 1..2, "98"), 3..5, "CX2")
        val problems = Nacha.validate(lines.joinToString("\n"))
        assertTrue(problems.any { it.message.contains("change code 'CX2' is malformed") }, "got $problems")
    }

    @Test
    fun `SEC codes map to their published Nacha names and include the check-conversion codes`() {
        // EXTERNAL anchor: Standard Entry Class code names from the Nacha Operating Rules & Guidelines.
        assertEquals("Prearranged Payment and Deposit Entry", Nacha.SEC_CODE_NAMES["PPD"])
        assertEquals("Corporate Credit or Debit Entry", Nacha.SEC_CODE_NAMES["CCD"])
        assertEquals("Accounts Receivable Entry", Nacha.SEC_CODE_NAMES["ARC"])
        assertEquals("Back Office Conversion Entry", Nacha.SEC_CODE_NAMES["BOC"])
        assertEquals("Point-of-Purchase Entry", Nacha.SEC_CODE_NAMES["POP"])
        assertEquals("Re-presented Check Entry", Nacha.SEC_CODE_NAMES["RCK"])
        // SEC_CODES is derived from the name map, so every listed code has a published name.
        assertEquals(Nacha.SEC_CODE_NAMES.keys.toList(), Nacha.SEC_CODES)
        assertTrue(Nacha.SEC_CODES.containsAll(listOf("ARC", "BOC", "POP", "RCK")))
    }

    @Test
    fun `the check-conversion SEC codes each produce a valid file carrying the code`() {
        // The SEC code occupies positions 51-53 (0-indexed 50..52) of the type-5 batch header record.
        for (sec in listOf("ARC", "BOC", "POP", "RCK")) {
            val file = Nacha.sampleFile(Rng(9L), valid = true, secCode = sec)
            val batchHeader = file.split("\n").first { it[0] == '5' }
            assertEquals(sec, batchHeader.substring(50, 53), "batch header carries the $sec SEC code")
            assertTrue(Nacha.validate(file).isEmpty(), "$sec file validates: ${Nacha.validate(file)}")
        }
    }
}
