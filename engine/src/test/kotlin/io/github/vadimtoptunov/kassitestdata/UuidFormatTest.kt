package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.generators.UuidFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Anchored to the example UUID published in RFC 4122 Appendix C —
 * `f81d4fae-7dec-11d0-a765-00a0c91e6bf6` and its URN form `urn:uuid:…` — an external reference, not a
 * value recomputed here.
 */
class UuidFormatTest {

    private val rfc4122Example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"

    @Test
    fun `URN form matches the RFC 4122 example`() {
        assertEquals(
            "urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
            UuidFormat.format(rfc4122Example, UuidFormat.Style.URN),
        )
    }

    @Test
    fun `uppercase, braces and no-hyphens forms`() {
        assertEquals("F81D4FAE-7DEC-11D0-A765-00A0C91E6BF6", UuidFormat.format(rfc4122Example, UuidFormat.Style.UPPERCASE))
        assertEquals("{f81d4fae-7dec-11d0-a765-00a0c91e6bf6}", UuidFormat.format(rfc4122Example, UuidFormat.Style.BRACES))
        assertEquals("f81d4fae7dec11d0a76500a0c91e6bf6", UuidFormat.format(rfc4122Example, UuidFormat.Style.NO_HYPHENS))
    }

    @Test
    fun `plain form is unchanged`() {
        assertEquals(rfc4122Example, UuidFormat.format(rfc4122Example, UuidFormat.Style.PLAIN))
    }
}
