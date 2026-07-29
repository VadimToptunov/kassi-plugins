package io.github.vadimtoptunov.kassitestdata.core

import kotlin.random.Random

/**
 * Thin, deterministic random source. When constructed with a seed the whole plugin
 * becomes reproducible — the same seed yields the same persona and identifiers, which
 * is part of the product DNA (determinism for repeatable test fixtures).
 */
class Rng(seed: Long? = null) {
    private val random: Random = if (seed != null) Random(seed) else Random.Default

    fun int(bound: Int): Int = random.nextInt(bound)
    fun intInRange(fromInclusive: Int, toInclusive: Int): Int = random.nextInt(fromInclusive, toInclusive + 1)
    fun boolean(): Boolean = random.nextBoolean()

    fun digit(): Char = ('0' + random.nextInt(10))
    fun digits(count: Int): String = buildString { repeat(count) { append(digit()) } }

    /** A non-zero leading run of digits (first digit 1..9), useful where a value must not be all zeros. */
    fun digitsNonZeroLead(count: Int): String = buildString {
        append('1' + random.nextInt(9))
        repeat(count - 1) { append(digit()) }
    }

    fun upperLetter(): Char = ('A' + random.nextInt(26))
    fun upperLetters(count: Int): String = buildString { repeat(count) { append(upperLetter()) } }

    /** Uppercase alphanumeric character (IBAN 'c' class). */
    fun alnum(): Char {
        val n = random.nextInt(36)
        return if (n < 10) ('0' + n) else ('A' + (n - 10))
    }
    fun alnums(count: Int): String = buildString { repeat(count) { append(alnum()) } }

    fun <T> pick(items: List<T>): T = items[random.nextInt(items.size)]
}
