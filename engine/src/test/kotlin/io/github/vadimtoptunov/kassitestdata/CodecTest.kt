package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.codec.Codec
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CodecTest {

    @Test
    fun `Base32 matches the RFC 4648 test vectors`() {
        // External anchor: RFC 4648 section 10.
        assertEquals("", Codec.base32Encode(ByteArray(0)))
        assertEquals("MY======", Codec.base32Encode("f".toByteArray()))
        assertEquals("MZXQ====", Codec.base32Encode("fo".toByteArray()))
        assertEquals("MZXW6===", Codec.base32Encode("foo".toByteArray()))
        assertEquals("MZXW6YTBOI======", Codec.base32Encode("foobar".toByteArray()))
        assertEquals("foobar", String(Codec.base32Decode("MZXW6YTBOI======")!!))
        assertEquals("fo", String(Codec.base32Decode("MZXQ====")!!))
        assertNull(Codec.base32Decode("0189")) // 0/1/8/9 are not in the Base32 alphabet
    }

    @Test
    fun `Base58 and Base58Check match the Bitcoin wiki worked example`() {
        // External anchor: en.bitcoin.it/wiki/Base58Check_encoding — the 25-byte string
        // 00010966776006953D5567439E5E39F86A0D273BEED61967F6 encodes to this address.
        val full = Codec.hexDecode("00010966776006953D5567439E5E39F86A0D273BEED61967F6")!!
        val address = "16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM"
        assertEquals(address, Codec.base58Encode(full))
        assertArrayEquals(full, Codec.base58Decode(address))

        // Base58Check over the 21-byte payload (version + hash160) appends the 4-byte checksum itself.
        val payload = Codec.hexDecode("00010966776006953D5567439E5E39F86A0D273BEE")!!
        assertEquals(address, Codec.base58CheckEncode(payload))
        assertArrayEquals(payload, Codec.base58CheckDecode(address))

        // The Bitcoin genesis address has a valid Base58Check checksum; a tampered one does not.
        assertTrue(Codec.base58CheckDecode("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa") != null)
        assertNull(Codec.base58CheckDecode("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb")) // last char changed
        assertNull(Codec.base58Decode("0OIl")) // 0, O, I, l are not in the Base58 alphabet
    }

    @Test
    fun `hex and round-trips`() {
        assertEquals("deadbeef", Codec.hexEncode(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())))
        assertArrayEquals(byteArrayOf(0, 1, 2, -1), Codec.hexDecode("000102ff"))
        assertNull(Codec.hexDecode("abc"))  // odd length
        assertNull(Codec.hexDecode("zz"))   // non-hex

        val samples = listOf("", "f", "hello world", "\u0000\u0000payload")
        for (s in samples) {
            val b = s.toByteArray()
            assertEquals(s, String(Codec.base58Decode(Codec.base58Encode(b))!!), "base58 round-trip: $s")
            assertEquals(s, String(Codec.base32Decode(Codec.base32Encode(b))!!), "base32 round-trip: $s")
            assertEquals(s, String(Codec.hexDecode(Codec.hexEncode(b))!!), "hex round-trip: $s")
            assertArrayEquals(b, Codec.base58CheckDecode(Codec.base58CheckEncode(b)), "base58check round-trip: $s")
        }
    }
}
