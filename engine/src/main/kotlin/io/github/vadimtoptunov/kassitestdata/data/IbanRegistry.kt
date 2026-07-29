package io.github.vadimtoptunov.kassitestdata.data

import io.github.vadimtoptunov.kassitestdata.core.Country

/**
 * IBAN structure registry (ISO 13616 / the SWIFT IBAN Registry): total length and the BBAN
 * layout for every IBAN-participating country in the coverage set. Bundled reference data,
 * no network.
 *
 * BBAN layout is a comma-separated list of segments `<count><type>`, where type is:
 *  - `n` numeric digit
 *  - `a` uppercase letter
 *  - `c` uppercase alphanumeric
 *
 * The plugin generates a BBAN matching this layout and appends ISO 7064 mod-97 check digits,
 * so every result has the correct national length and passes the IBAN mod-97 check. National
 * BBAN sub-checks (e.g. the Spanish/Italian/French internal keys) are not separately enforced
 * in v1 — the IBAN-level check is the standard one validators use.
 */
data class IbanSpec(val country: Country, val length: Int, val bban: String) {
    val bbanSegments: List<Pair<Int, Char>> by lazy {
        bban.split(",").map { seg ->
            val count = seg.dropLast(1).toInt()
            val type = seg.last()
            require(type in charArrayOf('n', 'a', 'c')) { "bad IBAN segment type in '$seg'" }
            count to type
        }
    }
}

object IbanRegistry {
    private val specs: Map<Country, IbanSpec> = buildMap {
        fun put(c: Country, len: Int, bban: String) = put(c, IbanSpec(c, len, bban))

        put(Country.AD, 24, "4n,4n,12c")
        put(Country.AL, 28, "8n,16c")
        put(Country.AT, 20, "5n,11n")
        put(Country.BA, 20, "16n")
        put(Country.BE, 16, "12n")
        put(Country.BG, 22, "4a,4n,2n,8c")
        put(Country.CH, 21, "5n,12c")
        put(Country.CY, 28, "3n,5n,16c")
        put(Country.CZ, 24, "20n")
        put(Country.DE, 22, "18n")
        put(Country.DK, 18, "14n")
        put(Country.EE, 20, "16n")
        put(Country.ES, 24, "20n")
        put(Country.FI, 18, "14n")
        put(Country.FO, 18, "14n")
        put(Country.FR, 27, "5n,5n,11c,2n")
        put(Country.GB, 22, "4a,6n,8n")
        put(Country.GI, 23, "4a,15c")
        put(Country.GL, 18, "14n")
        put(Country.GR, 27, "3n,4n,16c")
        put(Country.HR, 21, "17n")
        put(Country.HU, 28, "24n")
        put(Country.IE, 22, "4a,6n,8n")
        put(Country.IS, 26, "22n")
        put(Country.IT, 27, "1a,5n,5n,12c")
        put(Country.LI, 21, "5n,12c")
        put(Country.LT, 20, "16n")
        put(Country.LU, 20, "3n,13c")
        put(Country.LV, 21, "4a,13c")
        put(Country.MC, 27, "5n,5n,11c,2n")
        put(Country.MD, 24, "20c")
        put(Country.ME, 22, "18n")
        put(Country.MK, 19, "3n,10c,2n")
        put(Country.MT, 31, "4a,5n,18c")
        put(Country.NL, 18, "4a,10n")
        put(Country.NO, 15, "11n")
        put(Country.PL, 28, "24n")
        put(Country.PT, 25, "21n")
        put(Country.RO, 24, "4a,16c")
        put(Country.RS, 22, "18n")
        put(Country.SE, 24, "20n")
        put(Country.SI, 19, "15n")
        put(Country.SK, 24, "20n")
        put(Country.SM, 27, "1a,5n,5n,12c")
        put(Country.UA, 29, "6n,19c")
        put(Country.VA, 22, "18n")
        put(Country.XK, 20, "16n")
    }

    fun specFor(country: Country): IbanSpec? = specs[country]

    val supportedCountries: List<Country> get() = specs.keys.sortedBy { it.displayName }
}
