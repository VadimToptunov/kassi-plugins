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

    /**
     * AAMVA DL/ID data-element identifiers → their published names, from the AAMVA DL/ID Card Design
     * Standard (Annex D data-element dictionary). Covers the mandatory DL elements plus the common
     * optional ones. Used purely to LABEL a parsed element; an id absent here is shown as-is. The exact
     * names are the external published reference (see the unit test).
     */
    val ELEMENT_NAMES: Map<String, String> = linkedMapOf(
        // Mandatory DL elements
        "DCA" to "Jurisdiction-specific vehicle class",
        "DCB" to "Jurisdiction-specific restriction codes",
        "DCD" to "Jurisdiction-specific endorsement codes",
        "DBA" to "Document Expiration Date",
        "DCS" to "Customer Family Name",
        "DAC" to "Customer First Name",
        "DAD" to "Customer Middle Name(s)",
        "DBD" to "Document Issue Date",
        "DBB" to "Date of Birth",
        "DBC" to "Physical Description – Sex",
        "DAY" to "Physical Description – Eye Color",
        "DAU" to "Physical Description – Height",
        "DAG" to "Address – Street 1",
        "DAI" to "Address – City",
        "DAJ" to "Address – Jurisdiction Code",
        "DAK" to "Address – Postal Code",
        "DAQ" to "Customer ID Number",
        "DCF" to "Document Discriminator",
        "DCG" to "Country Identification",
        "DDE" to "Family Name Truncation",
        "DDF" to "First Name Truncation",
        "DDG" to "Middle Name Truncation",
        // Common optional elements
        "DAH" to "Address – Street 2",
        "DAZ" to "Hair Color",
        "DCI" to "Place of Birth",
        "DCJ" to "Audit Information",
        "DCK" to "Inventory Control Number",
        "DBN" to "Alias / AKA Family Name",
        "DBG" to "Alias / AKA Given Name",
        "DBS" to "Alias / AKA Suffix Name",
        "DCU" to "Name Suffix",
        "DCE" to "Physical Description – Weight Range",
        "DCL" to "Race / Ethnicity",
        "DDA" to "Compliance Type",
        "DDB" to "Card Revision Date",
        "DDC" to "HazMat Endorsement Expiration Date",
        "DDD" to "Limited Duration Document Indicator",
        "DAW" to "Weight (pounds)",
        "DAX" to "Weight (kilograms)",
        "DDK" to "Organ Donor Indicator",
        "DDL" to "Veteran Indicator",
    )

    /** The published AAMVA name for [elementId], or null when the id is not in the standard dictionary. */
    fun elementName(elementId: String): String? = ELEMENT_NAMES[elementId]

    /**
     * The mandatory data elements of a DL (driver's license) subfile, per the AAMVA DL/ID Card Design
     * Standard. An ID-card ("ID") subfile has a different mandatory set, so the presence check below is
     * intentionally scoped to the "DL" subfile only.
     */
    val MANDATORY_DL_ELEMENTS: Set<String> = linkedSetOf(
        "DCA", "DCB", "DCD", "DBA", "DCS", "DAC", "DAD", "DBD", "DBB", "DBC",
        "DAY", "DAU", "DAG", "DAI", "DAJ", "DAK", "DAQ", "DCF", "DCG", "DDE", "DDF", "DDG",
    )

    /**
     * The mandatory DL elements (in standard order) that the DL subfile of [result] is missing. Empty
     * when the DL subfile carries them all, or when there is no DL subfile (nothing to check).
     */
    fun missingMandatoryElements(result: AamvaResult): List<String> {
        val dl = result.subfiles.firstOrNull { it.type == "DL" } ?: return emptyList()
        val present = dl.fields.mapTo(HashSet()) { it.elementId }
        return MANDATORY_DL_ELEMENTS.filter { it !in present }
    }

    // AAMVA DL/ID Card Design Standard coded-value tables (Annex D): sex (ISO/IEC 5218), eye and hair
    // colour. The exact code→meaning strings are the external published reference (see the unit test).
    private val SEX_CODES = mapOf("1" to "Male", "2" to "Female", "9" to "Not specified")
    private val EYE_COLOUR_CODES = mapOf(
        "BLK" to "Black", "BLU" to "Blue", "BRO" to "Brown", "GRY" to "Gray", "GRN" to "Green",
        "HAZ" to "Hazel", "MAR" to "Maroon", "PNK" to "Pink", "DIC" to "Dichromatic", "UNK" to "Unknown",
    )
    private val HAIR_COLOUR_CODES = mapOf(
        "BAL" to "Bald", "BLK" to "Black", "BLN" to "Blond", "BRO" to "Brown", "GRY" to "Gray",
        "RED" to "Red/Auburn", "SDY" to "Sandy", "WHI" to "White", "UNK" to "Unknown",
    )

    /**
     * Human-readable decoding of a coded element [value] for the coded fields DBC (sex), DAY (eye
     * colour) and DAZ (hair colour), per the AAMVA code tables. Null for an uncoded element or a value
     * outside the table — callers then show the raw value unchanged.
     */
    fun decodeValue(elementId: String, value: String): String? = when (elementId) {
        "DBC" -> SEX_CODES[value.trim()]
        "DAY" -> EYE_COLOUR_CODES[value.trim().uppercase()]
        "DAZ" -> HAIR_COLOUR_CODES[value.trim().uppercase()]
        else -> null
    }
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
