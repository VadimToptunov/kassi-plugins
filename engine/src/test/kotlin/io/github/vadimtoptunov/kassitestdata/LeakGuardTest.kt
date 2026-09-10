package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import io.github.vadimtoptunov.kassitestdata.inspect.LeakGuard
import io.github.vadimtoptunov.kassitestdata.inspect.ReservedTestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LeakGuardTest {

    /** A Luhn-valid 16-digit PAN whose BIN is deliberately outside every known test-BIN range. */
    private fun realLookingPan(): String {
        val body = "999219501128303" // 15 digits, BIN "999219" is not a reserved test BIN
        return body + Checksums.luhnCheckDigit(body)
    }

    @Test
    fun `a Luhn-valid PAN outside the reserved test BINs is flagged, with a reserved-BIN replacement`() {
        val pan = realLookingPan()
        val findings = LeakGuard.scan("card on file: $pan (exp 12/29)")
        assertEquals(1, findings.size, findings.toString())
        val finding = findings.single()
        assertEquals(LeakGuard.Kind.CARD_PAN, finding.kind)
        assertEquals(pan, finding.matched)
        assertTrue(Checksums.isLuhnValid(finding.replacement), "replacement must itself be Luhn-valid")
        assertTrue(LeakGuard.scan(finding.replacement).isEmpty(), "replacement must not be re-flagged")
    }

    @Test
    fun `a PAN built from a known test BIN is not flagged`() {
        val testPan = ReservedTestData.testPanFor(16)!!
        assertTrue(LeakGuard.scan("Stripe test card: $testPan").isEmpty())
    }

    @Test
    fun `a valid-checksum IBAN outside the reserved set is flagged, with a reserved replacement`() {
        val iban = BankAccountGenerator.iban(Country.DE, Rng(123L), valid = true)
        val findings = LeakGuard.scan("wire to $iban please")
        assertEquals(1, findings.size, findings.toString())
        val finding = findings.single()
        assertEquals(LeakGuard.Kind.IBAN, finding.kind)
        assertEquals(iban, finding.matched)
        assertEquals(ReservedTestData.testIbanFor(Country.DE), finding.replacement)
        assertTrue(Checksums.isValidIbanMod97(finding.replacement))
        assertTrue(LeakGuard.scan(finding.replacement).isEmpty(), "replacement must not be re-flagged")
    }

    @Test
    fun `an IBAN that fails mod-97 is not flagged`() {
        val invalid = BankAccountGenerator.iban(Country.DE, Rng(123L), valid = false)
        assertTrue(LeakGuard.scan("wire to $invalid please").isEmpty())
    }

    @Test
    fun `a plausibly-real SSN is flagged, with a never-issued replacement`() {
        val findings = LeakGuard.scan("ssn: 123-45-6789 on file")
        assertEquals(1, findings.size, findings.toString())
        val finding = findings.single()
        assertEquals(LeakGuard.Kind.US_SSN, finding.kind)
        assertEquals("123-45-6789", finding.matched)
        assertEquals(ReservedTestData.testSsn, finding.replacement)
        assertTrue(LeakGuard.scan(finding.replacement).isEmpty(), "replacement must not be re-flagged")
    }

    @Test
    fun `an SSN in a never-issued area is not flagged`() {
        assertTrue(LeakGuard.scan("ssn: 900-12-3456 on file").isEmpty())
    }

    @Test
    fun `secret detectors flag AWS key, JWT and PEM private key with a redaction fix`() {
        // AWS access key ID — AWS-documented format (AKIA + 16 upper-alnum).
        val aws = LeakGuard.scan("aws_access_key_id = AKIAIOSFODNN7EXAMPLE").single()
        assertEquals(LeakGuard.Kind.AWS_ACCESS_KEY, aws.kind)
        assertEquals("AKIAIOSFODNN7EXAMPLE", aws.matched)
        assertEquals(false, aws.kind.synthetic) // redact, not synthetic-replace
        assertTrue(LeakGuard.scan(aws.replacement).isEmpty(), "the redaction placeholder must not re-flag")

        // JWT — three base64url segments, header starts eyJ.
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        assertEquals(LeakGuard.Kind.JWT, LeakGuard.scan("token: $jwt").single().kind)

        // PEM private-key block header.
        assertEquals(
            LeakGuard.Kind.PRIVATE_KEY,
            LeakGuard.scan("-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----").first().kind,
        )
    }

    @Test
    fun `secret detectors do not fire on lookalike non-secrets`() {
        assertTrue(LeakGuard.scan("prefix AKIA123 short and AKIALOWERcase0000000 nope").isEmpty()) // too short / lowercase
        assertTrue(LeakGuard.scan("not a jwt: eyJonly.one").isEmpty())
    }

    @Test
    fun `ordinary prose and code with no PII yields no findings`() {
        val text = """
            fun greet(name: String) = "Hello, ${'$'}name!"
            val port = 8080
            val version = "1.2.3"
        """.trimIndent()
        assertTrue(LeakGuard.scan(text).isEmpty())
    }

    @Test
    fun `a fixture blob mixing several PII types is fully caught in one pass`() {
        val pan = realLookingPan()
        val iban = BankAccountGenerator.iban(Country.NL, Rng(7L), valid = true)
        val text = """
            val card = "$pan"
            val iban = "$iban"
            val ssn = "123-45-6789"
        """.trimIndent()
        val kinds = LeakGuard.scan(text).map { it.kind }.toSet()
        assertEquals(setOf(LeakGuard.Kind.CARD_PAN, LeakGuard.Kind.IBAN, LeakGuard.Kind.US_SSN), kinds)
    }
}
