package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * Builds a spec-valid TD3 (passport) Machine Readable Zone — the writer companion to [MrzInspector].
 * Two lines of 44 characters with ICAO 7-3-1 check digits on the document number, date of birth,
 * expiry date, optional personal number, and the composite check digit. Deterministic; the check
 * digits are computed by the same [Checksums.icao731CheckDigit] the reader verifies with, and the
 * result is anchored in the unit test to the published ICAO Doc 9303 specimen passport.
 */
object MrzGenerator {

    /** Fields for a TD3 passport MRZ. Dates are YYMMDD. Names and the document number are ASCII A-Z/0-9. */
    data class Td3Fields(
        val issuingCountry: String,
        val nationality: String,
        val surname: String,
        val givenNames: String,
        val documentNumber: String,
        val dateOfBirth: String,
        val sex: String,            // "M", "F" or "<"
        val expiryDate: String,
        val personalNumber: String = "",
    )

    fun td3(f: Td3Fields): String {
        val nameField = nameField(f.surname, f.givenNames, 39)
        val line1 = "P<" + fitAlpha(f.issuingCountry, 3) + nameField

        val docField = mrzField(f.documentNumber, 9)
        val docCheck = check(docField)
        val dobCheck = check(f.dateOfBirth)
        val expCheck = check(f.expiryDate)
        val pnField = mrzField(f.personalNumber, 14)
        val pnCheck = check(pnField)
        // Composite covers the document number + its check, DOB + check, expiry + check, and the
        // personal number + its check (ICAO TD3 positions 1-10, 14-20, 22-43) — NOT nationality or sex.
        val composite = docField + docCheck + f.dateOfBirth + dobCheck + f.expiryDate + expCheck + pnField + pnCheck
        val compositeCheck = check(composite)

        val line2 = docField + docCheck + fitAlpha(f.nationality, 3) + f.dateOfBirth + dobCheck +
            sexChar(f.sex) + f.expiryDate + expCheck + pnField + pnCheck + compositeCheck
        return "$line1\n$line2"
    }

    private val COUNTRIES = listOf("UTO", "GBR", "DEU", "FRA", "NLD", "USA", "CYP", "AUS")
    private val SURNAMES = listOf("ERIKSSON", "SMITH", "MUELLER", "DUPONT", "DEVRIES", "NOVAK")
    private val GIVEN = listOf("ANNA MARIA", "JOHN", "MAX", "PIERRE", "SOPHIE", "LUCA")

    /** A random but fully valid TD3 passport MRZ. */
    fun sampleTd3(rng: Rng): String {
        val country = rng.pick(COUNTRIES)
        return td3(
            Td3Fields(
                issuingCountry = country,
                nationality = country,
                surname = rng.pick(SURNAMES),
                givenNames = rng.pick(GIVEN),
                documentNumber = rng.upperLetter().toString() + rng.digits(7),
                dateOfBirth = dateYymmdd(rng, 1960, 2004),
                sex = rng.pick(listOf("M", "F")),
                expiryDate = dateYymmdd(rng, 2026, 2034),
                personalNumber = rng.digits(9),
            ),
        )
    }

    private fun dateYymmdd(rng: Rng, fromYear: Int, toYear: Int): String {
        val year = rng.intInRange(fromYear, toYear)
        val month = rng.intInRange(1, 12)
        val day = rng.intInRange(1, 28)
        return "%02d%02d%02d".format(year % 100, month, day)
    }

    private fun check(field: String): Char = '0' + Checksums.icao731CheckDigit(field)

    /** Uppercase, keep only A-Z/0-9, replace everything else with '<', pad/truncate to [width]. */
    private fun mrzField(s: String, width: Int): String {
        val cleaned = s.uppercase().map { if (it in 'A'..'Z' || it in '0'..'9') it else '<' }.joinToString("")
        return (cleaned.take(width)).padEnd(width, '<')
    }

    /** Alpha-only field (country codes), padded with '<'. */
    private fun fitAlpha(s: String, width: Int): String {
        val cleaned = s.uppercase().filter { it in 'A'..'Z' }
        return cleaned.take(width).padEnd(width, '<')
    }

    /** TD3 name field: SURNAME<<GIVEN<NAMES, filled with '<' to [width]. */
    private fun nameField(surname: String, givenNames: String, width: Int): String {
        fun norm(s: String) = s.uppercase().map { if (it in 'A'..'Z') it else '<' }.joinToString("").trim('<')
        val raw = norm(surname) + "<<" + norm(givenNames)
        return raw.take(width).padEnd(width, '<')
    }

    private fun sexChar(sex: String): Char = when (sex.uppercase().firstOrNull()) {
        'M' -> 'M'
        'F' -> 'F'
        else -> '<'
    }
}
