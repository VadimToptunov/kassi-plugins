package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.CardGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardGeneratorTest {

    /** Independent Luhn check for the test (walks left-to-right after reversing). */
    private fun luhnValid(number: String): Boolean {
        val reversed = number.reversed()
        var sum = 0
        for (i in reversed.indices) {
            var d = reversed[i] - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    @Test
    fun `valid PANs pass Luhn with correct length from a test BIN, invalid variant fails Luhn`() {
        val rng = Rng(99L)
        for (network in CardGenerator.Network.entries) {
            repeat(50) {
                val valid = CardGenerator.pan(network, rng, valid = true)
                assertEquals(network.length, valid.length, "${network.displayName} PAN length")
                assertTrue(luhnValid(valid), "${network.displayName} PAN should pass Luhn: $valid")
                assertTrue(network.testBins.any { valid.startsWith(it) }, "PAN must use a test BIN: $valid")

                val invalid = CardGenerator.pan(network, rng, valid = false)
                assertEquals(network.length, invalid.length)
                assertFalse(luhnValid(invalid), "invalid PAN should fail Luhn: $invalid")
            }
        }
    }

    @Test
    fun `CVV length follows the network`() {
        val rng = Rng(7L)
        assertEquals(3, CardGenerator.card(CardGenerator.Network.VISA, rng).cvv.length)
        assertEquals(3, CardGenerator.card(CardGenerator.Network.MASTERCARD, rng).cvv.length)
        assertEquals(4, CardGenerator.card(CardGenerator.Network.AMEX, rng).cvv.length)
    }

    @Test
    fun `expiry is formatted MM slash YY`() {
        val card = CardGenerator.card(CardGenerator.Network.VISA, Rng(3L))
        assertTrue(Regex("\\d{2}/\\d{2}").matches(card.expiry), card.expiry)
    }
}
