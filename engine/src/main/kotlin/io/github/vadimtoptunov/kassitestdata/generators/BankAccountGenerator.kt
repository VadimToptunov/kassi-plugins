package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry

/**
 * Bank account identifier — a per-country abstraction (IBAN | sort code + account | BSB + account).
 * IBAN countries get a mod-97-valid IBAN; GB and AU get their domestic formats. Including AU (no IBAN)
 * is what forces this abstraction to stay clean.
 */
object BankAccountGenerator {

    /** A raw IBAN (no spaces) for [country]. [valid] = correct check digits; otherwise deliberately corrupted. */
    fun iban(country: Country, rng: Rng, valid: Boolean): String {
        val spec = IbanRegistry.specFor(country)
            ?: throw IllegalArgumentException("${country.code} is not an IBAN country")
        val bban = buildString {
            for ((count, type) in spec.bbanSegments) {
                append(
                    when (type) {
                        'n' -> rng.digits(count)
                        'a' -> rng.upperLetters(count)
                        'c' -> rng.alnums(count)
                        else -> error("unreachable")
                    }
                )
            }
        }
        val check = Checksums.ibanCheckDigits(country.code, bban)
        var iban = country.code + check + bban
        if (!valid) {
            var c = check.toInt()
            do {
                c = (c + 1) % 100
                iban = country.code + c.toString().padStart(2, '0') + bban
            } while (Checksums.isValidIbanMod97(iban))
        }
        return iban
    }

    /** IBAN grouped in blocks of four for readable insertion. */
    fun ibanFormatted(country: Country, rng: Rng, valid: Boolean): String =
        iban(country, rng, valid).chunked(4).joinToString(" ")

    /** GB domestic identifier (sort code + account), derived from a valid GB IBAN so it stays coherent. */
    fun gbSortCodeAndAccount(rng: Rng): String {
        val iban = iban(Country.GB, rng, valid = true) // GB BBAN = 4a bank, 6n sort code, 8n account
        val sort = iban.substring(8, 14)
        val account = iban.substring(14, 22)
        val sortFormatted = "${sort.substring(0, 2)}-${sort.substring(2, 4)}-${sort.substring(4, 6)}"
        return "Sort code $sortFormatted · Account $account"
    }

    /** Australian domestic identifier: BSB (bbb-bbb) + account number. */
    fun auBsbAndAccount(rng: Rng): String {
        val bsb = rng.digits(6)
        val bsbFormatted = "${bsb.substring(0, 3)}-${bsb.substring(3, 6)}"
        val account = rng.digits(rng.intInRange(6, 9))
        return "BSB $bsbFormatted · Account $account"
    }
}
