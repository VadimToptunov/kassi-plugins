package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.CryptoChecksums
import io.github.vadimtoptunov.kassitestdata.inspect.DataInspector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Parity checks: the engine's validators for VIN / ISBN / JMBG / crypto must match the generators
 * the Kassi Test Data monolith ships, anchored to the SAME external reference values.
 */
class ParityChecksTest {

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    @Test
    fun `VIN reference values`() {
        assertTrue(Checksums.isValidVin("1M8GDM9AXKP042788")) // canonical NHTSA example, check char 'X'
        assertTrue(Checksums.isValidVin("11111111111111111")) // all ones → check digit 1
        assertFalse(Checksums.isValidVin("1M8GDM9A0KP042788")) // wrong check char
        assertFalse(Checksums.isValidVin("1M8GDM9AXKP04278I")) // illegal 'I'
    }

    @Test
    fun `ISBN-10 and ISBN-13 reference values`() {
        assertTrue(Checksums.isValidIsbn10("0306406152"))
        assertTrue(Checksums.isValidIsbn10("097522980X")) // 'X' check char
        assertFalse(Checksums.isValidIsbn10("0306406153"))
        assertTrue(Checksums.isValidEan13("9780306406157")) // ISBN-13 978-0-306-40615-7
    }

    @Test
    fun `JMBG reference value`() {
        assertTrue(Checksums.isValidJmbg("0101006500006")) // documented worked example, check digit 6
        assertFalse(Checksums.isValidJmbg("0101006500007"))
    }

    @Test
    fun `Keccak-256 known-answer vectors`() {
        assertEquals(
            "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470",
            hex(CryptoChecksums.keccak256(ByteArray(0))),
        )
        assertEquals(
            "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45",
            hex(CryptoChecksums.keccak256("abc".toByteArray(Charsets.US_ASCII))),
        )
    }

    @Test
    fun `Bitcoin Base58Check and Ethereum EIP-55 references`() {
        assertTrue(CryptoChecksums.isValidBase58Check("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")) // genesis
        assertFalse(CryptoChecksums.isValidBase58Check("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNb"))
        for (a in listOf(
            "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
            "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
            "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
        )) assertTrue(CryptoChecksums.isValidEip55(a), "expected valid EIP-55: $a")
        assertFalse(CryptoChecksums.isValidEip55("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed"))
    }

    @Test
    fun `DataInspector surfaces the new checks as passing`() {
        fun passed(value: String, name: String) =
            DataInspector.inspect(value).any { it.name.startsWith(name) && it.passed }

        assertTrue(passed("1M8GDM9AXKP042788", "VIN"))
        assertTrue(passed("0-306-40615-2", "ISBN-10"))
        assertTrue(passed("0101006500006", "JMBG"))
        assertTrue(passed("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", "Bitcoin"))
        assertTrue(passed("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed", "Ethereum"))
    }
}
