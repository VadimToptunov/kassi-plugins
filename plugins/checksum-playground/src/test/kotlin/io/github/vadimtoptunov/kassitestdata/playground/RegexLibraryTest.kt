package io.github.vadimtoptunov.kassitestdata.playground

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegexLibraryTest {

    @Test
    fun `every shipped library pattern compiles`() {
        for ((label, p) in RegexLibrary.PATTERNS) {
            runCatching { Regex(p) }
                .onFailure { throw AssertionError("Library pattern '$label' does not compile: $p", it) }
        }
        assertTrue(RegexLibrary.PATTERNS.isNotEmpty())
    }

    @Test
    fun `library patterns match representative samples`() {
        fun matches(label: String, sample: String) =
            Regex(RegexLibrary.PATTERNS.first { it.first == label }.second).containsMatchIn(sample)

        assertTrue(matches("Email", "user.name+tag@example.co.uk"))
        assertTrue(matches("IPv4 address", "192.168.0.1"))
        assertTrue(matches("UUID", "550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(matches("ISO date (YYYY-MM-DD)", "2026-08-08"))
    }
}
