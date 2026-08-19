package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.algo.CryptoChecksums
import io.github.vadimtoptunov.kassitestdata.algo.RuIdChecksums
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
        // Crypto addresses are case-sensitive (Base58 / EIP-55), so they use the raw input, not [compact].
        val rawTrimmed = raw.trim()

        val results = LinkedHashMap<String, Boolean>()
        val digitsOnly = compact.all { it in '0'..'9' }
        val hasCountryPrefix = compact.length >= 3 && compact[0].isLetter() && compact[1].isLetter()
        val countryCode = if (hasCountryPrefix) compact.take(2) else ""
        val afterPrefix = if (hasCountryPrefix) compact.drop(2) else ""

        // IBAN — anything shaped like a country code + BBAN.
        if (hasCountryPrefix && compact.length in 5..34 && afterPrefix.first().isDigit()) {
            results["IBAN — ISO 7064 mod-97"] = Checksums.isValidIbanMod97(compact)
        }

        // ES IBAN — extra national BBAN control digits beyond mod-97.
        if (countryCode == "ES" && compact.length == 24 && afterPrefix.all { it in '0'..'9' }) {
            results["ES IBAN — national BBAN control"] = Checksums.isValidSpanishIbanBban(compact)
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
            results["NO VAT (MVA/orgnr) — mod-11"] = Checksums.isValidNorwegianVat(compact)
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

        // ISIN — 2-letter country + 9-char NSIN + a check digit.
        if (compact.length == 12 && compact[0].isLetter() && compact[1].isLetter() && compact.last().isDigit()) {
            results["ISIN — ISO 6166 (Luhn)"] = Checksums.isValidIsin(compact)
        }

        // LEI — 20 alphanumerics ending in two check digits.
        if (compact.length == 20 && compact.all { it in '0'..'9' || it in 'A'..'Z' }) {
            results["LEI — ISO 17442 (MOD 97-10)"] = Checksums.isValidLei(compact)
        }

        // EAN-13 / GTIN and IMEI — pure-digit product / device identifiers.
        if (digitsOnly && compact.length == 13) {
            results["EAN-13 — product barcode"] = Checksums.isValidEan13(compact)
            if (compact.startsWith("978") || compact.startsWith("979")) {
                results["ISBN-13 — book (EAN-13 check)"] = Checksums.isValidEan13(compact)
            }
            results["JMBG — ex-Yugoslav ID (mod-11)"] = Checksums.isValidJmbg(compact)
        }
        if (digitsOnly && compact.length == 15) {
            results["IMEI — device (Luhn)"] = Checksums.isLuhnValid(compact)
        }

        // ICCID — SIM identifier, 19–20 digits, Luhn-checked.
        if (digitsOnly && compact.length in 19..20) {
            results["ICCID — SIM card (Luhn)"] = Checksums.isLuhnValid(compact)
        }

        // VIN — 17 alphanumerics (no I/O/Q), weighted mod-11 check character.
        if (compact.length == 17 && compact.all { it in '0'..'9' || it in 'A'..'Z' }) {
            results["VIN — vehicle (ISO 3779)"] = Checksums.isValidVin(compact)
        }

        // ISBN-10 — 9 digits + a mod-11 check character (digit or X).
        if (compact.length == 10 && compact.take(9).all { it in '0'..'9' } &&
            (compact[9] in '0'..'9' || compact[9] == 'X')
        ) {
            results["ISBN-10 — book (mod-11)"] = Checksums.isValidIsbn10(compact)
        }

        // FI ALV VAT — bare 8-digit base + check.
        if (digitsOnly && compact.length == 8) {
            results["FI VAT (ALV) — weighted mod-11"] = Checksums.isValidFinnishVat(compact)
            results["DK VAT (CVR) — mod-11"] = Checksums.isValidDanishVat(compact)
        }

        // ISO 6346 — 4 letters + 6 digits + check digit.
        if (compact.length == 11 && compact.take(4).all { it in 'A'..'Z' } && compact.drop(4).all { it in '0'..'9' }) {
            results["ISO 6346 — shipping container"] = Checksums.isValidIso6346(compact)
        }

        // Crypto addresses — case-sensitive, so checked against the raw input.
        if (rawTrimmed.length in 26..35 && (rawTrimmed.startsWith("1") || rawTrimmed.startsWith("3"))) {
            results["Bitcoin address — Base58Check"] = CryptoChecksums.isValidBase58Check(rawTrimmed)
        }
        if (Regex("^0x[0-9a-fA-F]{40}$").matches(rawTrimmed)) {
            results["Ethereum address — EIP-55"] = CryptoChecksums.isValidEip55(rawTrimmed)
        }

        // Russian identifiers (digit strings of characteristic lengths).
        if (digitsOnly) when (compact.length) {
            10 -> results["RU ИНН (юр. лицо) — weighted mod-11"] = RuIdChecksums.isValidInnLegal(compact)
            11 -> results["RU СНИЛС — weighted mod-101"] = RuIdChecksums.isValidSnils(compact)
            12 -> results["RU ИНН (физ. лицо) — weighted mod-11"] = RuIdChecksums.isValidInnIndividual(compact)
            13 -> results["RU ОГРН — mod-11"] = RuIdChecksums.isValidOgrn(compact)
            15 -> results["RU ОГРНИП — mod-13"] = RuIdChecksums.isValidOgrnip(compact)
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
