package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.inspect.AamvaInspector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AAMVA has no reference checksum to anchor to (unlike MRZ), so these tests build a
 * spec-shaped payload from its parts and assert the parser recovers exactly those parts —
 * a round-trip check on the header/subfile-designator/data-element structure itself — plus
 * the error path for each structural rule the parser enforces.
 */
class AamvaInspectorTest {

    private val recordSeparator = "\u001e" // RS (0x1E)

    private fun header(iin: String, aamvaVersion: String, jurisdictionVersion: String, entryCount: String): String =
        "@\n$recordSeparator\rANSI $iin$aamvaVersion$jurisdictionVersion$entryCount"

    private fun buildPayload(iin: String, aamvaVersion: String, jurisdictionVersion: String, subfileBody: String): String {
        val subfileType = "DL"
        val head = header(iin, aamvaVersion, jurisdictionVersion, "01")
        val subOffset = head.length + 10 // one designator, 10 bytes
        val designator = subfileType + subOffset.toString().padStart(4, '0') + subfileBody.length.toString().padStart(4, '0')
        return head + designator + subfileBody
    }

    @Test
    fun `well-formed single-subfile payload round-trips every field`() {
        val subfileBody = "DLDAQF12345678\nDCSSAMPLE\nDACJOHN\r"
        val payload = buildPayload("636000", "08", "00", subfileBody)

        val outcome = AamvaInspector.inspect(payload) as AamvaInspector.Outcome.Success
        val r = outcome.result

        assertEquals("636000", r.iin)
        assertEquals("08", r.aamvaVersion)
        assertEquals("00", r.jurisdictionVersion)
        assertEquals(1, r.subfiles.size)

        val dl = r.subfiles.single()
        assertEquals("DL", dl.type)
        val byId = dl.fields.associate { it.elementId to it.value }
        assertEquals("F12345678", byId["DAQ"])
        assertEquals("SAMPLE", byId["DCS"])
        assertEquals("JOHN", byId["DAC"])
    }

    @Test
    fun `missing compliance indicator is reported`() {
        val payload = buildPayload("636000", "08", "00", "DLDAQF1\r")
        val bad = "X" + payload.drop(1)
        val outcome = AamvaInspector.inspect(bad) as AamvaInspector.Outcome.Invalid
        assertTrue(outcome.reason.contains("compliance indicator"))
    }

    @Test
    fun `non-numeric IIN is reported`() {
        val payload = buildPayload("ABCDEF", "08", "00", "DLDAQF1\r")
        val outcome = AamvaInspector.inspect(payload) as AamvaInspector.Outcome.Invalid
        assertTrue(outcome.reason.contains("IIN"))
    }

    @Test
    fun `truncated payload is reported instead of throwing`() {
        val outcome = AamvaInspector.inspect("@\n$recordSeparator\rANSI 636000") as AamvaInspector.Outcome.Invalid
        assertTrue(outcome.reason.contains("short"))
    }

    @Test
    fun `subfile designator pointing past the payload is reported`() {
        val head = header("636000", "08", "00", "01")
        val badDesignator = "DL" + "9999".padStart(4, '0') + "0010".padStart(4, '0')
        val outcome = AamvaInspector.inspect(head + badDesignator) as AamvaInspector.Outcome.Invalid
        assertTrue(outcome.reason.contains("extends past"))
    }
}
