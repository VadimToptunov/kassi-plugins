package io.github.vadimtoptunov.kassitestdata.inspect

/**
 * Parses the text payload of an AAMVA-compliant PDF417 barcode (US/Canada driver's license or ID
 * card), as decoded by any barcode scanner/reader. This does not decode barcode *images* — it
 * parses the already-decoded payload string into its header and per-subfile data elements, and
 * reports exactly which structural expectation (header signature, IIN, subfile bounds) failed.
 * AAMVA has no per-field checksum (unlike MRZ), so the "moat" here is spec-correct parsing.
 */
object AamvaInspector {

    private const val COMPLIANCE_INDICATOR = '@'
    private const val DATA_ELEMENT_SEPARATOR = '\n' // LF (0x0A) — separates fields inside a subfile
    private const val RECORD_SEPARATOR = '\u001e' // RS (0x1E)
    private const val SEGMENT_TERMINATOR = '\r' // CR (0x0D)
    private const val HEADER_LENGTH = 21 // '@' + LF + RS + CR + "ANSI " + 6-digit IIN + 2 + 2 + 2

    data class Field(val elementId: String, val value: String)
    data class Subfile(val type: String, val fields: List<Field>)
    data class AamvaResult(
        val iin: String,
        val aamvaVersion: String,
        val jurisdictionVersion: String,
        val subfiles: List<Subfile>,
    )

    sealed class Outcome {
        data class Success(val result: AamvaResult) : Outcome()
        data class Invalid(val reason: String) : Outcome()
    }

    fun inspect(raw: String): Outcome {
        if (raw.length < HEADER_LENGTH) {
            return Outcome.Invalid("Too short to be an AAMVA payload (need at least a $HEADER_LENGTH-byte header)")
        }
        if (raw[0] != COMPLIANCE_INDICATOR) return Outcome.Invalid("Missing compliance indicator '@' at position 0")
        if (raw[1] != DATA_ELEMENT_SEPARATOR) return Outcome.Invalid("Missing data element separator (LF) at position 1")
        if (raw[2] != RECORD_SEPARATOR) return Outcome.Invalid("Missing record separator (RS) at position 2")
        if (raw[3] != SEGMENT_TERMINATOR) return Outcome.Invalid("Missing segment terminator (CR) at position 3")

        val fileType = raw.substring(4, 9)
        if (fileType != "ANSI ") return Outcome.Invalid("Expected file type 'ANSI ', got '$fileType'")

        val iin = raw.substring(9, 15)
        if (!iin.all { it in '0'..'9' }) return Outcome.Invalid("IIN must be 6 digits, got '$iin'")

        val aamvaVersion = raw.substring(15, 17)
        val jurisdictionVersion = raw.substring(17, 19)
        val entryCountStr = raw.substring(19, 21)
        val entryCount = entryCountStr.toIntOrNull()
            ?: return Outcome.Invalid("Number of entries must be 2 digits, got '$entryCountStr'")

        val designators = mutableListOf<Triple<String, Int, Int>>()
        var offset = HEADER_LENGTH
        for (i in 0 until entryCount) {
            if (offset + 10 > raw.length) return Outcome.Invalid("Truncated subfile designator #${i + 1}")
            val type = raw.substring(offset, offset + 2)
            val subOffset = raw.substring(offset + 2, offset + 6).toIntOrNull()
                ?: return Outcome.Invalid("Bad offset in subfile designator '$type'")
            val subLength = raw.substring(offset + 6, offset + 10).toIntOrNull()
                ?: return Outcome.Invalid("Bad length in subfile designator '$type'")
            designators += Triple(type, subOffset, subLength)
            offset += 10
        }

        val subfiles = mutableListOf<Subfile>()
        for ((type, subOffset, subLength) in designators) {
            if (subOffset < 0 || subOffset + subLength > raw.length) {
                return Outcome.Invalid("Subfile '$type' (offset $subOffset, length $subLength) extends past the payload")
            }
            val body = raw.substring(subOffset, subOffset + subLength)
            val elements = body.removePrefix(type).trimEnd(SEGMENT_TERMINATOR).split(DATA_ELEMENT_SEPARATOR)
            val fields = elements.filter { it.length >= 3 }.map { Field(it.substring(0, 3), it.substring(3)) }
            subfiles += Subfile(type, fields)
        }

        return Outcome.Success(AamvaResult(iin, aamvaVersion, jurisdictionVersion, subfiles))
    }
}
