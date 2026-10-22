package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.codegen.DotNetFakerGen
import io.github.vadimtoptunov.kassitestdata.codegen.GoTableTestGen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DotNetFakerGenTest {

    @Test
    fun `emits a well-formed xUnit Theory with one InlineData per validated case`() {
        for (kind in GoTableTestGen.Kind.entries) {
            val cs = DotNetFakerGen.generate(kind)
            assertTrue(cs.startsWith("using Xunit;"), cs)
            assertTrue(cs.contains("public class ${kind.funcName}Tests"))
            assertTrue(cs.contains("[Theory]"))
            assertTrue(cs.contains("Assert.Equal(expectedValid, Validator.${kind.funcName}(value));"))
            // one InlineData row per engine case, with C# bool literals matching each case's validity
            val cases = GoTableTestGen.cases(kind)
            assertEquals(cases.size, Regex("\\[InlineData\\(").findAll(cs).count())
            for (c in cases) {
                assertTrue(cs.contains("[InlineData(\"${c.input}\", ${c.valid})]"),
                    "missing row for ${c.name} -> ${c.input} (${c.valid})")
            }
        }
    }

    @Test
    fun `is deterministic under a seed`() {
        assertEquals(DotNetFakerGen.generate(GoTableTestGen.Kind.IBAN, 7L), DotNetFakerGen.generate(GoTableTestGen.Kind.IBAN, 7L))
    }
}
