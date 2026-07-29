package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * US Social Security Number. An SSN carries no public checksum; instead the SSA has always
 * reserved certain area/group/serial values that are guaranteed never assigned to a real person
 * (area 000, area 666, area 900–999; group 00; serial 0000). [generate] draws from the 900–999
 * "never issued" area, so every generated value is provably synthetic — the exact property the
 * Secret & Card Leak Guard plugin relies on to tell a real-looking SSN from a safe test fixture.
 */
object UsSsnGenerator {

    private data class Parts(val area: Int, val group: Int, val serial: Int)

    private val PATTERN = Regex("""^(\d{3})-(\d{2})-(\d{4})$""")

    private fun parse(value: String): Parts? {
        val m = PATTERN.matchEntire(value) ?: return null
        return Parts(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }

    private fun isNeverIssued(parts: Parts): Boolean =
        parts.area == 0 || parts.area == 666 || parts.area in 900..999 || parts.group == 0 || parts.serial == 0

    /**
     * True if [value] is "XXX-XX-XXXX" shaped and falls in an area/group/serial the SSA has never
     * issued to a real person — i.e. safe, since it cannot be somebody's real SSN.
     */
    fun isReservedNeverIssued(value: String): Boolean {
        val parts = parse(value) ?: return false
        return isNeverIssued(parts)
    }

    /**
     * True if [value] is "XXX-XX-XXXX" shaped and structurally could be a real, currently
     * issuable SSN (outside every never-issued range) — the leak-risk case.
     */
    fun isPlausiblyReal(value: String): Boolean {
        val parts = parse(value) ?: return false
        return !isNeverIssued(parts)
    }

    /** A synthetic SSN in the 900–999 "never issued" area — safe to use in fixtures, docs, tests. */
    fun generate(rng: Rng): String {
        val area = rng.intInRange(900, 999)
        val group = rng.intInRange(1, 99)
        val serial = rng.intInRange(1, 9999)
        return "%03d-%02d-%04d".format(area, group, serial)
    }
}
