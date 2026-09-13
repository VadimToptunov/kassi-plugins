package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import io.github.vadimtoptunov.kassitestdata.generators.CardGenerator
import io.github.vadimtoptunov.kassitestdata.generators.UsSsnGenerator
import kotlin.math.absoluteValue

/**
 * Replaces PII in a blob (a log, JSON, CSV, or fixture) with **valid-synthetic** equivalents so the
 * repro you attach to a ticket stays reproducible but carries no real data. Unlike a redactor, the
 * replacements pass the real checksum (card Luhn, IBAN mod-97) — and unlike [LeakGuard] (an always-on
 * inspection that flags only *real* leaks), this is an on-demand whole-text sanitizer for everything.
 *
 * Deterministic + coherent: the same original value always maps to the same replacement (seeded by the
 * value), so relationships in the data survive. Detection runs once on the original text and all
 * replacements are spliced in a single pass, so a replacement is never itself re-processed.
 */
object PiiAnonymizer {

    data class Result(val text: String, val count: Int)

    private val EMAIL = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
    private val PAN = Regex("""(?<![\w.-])[0-9][0-9 -]{10,23}[0-9](?![\w.-])""")
    private val IBAN = Regex("""\b[A-Z]{2}\d{2}[A-Z0-9]{11,30}\b""", RegexOption.IGNORE_CASE)
    private val SSN = Regex("""\b\d{3}-\d{2}-\d{4}\b""")

    fun anonymize(text: String): Result {
        val cache = HashMap<String, String>()
        val repls = ArrayList<Pair<IntRange, String>>()

        fun collect(regex: Regex, synth: (String) -> String?) {
            for (m in regex.findAll(text)) {
                val rep = cache[m.value] ?: synth(m.value)?.also { cache[m.value] = it } ?: continue
                repls.add(m.range to rep)
            }
        }

        collect(EMAIL) { "user${it.hashCode().absoluteValue % 100000}@example.com" } // example.com is RFC 2606 reserved
        collect(IBAN) { orig ->
            val compact = orig.uppercase().filter { it.isLetterOrDigit() }
            if (!Checksums.isValidIbanMod97(compact)) return@collect null
            val country = Country.fromCode(compact.take(2))?.takeIf { it in IbanRegistry.supportedCountries } ?: return@collect null
            BankAccountGenerator.iban(country, seed(orig), valid = true)
        }
        collect(PAN) { orig ->
            val digits = orig.filter { it.isDigit() }
            if (digits.length !in 12..19 || !Checksums.isLuhnValid(digits)) return@collect null
            CardGenerator.pan(CardGenerator.Network.VISA, seed(orig), valid = true)
        }
        collect(SSN) { UsSsnGenerator.generate(seed(it)) }

        // Splice non-overlapping replacements in one pass (earliest start wins; ties keep the longer match).
        repls.sortWith(compareBy({ it.first.first }, { -it.first.last }))
        val sb = StringBuilder()
        var pos = 0
        var count = 0
        for ((range, rep) in repls) {
            if (range.first < pos) continue
            sb.append(text, pos, range.first).append(rep)
            pos = range.last + 1
            count++
        }
        sb.append(text, pos, text.length)
        return Result(sb.toString(), count)
    }

    private fun seed(value: String): Rng = Rng(value.hashCode().toLong())
}
