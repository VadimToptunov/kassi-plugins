package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.inspect.DataInspector
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataInspectorTest {

    private fun passed(input: String, checkContains: String): Boolean =
        DataInspector.inspect(input).firstOrNull { it.name.contains(checkContains) }?.passed
            ?: error("no '$checkContains' check for '$input': ${DataInspector.inspect(input)}")

    @Test
    fun `recognises and validates a correct IBAN, ignoring spaces`() {
        assertTrue(passed("GB82 WEST 1234 5698 7654 32", "IBAN"))
        assertFalse(passed("GB82 WEST 1234 5698 7654 33", "IBAN"))
    }

    @Test
    fun `validates a Luhn card number`() {
        assertTrue(passed("4242 4242 4242 4242", "Luhn"))
        assertFalse(passed("4242 4242 4242 4241", "Luhn"))
    }

    @Test
    fun `validates 9-digit schemes`() {
        assertTrue(passed("123456782", "BSN"))
        assertTrue(passed("123456782", "TFN"))
    }

    @Test
    fun `validates ABN and VAT with country prefix`() {
        assertTrue(passed("51824753556", "ABN"))
        assertTrue(passed("DE136695976", "DE VAT"))
        assertTrue(passed("CY10259033P", "CY VAT"))
        assertFalse(passed("DE136695975", "DE VAT"))
    }

    @Test
    fun `empty or junk input yields no applicable checks`() {
        assertTrue(DataInspector.inspect("").isEmpty())
        assertTrue(DataInspector.inspect("!!!").isEmpty())
    }
}
