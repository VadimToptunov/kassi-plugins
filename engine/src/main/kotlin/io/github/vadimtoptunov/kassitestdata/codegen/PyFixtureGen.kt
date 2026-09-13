package io.github.vadimtoptunov.kassitestdata.codegen

/**
 * Emits a pytest parametrized test whose cases are spec-valid values (and their invalid variants) from
 * the shared engine — the same [GoTableTestGen.cases] used by the Go emitter, rendered as Python. Pure
 * and deterministic; the IDE module only drops the text at the caret. Fixtures that actually pass the
 * real checksum, unlike a hand-rolled faker.
 */
object PyFixtureGen {

    private fun pyFunc(kind: GoTableTestGen.Kind): String = when (kind) {
        GoTableTestGen.Kind.IBAN -> "validate_iban"
        GoTableTestGen.Kind.CARD -> "validate_card"
    }

    /** A complete pytest parametrized validation test for [kind]. */
    fun generate(kind: GoTableTestGen.Kind, seed: Long = 42L): String {
        val fn = pyFunc(kind)
        val arg = kind.field
        val rows = GoTableTestGen.cases(kind, seed).joinToString("\n") { c ->
            "        (${pyString(c.input)}, ${if (c.valid) "True" else "False"}),  # ${c.name}"
        }
        val sb = StringBuilder()
        sb.append("import pytest\n\n\n")
        sb.append("# $fn is the function under test — replace with your implementation.\n")
        sb.append("@pytest.mark.parametrize(\n")
        sb.append("    (\"$arg\", \"valid\"),\n")
        sb.append("    [\n")
        sb.append(rows).append('\n')
        sb.append("    ],\n")
        sb.append(")\n")
        sb.append("def test_$fn($arg, valid):\n")
        sb.append("    assert $fn($arg) is valid\n")
        return sb.toString()
    }

    private fun pyString(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
