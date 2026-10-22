package io.github.vadimtoptunov.kassitestdata.codegen

/**
 * Emits an idiomatic TypeScript Vitest/Jest `it.each` table test whose rows are spec-valid values (and
 * their invalid variants) from the shared engine — fixtures that pass the real checksum, unlike a
 * hand-rolled faker. Reuses [GoTableTestGen.Kind] + [GoTableTestGen.cases]. Pure string in/out.
 */
object TsFactoryGen {

    /** The validator function name in camelCase (ValidateIBAN -> validateIBAN). */
    private fun fnName(kind: GoTableTestGen.Kind): String =
        kind.funcName.replaceFirstChar { it.lowercase() }

    /** A complete Vitest `it.each` test for [kind]; replace the import with your implementation. */
    fun generate(kind: GoTableTestGen.Kind, seed: Long = 42L): String {
        val fn = fnName(kind)
        val sb = StringBuilder()
        sb.append("import { describe, it, expect } from 'vitest';\n")
        sb.append("import { $fn } from './validators'; // replace with your implementation\n\n")
        sb.append("describe('$fn', () => {\n")
        sb.append("  it.each([\n")
        for (c in GoTableTestGen.cases(kind, seed)) {
            sb.append("    ['${c.input}', ${c.valid}], // ${c.name}\n")
        }
        sb.append("  ])('%s -> valid=%s', (value, expected) => {\n")
        sb.append("    expect($fn(value as string)).toBe(expected);\n")
        sb.append("  });\n")
        sb.append("});\n")
        return sb.toString()
    }
}
