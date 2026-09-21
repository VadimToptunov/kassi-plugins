package io.github.vadimtoptunov.leakguard.cli

import io.github.vadimtoptunov.kassitestdata.inspect.LeakGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeakGuardCliTest {

    // ---- fingerprint -------------------------------------------------------------------------

    @Test
    fun `fingerprint is deterministic and line-independent`() {
        val a = fingerprint("src/a.kt", "AWS_ACCESS_KEY", 0, "AKIAIOSFODNN7EXAMPLE")
        val b = fingerprint("src/a.kt", "AWS_ACCESS_KEY", 0, "AKIAIOSFODNN7EXAMPLE")
        assertEquals(a, b)
        assertEquals(16, a.length)
    }

    @Test
    fun `fingerprint changes on token, kind, path or occurrence`() {
        val base = fingerprint("src/a.kt", "AWS_ACCESS_KEY", 0, "AKIAIOSFODNN7EXAMPLE")
        assertNotEquals(base, fingerprint("src/a.kt", "AWS_ACCESS_KEY", 0, "AKIAIOSFODNN7DIFFERENT"))
        assertNotEquals(base, fingerprint("src/a.kt", "JWT", 0, "AKIAIOSFODNN7EXAMPLE"))
        assertNotEquals(base, fingerprint("src/b.kt", "AWS_ACCESS_KEY", 0, "AKIAIOSFODNN7EXAMPLE"))
        assertNotEquals(base, fingerprint("src/a.kt", "AWS_ACCESS_KEY", 1, "AKIAIOSFODNN7EXAMPLE"))
    }

    // ---- baseline ----------------------------------------------------------------------------

    @Test
    fun `baseline round-trips and reports membership`() {
        val fps = listOf("aaaa1111bbbb2222", "cccc3333dddd4444")
        val parsed = Baseline.parse(Baseline.serialize(fps))
        assertEquals(fps.toSet(), parsed.fingerprints)
        assertFalse(parsed.isNew("aaaa1111bbbb2222"))
        assertTrue(parsed.isNew("ffff9999ffff9999"))
    }

    @Test
    fun `an absent baseline file shape parses to empty`() {
        assertEquals(Baseline.EMPTY, Baseline.parse("{ \"schemaVersion\": 1 }"))
    }

    // ---- runner ------------------------------------------------------------------------------

    @Test
    fun `two same-kind findings in a file get distinct occurrence-based fingerprints`() {
        val file = FileFindings(
            "app/.env",
            listOf(
                LocatedFinding(LeakGuard.Kind.AWS_ACCESS_KEY, 1, 1, "AKIAIOSFODNN7EXAMPLE"),
                LocatedFinding(LeakGuard.Kind.AWS_ACCESS_KEY, 2, 1, "AKIAIOSFODNN7EXAMPLE"),
            ),
        )
        val results = buildResults(listOf(file), Baseline.EMPTY)
        assertEquals(2, results.size)
        assertNotEquals(results[0].fingerprint, results[1].fingerprint)
    }

    @Test
    fun `secrets are errors and checksum-values are warnings`() {
        assertEquals("error", levelOf(LeakGuard.Kind.AWS_ACCESS_KEY))
        assertEquals("error", levelOf(LeakGuard.Kind.PRIVATE_KEY))
        assertEquals("warning", levelOf(LeakGuard.Kind.CARD_PAN))
        assertEquals("warning", levelOf(LeakGuard.Kind.US_SSN))
    }

    @Test
    fun `only NEW findings fail, and the severity gate filters by level`() {
        val findings = listOf(
            LocatedFinding(LeakGuard.Kind.AWS_ACCESS_KEY, 1, 1, "AKIAIOSFODNN7EXAMPLE"), // error
            LocatedFinding(LeakGuard.Kind.CARD_PAN, 2, 1, "4111111111111111"), // warning
        )
        val results = buildResults(listOf(FileFindings("f", findings)), Baseline.EMPTY)
        assertEquals(2, failingResults(results, FailGate.ANY).size)
        assertEquals(1, failingResults(results, FailGate.ERROR).size) // only the AWS key
        // A baseline containing both fingerprints suppresses everything.
        val baseline = Baseline(results.map { it.fingerprint }.toSet())
        val rerun = buildResults(listOf(FileFindings("f", findings)), baseline)
        assertTrue(failingResults(rerun, FailGate.ANY).isEmpty())
        assertTrue(rerun.all { it.baselineState == "unchanged" })
    }

    // ---- end to end (real vector: AWS-documented example key) --------------------------------

    @Test
    fun `scan to SARIF flags an AWS key without leaking the secret, and baseline clears it`() {
        val text = "aws_secret_config:\n  key = AKIAIOSFODNN7EXAMPLE\n"
        val scanned = LeakGuard.scan(text)
        val awsFinding = scanned.single { it.kind == LeakGuard.Kind.AWS_ACCESS_KEY }
        assertEquals("AKIAIOSFODNN7EXAMPLE", awsFinding.matched)

        val located = scanned.map { LocatedFinding(it.kind, 2, it.range.first, it.matched) }
        val results = buildResults(listOf(FileFindings("config.yaml", located)), Baseline.EMPTY)
        assertTrue(results.any { it.ruleId == "AWS_ACCESS_KEY" && it.level == "error" && it.baselineState == "new" })

        val sarif = toSarif("1.2.0", results)
        assertTrue(sarif.contains("\"version\": \"2.1.0\""))
        assertTrue(sarif.contains("\"leakGuard/v1\""))
        assertTrue(sarif.contains("\"AWS_ACCESS_KEY\""))
        // The report must never carry the raw secret.
        assertFalse(sarif.contains("AKIAIOSFODNN7EXAMPLE"))

        val baseline = Baseline(results.map { it.fingerprint }.toSet())
        val rerun = buildResults(listOf(FileFindings("config.yaml", located)), baseline)
        assertTrue(failingResults(rerun, FailGate.ANY).isEmpty())
    }
}
