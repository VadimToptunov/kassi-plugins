package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.generators.BicGenerator
import io.github.vadimtoptunov.kassitestdata.generators.NationalIdGenerator

/** One check applied to an input value. [passed] is the algorithm's verdict. */
data class CheckResult(val name: String, val passed: Boolean)

/**
 * Given an arbitrary pasted value, run every checksum/format check that plausibly applies (by shape)
 * and report which pass. Pure logic, reused across plugin surfaces (the Checksum Playground tool window).
 * Spaces and dashes are ignored; letters are upper-cased.
 */
object DataInspector {

    fun inspect(raw: String): List<CheckResult> {
        val compact = raw.trim().replace(" ", "").replace("-", "").uppercase()
        if (compact.isEmpty()) return emptyList()

        val results = LinkedHashMap<String, Boolean>()
        val digitsOnly = compact.all { it in '0'..'9' }
        val hasCountryPrefix = compact.length >= 3 && compact[0].isLetter() && compact[1].isLetter()
        val countryCode = if (hasCountryPrefix) compact.take(2) else ""
        val afterPrefix = if (hasCountryPrefix) compact.drop(2) else ""

        // IBAN — anything shaped like a country code + BBAN.
        if (hasCountryPrefix && compact.length in 5..34 && afterPrefix.first().isDigit()) {
            results["IBAN — ISO 7064 mod-97"] = Checksums.isValidIbanMod97(compact)
        }

        // Luhn — any plausible card-length digit string.
        if (digitsOnly && compact.length in 12..19) {
            results["Luhn — card number (ISO/IEC 7812)"] = Checksums.isLuhnValid(compact)
        }

        // 9-digit schemes.
        if (digitsOnly && compact.length == 9) {
            results["NL BSN — 11-proef"] = Checksums.isValidElevenProof(compact)
            results["AU TFN — weighted mod-11"] = Checksums.isValidTfn(compact)
            results["DE VAT — ISO 7064 MOD 11,10"] = Checksums.isValidGermanVat(compact)
            results["UK VAT — modulo-97"] = Checksums.isValidUkVat(compact)
        }

        // AU ABN.
        if (digitsOnly && compact.length == 11) {
            results["AU ABN — modulo-89"] = Checksums.isValidAbn(compact)
        }

        // Cyprus VAT body: 8 digits + check letter.
        if (compact.length == 9 && compact.take(8).all { it in '0'..'9' } && compact[8].isLetter()) {
            results["CY VAT — check letter"] = Checksums.isValidCyprusVat(compact)
        }

        // DE ID card: 9-char serial + check digit.
        if (compact.length == 10 && compact.all { it in '0'..'9' || it in 'A'..'Z' }) {
            results["DE ID card — ICAO 7-3-1"] = Checksums.isValidGermanIdCard(compact)
        }

        // BIC/SWIFT structure.
        if (compact.length == 8 || compact.length == 11) {
            results["BIC/SWIFT — ISO 9362 structure"] = BicGenerator.isStructurallyValid(compact)
        }

        // GB NINo format.
        if (compact.length == 9 && compact[0].isLetter() && compact[1].isLetter()) {
            results["GB NINo — format"] = NationalIdGenerator.isValidNino(compact)
        }

        // VAT numbers carrying a country prefix (e.g. DE123456789, CY10259033P).
        when (countryCode) {
            "DE" -> if (afterPrefix.length == 9 && afterPrefix.all { it in '0'..'9' })
                results["DE VAT — ISO 7064 MOD 11,10"] = Checksums.isValidGermanVat(afterPrefix)
            "GB" -> if (afterPrefix.length == 9 && afterPrefix.all { it in '0'..'9' })
                results["UK VAT — modulo-97"] = Checksums.isValidUkVat(afterPrefix)
            "CY" -> if (afterPrefix.length == 9)
                results["CY VAT — check letter"] = Checksums.isValidCyprusVat(afterPrefix)
            "NL" -> afterPrefix.takeWhile { it in '0'..'9' }.let { body ->
                if (body.length == 9) results["NL VAT — 11-proef body"] = Checksums.isValidElevenProof(body)
            }
        }

        return results.map { (name, passed) -> CheckResult(name, passed) }
    }
}
