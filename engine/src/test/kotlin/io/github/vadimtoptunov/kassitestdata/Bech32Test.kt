package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.codec.Bech32
import io.github.vadimtoptunov.kassitestdata.codec.Codec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Anchored to the BIP-173 (Bech32) and BIP-350 (Bech32m) published test vectors, so the implementation
 * is verified against the specifications and not just against itself.
 */
class Bech32Test {

    @Test
    fun `BIP-173 P2WPKH address decodes to its published witness program`() {
        // BIP-173 worked example: this mainnet P2WPKH address is witness v0 over program
        // 751e76e8199196d454941c45d1b3a323f1433bd6, and its checksum is the original Bech32 constant.
        val seg = Bech32.decodeSegwit("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4")!!
        assertEquals("bc", seg.hrp)
        assertEquals(0, seg.witnessVersion)
        assertEquals("751e76e8199196d454941c45d1b3a323f1433bd6", Codec.hexEncode(seg.program))
        assertEquals(Bech32.Variant.BECH32, Bech32.decode("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4")!!.variant)
    }

    @Test
    fun `encoding the published program round-trips to the lowercase address`() {
        val program = Codec.hexDecode("751e76e8199196d454941c45d1b3a323f1433bd6")!!
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", Bech32.encodeSegwit("bc", 0, program))
    }

    @Test
    fun `BIP-350 Bech32m string vector is recognized as Bech32m`() {
        // BIP-350 valid test vector (the shortest), whose checksum uses the Bech32m constant.
        assertEquals(Bech32.Variant.BECH32M, Bech32.decode("A1LQFN3A")!!.variant)
        assertEquals(Bech32.Variant.BECH32M, Bech32.decode("a1lqfn3a")!!.variant)
    }

    @Test
    fun `a Taproot (v1) address is Bech32m with a 32-byte program`() {
        // The first mainnet Taproot output address (witness v1, 32-byte x-only key) — a published value.
        val addr = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0"
        val seg = Bech32.decodeSegwit(addr)!!
        assertEquals(1, seg.witnessVersion)
        assertEquals(32, seg.program.size)
        assertEquals(Bech32.Variant.BECH32M, Bech32.decode(addr)!!.variant)
    }

    @Test
    fun `invalid Bech32 inputs are rejected`() {
        // Mixed case is invalid per BIP-173.
        assertNull(Bech32.decode("bc1Qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"))
        // A changed character breaks the checksum.
        assertNull(Bech32.decodeSegwit("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5"))
        // A v0 witness program must be 20 or 32 bytes.
        assertNull(Bech32.encodeSegwit("bc", 0, ByteArray(21)))
        // Witness version out of range.
        assertNull(Bech32.encodeSegwit("bc", 17, ByteArray(20)))
        assertNotNull(Bech32.encodeSegwit("bc", 0, ByteArray(20))) // control: a legal v0 program encodes
    }
}
