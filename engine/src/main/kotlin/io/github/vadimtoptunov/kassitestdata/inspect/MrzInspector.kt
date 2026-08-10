package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.algo.Checksums

/**
 * Parses and validates travel-document Machine Readable Zones (ICAO Doc 9303): TD1 (3 lines of
 * 30, ID cards), TD2 (2 lines of 36) and TD3 (2 lines of 44, passports). Every per-field check
 * digit — and the composite check digit — is verified with the same ICAO 7-3-1 routine already
 * used by the DE ID card generator ([Checksums.icao731CheckDigit]); this is the reader companion
 * to that writer.
 */
object MrzInspector {

    enum class Format(val lineCount: Int, val lineLength: Int) {
        TD1(3, 30),
        TD2(2, 36),
        TD3(2, 44),
    }

    /** One check-digit-bearing field: its raw value, the check-digit character read from the MRZ, and the verdict. */
    data class FieldCheck(
        val name: String,
        val value: String,
        val checkDigit: Char,
        val valid: Boolean,
        val expectedCheckDigit: Char,
    )

    data class MrzResult(
        val format: Format,
        val documentType: String,
        val issuingCountry: String,
        val nationality: String,
        val sex: String,
        val surname: String,
        val givenNames: String,
        val documentNumber: FieldCheck,
        val dateOfBirth: FieldCheck,
        val expiryDate: FieldCheck,
        /** TD3 only — the optional personal-number field carries its own check digit there. */
        val personalNumber: FieldCheck?,
        val composite: FieldCheck,
    ) {
        val allChecksValid: Boolean
            get() = documentNumber.valid && dateOfBirth.valid && expiryDate.valid &&
                (personalNumber?.valid ?: true) && composite.valid
    }

    sealed class Outcome {
        data class Success(val result: MrzResult) : Outcome()
        data class Invalid(val reason: String) : Outcome()
    }

    fun inspect(rawLines: List<String>): Outcome {
        val lines = rawLines.map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        val format = Format.entries.firstOrNull { fmt -> fmt.lineCount == lines.size && lines.all { it.length == fmt.lineLength } }
            ?: return Outcome.Invalid(
                "Expected 2 lines of 44 chars (TD3/passport), 2 of 36 (TD2), or 3 of 30 (TD1/ID card); " +
                    "got ${lines.size} line(s) of length(s) ${lines.map { it.length }}",
            )

        return try {
            Outcome.Success(
                when (format) {
                    Format.TD3 -> parseTd3(lines)
                    Format.TD2 -> parseTd2(lines)
                    Format.TD1 -> parseTd1(lines)
                },
            )
        } catch (e: IndexOutOfBoundsException) {
            Outcome.Invalid("Malformed $format MRZ: field out of range")
        }
    }

    private fun fieldCheck(name: String, value: String, checkDigitChar: Char): FieldCheck {
        val expected = '0' + Checksums.icao731CheckDigit(value) // the digit the value should carry
        val valid = when {
            checkDigitChar in '0'..'9' -> Checksums.icao731CheckDigit(value) == (checkDigitChar - '0')
            checkDigitChar == '<' && value.all { it == '<' } -> true
            else -> false
        }
        return FieldCheck(name, value, checkDigitChar, valid, expected)
    }

    private fun parseName(nameField: String): Pair<String, String> {
        val parts = nameField.split("<<", limit = 2)
        val surname = parts[0].replace('<', ' ').trim()
        val given = parts.getOrElse(1) { "" }.replace('<', ' ').trim()
        return surname to given
    }

    private fun parseTd3(lines: List<String>): MrzResult {
        val (line1, line2) = lines
        val documentType = line1.substring(0, 2).trim('<')
        val issuingCountry = line1.substring(2, 5)
        val (surname, given) = parseName(line1.substring(5, 44))

        val docNumber = line2.substring(0, 9)
        val docNumberCheck = fieldCheck("Document number", docNumber, line2[9])
        val nationality = line2.substring(10, 13)
        val dob = line2.substring(13, 19)
        val dobCheck = fieldCheck("Date of birth", dob, line2[19])
        val sex = line2.substring(20, 21)
        val expiry = line2.substring(21, 27)
        val expiryCheck = fieldCheck("Expiry date", expiry, line2[27])
        val personalNumber = line2.substring(28, 42)
        val personalCheck = fieldCheck("Personal number", personalNumber, line2[42])
        val compositeInput = docNumber + line2[9] + dob + line2[19] + expiry + line2[27] + personalNumber + line2[42]
        val compositeCheck = fieldCheck("Composite", compositeInput, line2[43])

        return MrzResult(
            Format.TD3, documentType, issuingCountry, nationality, sex, surname, given,
            docNumberCheck, dobCheck, expiryCheck, personalCheck, compositeCheck,
        )
    }

    private fun parseTd2(lines: List<String>): MrzResult {
        val (line1, line2) = lines
        val documentType = line1.substring(0, 2).trim('<')
        val issuingCountry = line1.substring(2, 5)
        val (surname, given) = parseName(line1.substring(5, 36))

        val docNumber = line2.substring(0, 9)
        val docNumberCheck = fieldCheck("Document number", docNumber, line2[9])
        val nationality = line2.substring(10, 13)
        val dob = line2.substring(13, 19)
        val dobCheck = fieldCheck("Date of birth", dob, line2[19])
        val sex = line2.substring(20, 21)
        val expiry = line2.substring(21, 27)
        val expiryCheck = fieldCheck("Expiry date", expiry, line2[27])
        val optionalData = line2.substring(28, 35)
        val compositeInput = docNumber + line2[9] + dob + line2[19] + expiry + line2[27] + optionalData
        val compositeCheck = fieldCheck("Composite", compositeInput, line2[35])

        return MrzResult(
            Format.TD2, documentType, issuingCountry, nationality, sex, surname, given,
            docNumberCheck, dobCheck, expiryCheck, null, compositeCheck,
        )
    }

    private fun parseTd1(lines: List<String>): MrzResult {
        val (line1, line2, line3) = lines
        val documentType = line1.substring(0, 2).trim('<')
        val issuingCountry = line1.substring(2, 5)
        val docNumber = line1.substring(5, 14)
        val docNumberCheck = fieldCheck("Document number", docNumber, line1[14])
        val optionalData1 = line1.substring(15, 30)

        val dob = line2.substring(0, 6)
        val dobCheck = fieldCheck("Date of birth", dob, line2[6])
        val sex = line2.substring(7, 8)
        val expiry = line2.substring(8, 14)
        val expiryCheck = fieldCheck("Expiry date", expiry, line2[14])
        val nationality = line2.substring(15, 18)
        val optionalData2 = line2.substring(18, 29)
        val compositeInput = docNumber + line1[14] + optionalData1 + dob + line2[6] + expiry + line2[14] + optionalData2
        val compositeCheck = fieldCheck("Composite", compositeInput, line2[29])

        val (surname, given) = parseName(line3)

        return MrzResult(
            Format.TD1, documentType, issuingCountry, nationality, sex, surname, given,
            docNumberCheck, dobCheck, expiryCheck, null, compositeCheck,
        )
    }
}
