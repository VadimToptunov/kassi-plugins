package io.github.vadimtoptunov.kassitestdata.algo

/**
 * Standard checksum algorithms used by real-world identifiers.
 *
 * Every function here is an *independent* implementation of a published standard
 * (ISO 7064, ISO/IEC 7812 Luhn, the Dutch 11-proef, etc.). The generators produce
 * values that satisfy these checks; the unit tests validate generated output against
 * these same routines. This is the plugin's correctness moat: "data that actually
 * passes validators", not plausible-looking data.
 */
object Checksums {

    // ---------------------------------------------------------------------
    // ISO 7064 MOD 97-10 — used by IBAN (ISO 13616) and others.
    // ---------------------------------------------------------------------

    /** Convert A→10, B→11 … Z→35; digits stay as-is. */
    private fun toNumericString(input: String): String = buildString {
        for (c in input) {
            when {
                c in '0'..'9' -> append(c)
                c in 'A'..'Z' -> append((c - 'A' + 10).toString())
                c in 'a'..'z' -> append((c.uppercaseChar() - 'A' + 10).toString())
                else -> throw IllegalArgumentException("Illegal char '$c' for mod-97")
            }
        }
    }

    /** Compute n mod 97 over an arbitrarily long numeric string, digit by digit. */
    fun mod97(numeric: String): Int {
        var remainder = 0
        for (c in numeric) {
            require(c in '0'..'9') { "mod97 expects digits only" }
            remainder = (remainder * 10 + (c - '0')) % 97
        }
        return remainder
    }

    /**
     * The two IBAN check digits for a given country code + BBAN, per ISO 13616 / ISO 7064.
     * Rearrange as BBAN + countryCode + "00", convert letters to numbers, then check = 98 - (n mod 97).
     */
    fun ibanCheckDigits(countryCode: String, bban: String): String {
        val rearranged = toNumericString(bban + countryCode + "00")
        val check = 98 - mod97(rearranged)
        return check.toString().padStart(2, '0')
    }

    /** Validate a full IBAN by the mod-97 rule (rearranged remainder must equal 1). Ignores spaces/case. */
    fun isValidIbanMod97(iban: String): Boolean {
        val s = iban.replace(" ", "").uppercase()
        if (s.length < 5) return false
        if (!s.all { it in '0'..'9' || it in 'A'..'Z' }) return false
        val rearranged = s.substring(4) + s.substring(0, 4)
        return mod97(toNumericString(rearranged)) == 1
    }

    // ---------------------------------------------------------------------
    // Luhn (ISO/IEC 7812) — card PANs.
    // ---------------------------------------------------------------------

    /** The Luhn check digit that completes [withoutCheckDigit] into a valid number. */
    fun luhnCheckDigit(withoutCheckDigit: String): Int {
        var sum = 0
        var doubleIt = true // rightmost appended digit is the check; the digit left of it is doubled
        for (i in withoutCheckDigit.indices.reversed()) {
            var d = withoutCheckDigit[i] - '0'
            if (doubleIt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            doubleIt = !doubleIt
        }
        return (10 - (sum % 10)) % 10
    }

    /** Validate a full number by Luhn. */
    fun isLuhnValid(number: String): Boolean {
        if (number.isEmpty() || !number.all { it in '0'..'9' }) return false
        var sum = 0
        var doubleIt = false
        for (i in number.indices.reversed()) {
            var d = number[i] - '0'
            if (doubleIt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            doubleIt = !doubleIt
        }
        return sum % 10 == 0
    }

    /** ISIN (ISO 6166): letters expand to two digits (A=10..Z=35), then Luhn over the string. */
    fun isValidIsin(value: String): Boolean {
        val s = value.uppercase()
        if (!Regex("^[A-Z]{2}[A-Z0-9]{9}[0-9]$").matches(s)) return false
        val expanded = buildString { for (c in s.take(11)) if (c in '0'..'9') append(c) else append(c - 'A' + 10) }
        return luhnCheckDigit(expanded) == (s[11] - '0')
    }

    /** EAN-13 / GTIN-13 check digit (alternating 1,3 weights). */
    fun isValidEan13(value: String): Boolean {
        if (value.length != 13 || !value.all { it in '0'..'9' }) return false
        var sum = 0
        for (i in 0..11) sum += (value[i] - '0') * if (i % 2 == 0) 1 else 3
        return (10 - sum % 10) % 10 == (value[12] - '0')
    }

    /** LEI (ISO 17442 / ISO 7064 MOD 97-10): 18 alnum + 2 check digits, whole thing ≡ 1 (mod 97). */
    fun isValidLei(value: String): Boolean {
        val s = value.uppercase()
        if (!Regex("^[A-Z0-9]{18}[0-9]{2}$").matches(s)) return false
        return mod97(toNumericString(s)) == 1
    }

    // ---------------------------------------------------------------------
    // Dutch 11-proef — BSN (national ID) and legacy BTW/RSIN (tax).
    // ---------------------------------------------------------------------

    /** 9-digit 11-proef: weights 9..2 then -1 on the last digit; sum divisible by 11. */
    fun isValidElevenProof(nineDigits: String): Boolean {
        if (nineDigits.length != 9 || !nineDigits.all { it in '0'..'9' }) return false
        val weights = intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, -1)
        var sum = 0
        for (i in 0..8) sum += (nineDigits[i] - '0') * weights[i]
        return sum % 11 == 0 && nineDigits != "000000000"
    }

    // ---------------------------------------------------------------------
    // Weighted modulo checks.
    // ---------------------------------------------------------------------

    /** Generic weighted-sum modulo check: sum(digit_i * weight_i) % modulus == 0. */
    fun weightedModZero(digits: String, weights: IntArray, modulus: Int): Boolean {
        if (digits.length != weights.size || !digits.all { it in '0'..'9' }) return false
        var sum = 0
        for (i in digits.indices) sum += (digits[i] - '0') * weights[i]
        return sum % modulus == 0
    }

    /** Australian TFN: 9 digits, weights 1,4,3,7,5,8,6,9,10, sum % 11 == 0. */
    val TFN_WEIGHTS = intArrayOf(1, 4, 3, 7, 5, 8, 6, 9, 10)
    fun isValidTfn(tfn: String) = weightedModZero(tfn, TFN_WEIGHTS, 11)

    /** Australian ABN: 11 digits, subtract 1 from first digit, weights below, sum % 89 == 0. */
    val ABN_WEIGHTS = intArrayOf(10, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19)
    fun isValidAbn(abn: String): Boolean {
        if (abn.length != 11 || !abn.all { it in '0'..'9' }) return false
        var sum = 0
        for (i in 0..10) {
            var d = abn[i] - '0'
            if (i == 0) d -= 1
            sum += d * ABN_WEIGHTS[i]
        }
        return sum % 89 == 0
    }

    // ---------------------------------------------------------------------
    // ISO 7064 MOD 11,10 — German VAT (USt-IdNr) check digit.
    // ---------------------------------------------------------------------

    /** MOD 11,10 check digit over [digits] (all but the last of a full number). */
    fun mod1110CheckDigit(digits: String): Int {
        var product = 10
        for (c in digits) {
            var sum = ((c - '0') + product) % 10
            if (sum == 0) sum = 10
            product = (sum * 2) % 11
        }
        return (11 - product) % 10
    }

    /** German USt-IdNr: 9 digits, last is the MOD 11,10 check digit of the first 8. */
    fun isValidGermanVat(nineDigits: String): Boolean {
        if (nineDigits.length != 9 || !nineDigits.all { it in '0'..'9' }) return false
        return mod1110CheckDigit(nineDigits.substring(0, 8)) == (nineDigits[8] - '0')
    }

    // ---------------------------------------------------------------------
    // UK VAT — classic modulo-97 check.
    // ---------------------------------------------------------------------

    private val UK_VAT_WEIGHTS = intArrayOf(8, 7, 6, 5, 4, 3, 2)

    /** UK VAT (9-digit standard): weighted sum of first 7 + last-two-as-int, divisible by 97. */
    fun isValidUkVat(nineDigits: String): Boolean {
        if (nineDigits.length != 9 || !nineDigits.all { it in '0'..'9' }) return false
        var sum = 0
        for (i in 0..6) sum += (nineDigits[i] - '0') * UK_VAT_WEIGHTS[i]
        val check = nineDigits.substring(7).toInt()
        return (sum + check) % 97 == 0
    }

    /** The two UK VAT check digits (00–96) for a given 7-digit base. */
    fun ukVatCheckDigits(sevenDigits: String): String {
        var sum = 0
        for (i in 0..6) sum += (sevenDigits[i] - '0') * UK_VAT_WEIGHTS[i]
        val check = (97 - (sum % 97)) % 97
        return check.toString().padStart(2, '0')
    }

    // ---------------------------------------------------------------------
    // Cyprus VAT / TIC — check letter.
    // ---------------------------------------------------------------------

    private val CY_EVEN_MAP = intArrayOf(1, 0, 5, 7, 9, 13, 15, 17, 19, 21)

    /** Cyprus VAT check letter for the first 8 digits (even positions transformed, sum mod 26 → A..Z). */
    fun cyprusVatCheckLetter(eightDigits: String): Char {
        var sum = 0
        for (i in 0..7) {
            val d = eightDigits[i] - '0'
            sum += if (i % 2 == 0) CY_EVEN_MAP[d] else d
        }
        return 'A' + (sum % 26)
    }

    /** Cyprus VAT: 8 digits + check letter. */
    fun isValidCyprusVat(body: String): Boolean {
        if (body.length != 9) return false
        val digits = body.substring(0, 8)
        val letter = body[8]
        if (!digits.all { it in '0'..'9' } || letter !in 'A'..'Z') return false
        return cyprusVatCheckLetter(digits) == letter
    }

    // ---------------------------------------------------------------------
    // ICAO 7-3-1 — German ID card (Personalausweis) document-number check digit,
    // also the per-field check digit used by MRZ (roadmap v1.1).
    // ---------------------------------------------------------------------

    private val ICAO_WEIGHTS = intArrayOf(7, 3, 1)

    /** Value of an MRZ/ID character: digit → itself, A→10 … Z→35, filler '<' → 0. */
    private fun icaoValue(c: Char): Int = when {
        c in '0'..'9' -> c - '0'
        c in 'A'..'Z' -> c - 'A' + 10
        c == '<' -> 0
        else -> throw IllegalArgumentException("Illegal ICAO char '$c'")
    }

    /** ICAO 7-3-1 check digit over [field]. */
    fun icao731CheckDigit(field: String): Int {
        var sum = 0
        for (i in field.indices) sum += icaoValue(field[i]) * ICAO_WEIGHTS[i % 3]
        return sum % 10
    }

    /** German ID card number: 9-char serial + check digit computed by ICAO 7-3-1. */
    fun isValidGermanIdCard(tenChars: String): Boolean {
        if (tenChars.length != 10) return false
        val serial = tenChars.substring(0, 9)
        val check = tenChars[9]
        if (check !in '0'..'9') return false
        if (!serial.all { it in '0'..'9' || it in 'A'..'Z' }) return false
        return icao731CheckDigit(serial) == (check - '0')
    }

    // ---------------------------------------------------------------------
    // VIN (ISO 3779 / NHTSA) — 17 chars, weighted mod-11 check character at position 9.
    // ---------------------------------------------------------------------

    private val VIN_TRANSLIT: Map<Char, Int> = mapOf(
        'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7, 'H' to 8,
        'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'P' to 7, 'R' to 9,
        'S' to 2, 'T' to 3, 'U' to 4, 'V' to 5, 'W' to 6, 'X' to 7, 'Y' to 8, 'Z' to 9,
    )
    private val VIN_WEIGHTS = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    private fun vinValue(c: Char): Int =
        if (c in '0'..'9') c - '0' else VIN_TRANSLIT[c] ?: throw IllegalArgumentException("Illegal VIN char '$c'")

    /** VIN check character (position 9): weighted sum mod 11, where a remainder of 10 is written as 'X'. */
    fun vinCheckChar(seventeen: String): Char {
        var sum = 0
        for (i in 0 until 17) sum += vinValue(seventeen[i]) * VIN_WEIGHTS[i]
        val r = sum % 11
        return if (r == 10) 'X' else ('0' + r)
    }

    /** Validate a full 17-char VIN by its position-9 check character. */
    fun isValidVin(vin: String): Boolean {
        val s = vin.uppercase()
        if (s.length != 17) return false
        if (s.any { it == 'I' || it == 'O' || it == 'Q' }) return false
        if (!s.all { it in '0'..'9' || it in 'A'..'Z' }) return false
        return vinCheckChar(s) == s[8]
    }

    // ---------------------------------------------------------------------
    // ISBN-10 — mod-11 check character (ISBN-13 is EAN-13, see isValidEan13).
    // ---------------------------------------------------------------------

    /** ISBN-10 check character: weights 10..2 over the first 9 digits; (11 - sum%11)%11, with 10 → 'X'. */
    fun isbn10CheckChar(firstNine: String): Char {
        var sum = 0
        for (i in 0 until 9) sum += (firstNine[i] - '0') * (10 - i)
        val c = (11 - (sum % 11)) % 11
        return if (c == 10) 'X' else ('0' + c)
    }

    /** Validate a full ISBN-10 (hyphens/spaces ignored): 9 digits + mod-11 check character (digit or 'X'). */
    fun isValidIsbn10(value: String): Boolean {
        val s = value.replace("-", "").replace(" ", "").uppercase()
        if (s.length != 10) return false
        if (!s.substring(0, 9).all { it in '0'..'9' }) return false
        val last = s[9]
        if (last !in '0'..'9' && last != 'X') return false
        return isbn10CheckChar(s.substring(0, 9)) == last
    }

    // ---------------------------------------------------------------------
    // JMBG — ex-Yugoslav 13-digit citizen number (RS/SI/ME/MK/BA), weighted mod-11.
    // ---------------------------------------------------------------------

    private val JMBG_WEIGHTS = intArrayOf(7, 6, 5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    /** JMBG check digit for the first 12 digits: m = 11 - (Σ digit·weight mod 11); 11→0, 10→null (unassignable). */
    fun jmbgCheckDigit(first12: String): Int? {
        var sum = 0
        for (i in 0 until 12) sum += (first12[i] - '0') * JMBG_WEIGHTS[i]
        return when (val m = 11 - (sum % 11)) {
            11 -> 0
            10 -> null
            else -> m
        }
    }

    /** Validate a full 13-digit JMBG by its check digit. */
    fun isValidJmbg(value: String): Boolean {
        if (value.length != 13 || !value.all { it in '0'..'9' }) return false
        val check = jmbgCheckDigit(value.substring(0, 12)) ?: return false
        return check == (value[12] - '0')
    }

    // ---- ISO 6346 shipping-container code: 4 letters + 6 digits + 1 check digit ----
    private fun iso6346Value(c: Char): Int {
        var v = 10
        for (ch in 'A' until c) { v++; if (v % 11 == 0) v++ }
        return v
    }
    /** ISO 6346 check digit for the first 10 chars (4 letters + 6 digits). */
    fun iso6346CheckDigit(firstTen: String): Int {
        var sum = 0
        for (i in 0 until 10) {
            val value = if (firstTen[i] in 'A'..'Z') iso6346Value(firstTen[i]) else (firstTen[i] - '0')
            sum += value * (1 shl i)
        }
        return (sum % 11) % 10
    }
    /** Validate a full 11-char ISO 6346 container code. */
    fun isValidIso6346(code: String): Boolean {
        val s = code.uppercase()
        if (s.length != 11) return false
        if (!s.substring(0, 4).all { it in 'A'..'Z' }) return false
        if (!s.substring(4).all { it in '0'..'9' }) return false
        return iso6346CheckDigit(s.substring(0, 10)) == (s[10] - '0')
    }
}
