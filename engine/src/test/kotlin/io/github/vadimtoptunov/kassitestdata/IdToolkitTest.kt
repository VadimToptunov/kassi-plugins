package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.IdToolkit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IdToolkitTest {

    @Test
    fun `name-based UUID v5 and v3 reference values (Python uuid docs)`() {
        // uuid5(NAMESPACE_DNS, 'python.org') and uuid3(NAMESPACE_DNS, 'python.org').
        assertEquals("886313e1-3b8a-5372-9b90-0c9aee199e5d", IdToolkit.uuidV5(IdToolkit.Namespace.DNS, "python.org"))
        assertEquals("6fa459ea-ee8a-3ca4-894e-db77e160355e", IdToolkit.uuidV3(IdToolkit.Namespace.DNS, "python.org"))
        // deterministic + correctly versioned
        assertEquals(IdToolkit.uuidV5(IdToolkit.Namespace.URL, "x"), IdToolkit.uuidV5(IdToolkit.Namespace.URL, "x"))
        assertEquals(5, IdToolkit.inspectUuid(IdToolkit.uuidV5(IdToolkit.Namespace.DNS, "a"))!!.version)
        assertEquals(3, IdToolkit.inspectUuid(IdToolkit.uuidV3(IdToolkit.Namespace.DNS, "a"))!!.version)
    }

    @Test
    fun `UUID reference values (RFC 9562)`() {
        val v7 = IdToolkit.inspectUuid("017F22E2-79B0-7CC3-98C4-DC0C0C07398F")!!
        assertEquals(7, v7.version)
        assertEquals(1645557742000L, v7.timestampMillis) // RFC 9562 v7 example, 2022-02-22 14:22:22 UTC

        val v4 = IdToolkit.inspectUuid("550e8400-e29b-41d4-a716-446655440000")!!
        assertEquals(4, v4.version)
        assertEquals("RFC 4122 (10x)", v4.variant)
        assertNull(v4.timestampMillis)

        assertNull(IdToolkit.inspectUuid("not-a-uuid"))
    }

    @Test
    fun `ULID timestamp bounds (per spec)`() {
        // The ULID spec documents the max timestamp 7ZZZZZZZZZ = 2^48 - 1, and min = 0.
        assertEquals(281474976710655L, IdToolkit.ulidTimestampMillis("7ZZZZZZZZZ0000000000000000"))
        assertEquals(0L, IdToolkit.ulidTimestampMillis("00000000000000000000000000"))
        assertNull(IdToolkit.ulidTimestampMillis("too-short"))
    }

    @Test
    fun `generators round-trip - embedded structure decodes back`() {
        val rng = Rng(2024L)
        val ts = 1_700_000_000_000L
        repeat(100) {
            val u4 = IdToolkit.uuidV4(rng)
            assertEquals(4, IdToolkit.inspectUuid(u4)!!.version)

            val u7 = IdToolkit.uuidV7(rng, ts)
            val info = IdToolkit.inspectUuid(u7)!!
            assertEquals(7, info.version)
            assertEquals(ts, info.timestampMillis)

            val ulid = IdToolkit.ulid(rng, ts)
            assertTrue(IdToolkit.isValidUlid(ulid))
            assertEquals(ts, IdToolkit.ulidTimestampMillis(ulid))

            val nano = IdToolkit.nanoId(rng)
            assertEquals(21, nano.length)
            assertTrue(IdToolkit.isValidNanoId(nano))
        }
        assertFalse(IdToolkit.isValidNanoId("has spaces"))
    }
}
