package io.github.vadimtoptunov.kassitestdata.inspect

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry
import io.github.vadimtoptunov.kassitestdata.generators.CardGenerator
import io.github.vadimtoptunov.kassitestdata.generators.UsSsnGenerator

/**
 * Scans arbitrary text for values that are checksum-valid AND fall outside every publicly
 * documented "reserved for testing" range — the same distinction a payment gateway (test BINs)
 * or the SSA itself (never-issued SSN areas) already draws. A value that merely passes its
 * checksum is not risky by itself — every other Kassi plugin produces plenty of those — but one
 * that ALSO sits outside the reserved ranges is either genuinely real or an accident waiting to
 * be mistaken for one, which is what's worth flagging before a commit.
 *
 * Pure text in, findings out — no IDE/PSI dependency, so the plugin module only has to map the
 * returned offsets onto its editor/document and stays UI-only.
 */
object LeakGuard {

    /**
     * [synthetic] = the finding can be swapped for a checksum-valid synthetic test value from a reserved
     * range (card/IBAN/SSN). Secrets have no such equivalent — the only safe fix is redaction — so they
     * are [synthetic] = false and their [Finding.replacement] is a redaction placeholder.
     */
    enum class Kind(val label: String, val synthetic: Boolean) {
        CARD_PAN("card number (PAN)", synthetic = true),
        IBAN("IBAN", synthetic = true),
        US_SSN("US Social Security Number", synthetic = true),
        AWS_ACCESS_KEY("AWS access key ID", synthetic = false),
        JWT("JWT", synthetic = false),
        PRIVATE_KEY("private key", synthetic = false),
    }

    data class Finding(val kind: Kind, val range: IntRange, val matched: String, val replacement: String)

    // Bounded run of digits/spaces/dashes, not glued to a longer alphanumeric token on either side.
    private val PAN_CANDIDATE = Regex("""(?<![\w.-])[0-9][0-9 -]{10,23}[0-9](?![\w.-])""")
    private val IBAN_CANDIDATE = Regex("""\b[A-Z]{2}\d{2}[A-Z0-9]{11,30}\b""", RegexOption.IGNORE_CASE)
    private val SSN_CANDIDATE = Regex("""\b\d{3}-\d{2}-\d{4}\b""")

    private val reservedTestBins: Set<String> = CardGenerator.Network.entries.flatMap { it.testBins }.toSet()

    // Secret patterns — high-precision (a match IS the signal; no checksum/reserved-range applies).
    // AWS access key ID: AKIA/ASIA + 16 upper-alnum (AWS-documented format).
    private val AWS_KEY = Regex("""\b(?:AKIA|ASIA)[0-9A-Z]{16}\b""")
    // JWT: three base64url segments; the header segment starts `eyJ` (base64 of `{"`).
    private val JWT = Regex("""\beyJ[A-Za-z0-9_-]{5,}\.eyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\b""")
    // PEM private-key block header (RSA/EC/OpenSSH/DSA/PGP or bare).
    private val PRIVATE_KEY = Regex("""-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----""")

    fun scan(text: String): List<Finding> =
        (scanIbans(text) + scanPans(text) + scanSsns(text) + scanSecrets(text)).sortedBy { it.range.first }

    private fun scanSecrets(text: String): List<Finding> = buildList {
        AWS_KEY.findAll(text).forEach { add(Finding(Kind.AWS_ACCESS_KEY, it.range, it.value, "REDACTED_AWS_ACCESS_KEY")) }
        JWT.findAll(text).forEach { add(Finding(Kind.JWT, it.range, it.value, "REDACTED_JWT")) }
        PRIVATE_KEY.findAll(text).forEach { add(Finding(Kind.PRIVATE_KEY, it.range, it.value, "REDACTED_PRIVATE_KEY")) }
    }

    private fun scanIbans(text: String): List<Finding> = IBAN_CANDIDATE.findAll(text).mapNotNull { m ->
        val compact = m.value.uppercase()
        val country = Country.entries.find { it.code == compact.take(2) } ?: return@mapNotNull null
        val spec = IbanRegistry.specFor(country) ?: return@mapNotNull null
        if (compact.length != spec.length) return@mapNotNull null
        if (!Checksums.isValidIbanMod97(compact)) return@mapNotNull null
        if (ReservedTestData.isReservedIban(compact)) return@mapNotNull null
        val replacement = ReservedTestData.testIbanFor(country) ?: return@mapNotNull null
        Finding(Kind.IBAN, m.range, m.value, replacement)
    }.toList()

    private fun scanPans(text: String): List<Finding> = PAN_CANDIDATE.findAll(text).mapNotNull { m ->
        val compact = m.value.filter { it in '0'..'9' }
        if (compact.length !in 12..19) return@mapNotNull null
        if (!Checksums.isLuhnValid(compact)) return@mapNotNull null
        if (reservedTestBins.any { compact.startsWith(it) }) return@mapNotNull null
        val replacement = ReservedTestData.testPanFor(compact.length) ?: return@mapNotNull null
        Finding(Kind.CARD_PAN, m.range, m.value, replacement)
    }.toList()

    private fun scanSsns(text: String): List<Finding> = SSN_CANDIDATE.findAll(text).mapNotNull { m ->
        if (!UsSsnGenerator.isPlausiblyReal(m.value)) return@mapNotNull null
        Finding(Kind.US_SSN, m.range, m.value, ReservedTestData.testSsn)
    }.toList()
}
