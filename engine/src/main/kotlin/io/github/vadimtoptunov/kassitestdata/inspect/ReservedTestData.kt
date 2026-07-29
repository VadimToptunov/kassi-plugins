package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import io.github.vadimtoptunov.kassitestdata.generators.CardGenerator
import io.github.vadimtoptunov.kassitestdata.generators.UsSsnGenerator

/**
 * Deterministic "obviously fake" fixture values that [LeakGuard] offers as its quick-fix
 * replacement. Cards already have an industry-standard reserved range (the payment-gateway test
 * BINs [CardGenerator] draws from); IBANs have no such public equivalent, so this generates and
 * remembers exactly one canonical IBAN per country instead — through the same validated
 * [BankAccountGenerator] the rest of the engine uses, from a fixed seed so the value is stable
 * across runs. That's what lets [LeakGuard] recognize its own quick-fix output on a second pass
 * instead of re-flagging it.
 */
object ReservedTestData {

    private val rng = Rng(0x4B41535349L) // fixed seed ("KASSI"): reproducible, never random

    private val testIbans: Map<Country, String> =
        IbanRegistry.supportedCountries.associateWith { BankAccountGenerator.iban(it, rng, valid = true) }

    private val testIbanSet: Set<String> = testIbans.values.toSet()

    fun isReservedIban(compactIban: String): Boolean = compactIban in testIbanSet

    fun testIbanFor(country: Country): String? = testIbans[country]

    private val testPan15: String = CardGenerator.pan(CardGenerator.Network.AMEX, rng, valid = true)
    private val testPan16: String = CardGenerator.pan(CardGenerator.Network.VISA, rng, valid = true)

    /** The reserved test PAN matching [panLength] (Amex is 15 digits; every other network here is 16). */
    fun testPanFor(panLength: Int): String? = when (panLength) {
        15 -> testPan15
        16 -> testPan16
        else -> null
    }

    val testSsn: String = UsSsnGenerator.generate(rng)
}
