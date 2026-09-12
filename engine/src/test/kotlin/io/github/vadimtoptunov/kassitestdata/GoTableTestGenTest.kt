package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.codegen.GoTableTestGen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GoTableTestGenTest {

    @Test
    fun `IBAN cases are exactly as labelled (valid pass mod-97, invalid fail)`() {
        val cases = GoTableTestGen.cases(GoTableTestGen.Kind.IBAN)
        assertTrue(cases.size >= 8, "expected several countries")
        for (c in cases) {
            if (c.valid) assertTrue(Checksums.isValidIbanMod97(c.input), "valid case must pass mod-97: ${c.name} -> ${c.input}")
            else assertFalse(Checksums.isValidIbanMod97(c.input), "invalid case must fail mod-97: ${c.name} -> ${c.input}")
        }
    }

    @Test
    fun `CARD cases are exactly as labelled (valid pass Luhn, invalid fail)`() {
        for (c in GoTableTestGen.cases(GoTableTestGen.Kind.CARD)) {
            if (c.valid) assertTrue(Checksums.isLuhnValid(c.input), "valid card must pass Luhn: ${c.name}")
            else assertFalse(Checksums.isLuhnValid(c.input), "invalid card must fail Luhn: ${c.name}")
        }
    }

    @Test
    fun `emits a compilable-looking Go table test`() {
        val go = GoTableTestGen.generate(GoTableTestGen.Kind.IBAN)
        assertTrue(go.startsWith("package validation_test"), go)
        assertTrue(go.contains("func TestValidateIBAN(t *testing.T)"))
        assertTrue(go.contains("tests := []struct {"))
        assertTrue(go.contains("t.Run(tt.name, func(t *testing.T) {"))
        // every emitted string literal is balanced (even number of unescaped quotes per case line)
        assertTrue(go.lines().none { it.trimStart().startsWith("{") && it.count { ch -> ch == '"' } % 2 != 0 })
    }

    @Test
    fun `is deterministic under a seed`() {
        assertEquals(GoTableTestGen.generate(GoTableTestGen.Kind.CARD, 7L), GoTableTestGen.generate(GoTableTestGen.Kind.CARD, 7L))
    }
}
