package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.codegen.GoTableTestGen
import io.github.vadimtoptunov.kassitestdata.codegen.PhpFactoryGen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhpFactoryGenTest {

    @Test
    fun `emits a well-formed PHPUnit dataProvider test with one row per validated case`() {
        for (kind in GoTableTestGen.Kind.entries) {
            val php = PhpFactoryGen.generate(kind)
            assertTrue(php.startsWith("<?php"), php)
            assertTrue(php.contains("final class ${kind.funcName}Test extends TestCase"))
            assertTrue(php.contains("@dataProvider cases"))
            assertTrue(php.contains("\$this->assertSame(\$expected, Validator::${kind.funcName}(\$value));"))
            val cases = GoTableTestGen.cases(kind)
            assertEquals(cases.size, php.lines().count { it.trimStart().startsWith("['") })
            for (c in cases) {
                assertTrue(php.contains("['${c.input}', ${c.valid}],"), "missing row for ${c.name} -> ${c.input} (${c.valid})")
            }
        }
    }

    @Test
    fun `is deterministic under a seed`() {
        assertEquals(PhpFactoryGen.generate(GoTableTestGen.Kind.NATIONAL_ID, 7L), PhpFactoryGen.generate(GoTableTestGen.Kind.NATIONAL_ID, 7L))
    }
}
