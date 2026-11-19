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
    fun `high-precision secret detectors flag real tokens but not safe test values`() {
        // Format-shaped synthetic examples (not real secrets), matching each vendor's documented format.
        val github = "ghp_1234567890abcdefghijABCDEFGHIJ123456"   // ghp_ + 36 base62
        val stripeLive = "sk_live_0123456789abcdefABCD"             // sk_live_ + >=10 alnum
        val google = "AIzaSyA1234567890abcdefghijklmnopqrstuv"      // AIza + 35 chars

        assertEquals(LeakGuard.Kind.GITHUB_TOKEN, LeakGuard.scan("token = \"$github\"").single().kind)
        assertEquals(LeakGuard.Kind.STRIPE_SECRET_KEY, LeakGuard.scan("stripe=$stripeLive").single().kind)
        assertEquals(LeakGuard.Kind.GOOGLE_API_KEY, LeakGuard.scan("key: $google").single().kind)

        // Stripe TEST keys are safe and must not be flagged; near-misses (wrong length) must not match.
        assertTrue(LeakGuard.scan("sk_test_0123456789abcdefABCD").isEmpty())
        assertTrue(LeakGuard.scan("ghp_tooShort").isEmpty())
        assertTrue(LeakGuard.scan("AIzaTooShort").isEmpty())
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
    fun `secret detectors flag Slack, GitLab and npm tokens with a redaction fix`() {
        // Tokens are assembled from fragments so no full token literal sits in source (which would
        // trip upstream secret scanners). Each still matches its documented format.
        // Slack token — documented xoxb- bot-token format.
        val slack = LeakGuard.scan("SLACK_TOKEN=" + "xox" + "b-2468135790-1357924680-aBcDeFgHiJkLmNoPqRsTuVwX").single()
        assertEquals(LeakGuard.Kind.SLACK_TOKEN, slack.kind)
        assertEquals(false, slack.kind.synthetic)
        assertTrue(LeakGuard.scan(slack.replacement).isEmpty(), "the redaction placeholder must not re-flag")

        // GitLab PAT — documented glpat- prefix + 20 chars.
        assertEquals(
            LeakGuard.Kind.GITLAB_PAT,
            LeakGuard.scan("gitlab: glp" + "at-ABCDEFghijkl1234_xyz").single().kind,
        )

        // npm token — documented npm_ prefix + 36 base62 chars.
        assertEquals(
            LeakGuard.Kind.NPM_TOKEN,
            LeakGuard.scan("//registry.npmjs.org/:_authToken=" + "npm" + "_abcdefghijklmnopqrstuvwxyz0123456789").single().kind,
        )
    }

    @Test
    fun `secret detectors flag OpenAI, Anthropic and Hugging Face keys with a redaction fix`() {
        // Tokens assembled from fragments so no full literal sits in source; each matches its
        // provider-documented format.
        // Anthropic — documented sk-ant- prefix (scanned before OpenAI; the sk- forms are mutually exclusive).
        val anthropic = LeakGuard.scan("ANTHROPIC_API_KEY=" + "sk-ant-" + "api03-aBcD1234_efGH5678-ijKL").single()
        assertEquals(LeakGuard.Kind.ANTHROPIC_API_KEY, anthropic.kind)
        assertEquals(false, anthropic.kind.synthetic)
        assertTrue(LeakGuard.scan(anthropic.replacement).isEmpty(), "the redaction placeholder must not re-flag")

        // OpenAI — documented project-scoped sk-proj- form.
        assertEquals(
            LeakGuard.Kind.OPENAI_API_KEY,
            LeakGuard.scan("OPENAI_API_KEY=" + "sk-proj-" + "abcdefghijklmnopqrstuvwxyz0123").single().kind,
        )
        // OpenAI — legacy sk- + 48 base62 form.
        assertEquals(
            LeakGuard.Kind.OPENAI_API_KEY,
            LeakGuard.scan("key " + "sk-" + "A".repeat(48)).single().kind,
        )

        // Hugging Face — documented hf_ prefix + 34 base62 chars.
        assertEquals(
            LeakGuard.Kind.HUGGINGFACE_TOKEN,
            LeakGuard.scan("HF_TOKEN=" + "hf_" + "abcdefghijklmnopqrstuvwxyz01234567").single().kind,
        )
    }

    @Test
    fun `secret detectors flag SendGrid, Mailgun and Mailchimp keys with a redaction fix`() {
        // Fragment-assembled; each matches its provider-documented format.
        // SendGrid — documented SG. + 22-char id + . + 43-char secret.
        val sendgrid = LeakGuard.scan("SENDGRID_API_KEY=" + "SG." + "A".repeat(22) + "." + "B".repeat(43)).single()
        assertEquals(LeakGuard.Kind.SENDGRID_API_KEY, sendgrid.kind)
        assertEquals(false, sendgrid.kind.synthetic)
        assertTrue(LeakGuard.scan(sendgrid.replacement).isEmpty(), "the redaction placeholder must not re-flag")

        // Mailgun — documented key- prefix + 32 hex.
        assertEquals(
            LeakGuard.Kind.MAILGUN_API_KEY,
            LeakGuard.scan("MAILGUN=" + "key-" + "0123456789abcdef".repeat(2)).single().kind,
        )
        // Mailchimp — documented 32 hex + -us + datacenter number.
        assertEquals(
            LeakGuard.Kind.MAILCHIMP_API_KEY,
            LeakGuard.scan("MAILCHIMP=" + "0123456789abcdef".repeat(2) + "-us" + "21").single().kind,
        )
    }

    @Test
    fun `secret detectors flag GitHub fine-grained PAT, Shopify and DigitalOcean tokens with a redaction fix`() {
        // Fragment-assembled; each matches its provider-documented format.
        // GitHub fine-grained PAT — documented github_pat_ prefix + 82 chars.
        val ghPat = LeakGuard.scan("GH_PAT=" + "github" + "_pat_" + "A".repeat(82)).single()
        assertEquals(LeakGuard.Kind.GITHUB_FINE_GRAINED_PAT, ghPat.kind)
        assertEquals(false, ghPat.kind.synthetic)
        assertTrue(LeakGuard.scan(ghPat.replacement).isEmpty(), "the redaction placeholder must not re-flag")

        // Shopify — documented shpat_ prefix + 32 hex.
        assertEquals(
            LeakGuard.Kind.SHOPIFY_TOKEN,
            LeakGuard.scan("SHOPIFY=" + "shp" + "at_" + "0123456789abcdef".repeat(2)).single().kind,
        )
        // DigitalOcean — documented dop_v1_ prefix + 64 hex.
        assertEquals(
            LeakGuard.Kind.DIGITALOCEAN_TOKEN,
            LeakGuard.scan("DO_TOKEN=" + "dop" + "_v1_" + "0123456789abcdef".repeat(4)).single().kind,
        )
    }

    @Test
    fun `secret detectors do not fire on lookalike non-secrets`() {
        assertTrue(LeakGuard.scan("prefix AKIA123 short and AKIALOWERcase0000000 nope").isEmpty()) // too short / lowercase
        assertTrue(LeakGuard.scan("not a jwt: eyJonly.one").isEmpty())
        assertTrue(LeakGuard.scan("gitlab: glpat-tooshort and npm_too_short").isEmpty())        // wrong lengths
        assertTrue(LeakGuard.scan("ai keys: sk-proj-short and hf_tooShort and sk-ant-x").isEmpty()) // wrong lengths
        assertTrue(LeakGuard.scan("mail: SG.short.x and key-00 and abcdef-us1").isEmpty())       // wrong lengths
        assertTrue(LeakGuard.scan("plat: github_pat_short and shpat_short and dop_v1_short").isEmpty()) // wrong lengths
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
