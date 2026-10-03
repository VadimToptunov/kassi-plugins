package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.inspect.MrzGenerator
import io.github.vadimtoptunov.kassitestdata.inspect.MrzInspector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MrzGeneratorTest {

    @Test
    fun `reproduces the published ICAO Doc 9303 specimen passport`() {
        // External anchor: the specimen TD3 from ICAO Doc 9303 Part 4.
        val mrz = MrzGenerator.td3(
            MrzGenerator.Td3Fields(
                issuingCountry = "UTO",
                nationality = "UTO",
                surname = "ERIKSSON",
                givenNames = "ANNA MARIA",
                documentNumber = "L898902C3",
                dateOfBirth = "740812",
                sex = "F",
                expiryDate = "120415",
                personalNumber = "ZE184226B",
            ),
        )
        val lines = mrz.split("\n")
        assertEquals("P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<", lines[0])
        assertEquals("L898902C36UTO7408122F1204159ZE184226B<<<<<10", lines[1])
    }

    @Test
    fun `generated TD3 passports round-trip through the inspector as all-valid`() {
        val rng = Rng(5L)
        repeat(200) {
            val mrz = MrzGenerator.sampleTd3(rng)
            val lines = mrz.split("\n")
            assertTrue(lines.all { it.length == 44 }, "each line is 44 chars: $mrz")
            val outcome = MrzInspector.inspect(lines)
            assertTrue(outcome is MrzInspector.Outcome.Success, "should parse: $mrz")
            assertTrue((outcome as MrzInspector.Outcome.Success).result.allChecksValid, "all checks valid: $mrz")
        }
    }
}
