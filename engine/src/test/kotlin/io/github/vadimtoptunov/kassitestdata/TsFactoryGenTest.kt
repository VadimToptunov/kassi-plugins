package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.codegen.GoTableTestGen
import io.github.vadimtoptunov.kassitestdata.codegen.TsFactoryGen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TsFactoryGenTest {

    @Test
    fun `emits a well-formed Vitest it_each table with one row per validated case`() {
        for (kind in GoTableTestGen.Kind.entries) {
            val ts = TsFactoryGen.generate(kind)
            val fn = kind.funcName.replaceFirstChar { it.lowercase() }
            assertTrue(ts.startsWith("import { describe, it, expect } from 'vitest';"), ts)
            assertTrue(ts.contains("import { $fn } from './validators';"))
            assertTrue(ts.contains("describe('$fn', () => {"))
            assertTrue(ts.contains("it.each(["))
            assertTrue(ts.contains("expect($fn(value as string)).toBe(expected);"))
            val cases = GoTableTestGen.cases(kind)
            assertEquals(cases.size, ts.lines().count { it.trimStart().startsWith("['") })
            for (c in cases) {
                assertTrue(ts.contains("['${c.input}', ${c.valid}],"), "missing row for ${c.name} -> ${c.input} (${c.valid})")
            }
        }
    }

    @Test
    fun `is deterministic under a seed`() {
        assertEquals(TsFactoryGen.generate(GoTableTestGen.Kind.CARD, 7L), TsFactoryGen.generate(GoTableTestGen.Kind.CARD, 7L))
    }
}
