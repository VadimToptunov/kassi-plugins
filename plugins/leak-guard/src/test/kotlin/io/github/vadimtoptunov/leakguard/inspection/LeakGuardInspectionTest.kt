package io.github.vadimtoptunov.leakguard.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.vadimtoptunov.kassitestdata.inspect.LeakGuard

/**
 * Golden-file tests for the inspection surface (the piece the engine's own [LeakGuard] tests don't
 * cover): the exact warning range and message, that reserved test fixtures stay quiet, and that the
 * quick-fix actually removes the leak without introducing a new one.
 *
 * The `<warning descr="...">` markup is a test-only convention — [BasePlatformTestCase.myFixture]
 * strips it, runs the inspection, and asserts the highlight lands on exactly that range with exactly
 * that message.
 */
class LeakGuardInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(LeakGuardInspection())
    }

    // A real-looking IBAN (mod-97 valid, not the seed-generated reserved one) must be flagged,
    // wherever it sits — here a plain `.env` file, not source code.
    fun `test flags a real-looking IBAN in a config file`() {
        myFixture.configureByText(
            "app.env",
            "SETTLEMENT_IBAN=<warning descr=\"Possible real IBAN — passes its checksum and isn't in a " +
                "known reserved test range\">GB29NWBK60161331926819</warning>\n",
        )
        myFixture.checkHighlighting()
    }

    // The inverse guard: a value from a reserved payment-gateway test BIN (424242…) is a safe
    // fixture and must NOT be flagged. No markup = the assertion that nothing is highlighted.
    fun `test leaves a reserved test card untouched`() {
        myFixture.configureByText("cards.json", "{ \"pan\": \"4242424242424242\" }")
        myFixture.checkHighlighting()
    }

    // The quick-fix replaces the leak with a generated synthetic value, so there is no fixed "after"
    // to golden-compare against — instead assert the invariant: the leak is gone and the file is
    // clean on a re-scan (the replacement itself must not trip the guard).
    fun `test quick-fix replaces the leak with a clean synthetic value`() {
        myFixture.configureByText("app.env", "SETTLEMENT_IBAN=<caret>GB29NWBK60161331926819\n")

        val fix = myFixture.findSingleIntention("Replace with synthetic test IBAN")
        myFixture.launchAction(fix)

        val after = myFixture.file.text
        assertFalse("the leaked IBAN must be gone", after.contains("GB29NWBK60161331926819"))
        assertTrue("the replacement must not itself trip the guard", LeakGuard.scan(after).isEmpty())
    }
}
