package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Rng
import java.time.Year

/**
 * Card (PAN) generator. Numbers are Luhn-valid and built from well-known payment-gateway
 * TEST BIN ranges only — never real issuable PANs. CVV length and PAN length follow the network.
 */
object CardGenerator {

    enum class Network(val displayName: String, val length: Int, val cvvLength: Int, val testBins: List<String>) {
        // These BINs are the documented test-card prefixes published by payment gateways
        // (Stripe, Adyen, etc.), reserved for testing and never issued to real cardholders.
        VISA("Visa", 16, 3, listOf("400000", "424242", "411111")),
        MASTERCARD("Mastercard", 16, 3, listOf("555555", "222300", "510510")),
        AMEX("American Express", 15, 4, listOf("378282", "371449")),
    }

    data class Card(val network: Network, val pan: String, val cvv: String, val expiry: String) {
        /** Single-line insertable form, PAN grouped for readability. */
        fun formatted(): String {
            val grouped = if (network == Network.AMEX) {
                "${pan.substring(0, 4)} ${pan.substring(4, 10)} ${pan.substring(10)}"
            } else {
                pan.chunked(4).joinToString(" ")
            }
            return "$grouped · CVV $cvv · Exp $expiry"
        }
    }

    /** The bare PAN. [valid] = Luhn-valid; otherwise the Luhn check digit is corrupted. */
    fun pan(network: Network, rng: Rng, valid: Boolean): String {
        val bin = rng.pick(network.testBins)
        val body = bin + rng.digits(network.length - bin.length - 1)
        val checkDigit = Checksums.luhnCheckDigit(body)
        if (valid) return body + checkDigit
        // Corrupt: pick a different final digit so Luhn fails.
        val wrong = (checkDigit + 1) % 10
        return body + wrong
    }

    fun card(network: Network, rng: Rng, valid: Boolean = true): Card {
        val pan = pan(network, rng, valid)
        val cvv = rng.digits(network.cvvLength)
        val month = rng.intInRange(1, 12).toString().padStart(2, '0')
        val year = (Year.now().value + rng.intInRange(1, 4)) % 100
        val expiry = "$month/${year.toString().padStart(2, '0')}"
        return Card(network, pan, cvv, expiry)
    }
}
