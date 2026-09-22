package io.github.vadimtoptunov.kassitestdata.generators

/**
 * Render a canonical 8-4-4-4-12 UUID string in the common presentation formats: the RFC 4122 URN
 * (`urn:uuid:…`), the registry/GUID braces form (`{…}`), uppercase, and the hyphen-less "N" form.
 * Pure and offline — the values themselves come from [IdToolkit]; this only reshapes the text.
 */
object UuidFormat {

    enum class Style(val label: String) {
        PLAIN("Plain (lowercase)"),
        UPPERCASE("UPPERCASE"),
        BRACES("{Braces}"),
        URN("urn:uuid:"),
        NO_HYPHENS("No hyphens (32 hex)"),
    }

    /** Reshapes a canonical UUID string [uuid] into [style]. */
    fun format(uuid: String, style: Style): String = when (style) {
        Style.PLAIN -> uuid
        Style.UPPERCASE -> uuid.uppercase()
        Style.BRACES -> "{$uuid}"
        Style.URN -> "urn:uuid:$uuid"
        Style.NO_HYPHENS -> uuid.replace("-", "")
    }
}
