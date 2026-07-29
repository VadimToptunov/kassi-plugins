package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * BIC/SWIFT (ISO 9362): 4-letter institution code + 2-letter country code + 2-char location code,
 * with an optional 3-char branch code (8 or 11 total). The country code matches the persona's country,
 * so a BIC generated for a persona is internationally consistent. No checksum exists in the standard;
 * validity is structural.
 */
object BicGenerator {

    val PATTERN = Regex("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")

    fun bic(country: Country, rng: Rng, withBranch: Boolean = rng.boolean()): String {
        val institution = rng.upperLetters(4)
        val location = rng.alnums(2)
        val branch = if (withBranch) rng.alnums(3) else ""
        return institution + country.code + location + branch
    }

    /** A deliberately malformed BIC (wrong length / illegal characters) for error-path tests. */
    fun invalidBic(country: Country, rng: Rng): String =
        rng.upperLetters(3) + country.code + rng.digits(2) // 7 chars — never valid

    fun isStructurallyValid(bic: String): Boolean = PATTERN.matches(bic)
}
