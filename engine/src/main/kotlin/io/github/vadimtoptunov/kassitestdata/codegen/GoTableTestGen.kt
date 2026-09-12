package io.github.vadimtoptunov.kassitestdata.codegen

import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry
import io.github.vadimtoptunov.kassitestdata.generators.BankAccountGenerator
import io.github.vadimtoptunov.kassitestdata.generators.CardGenerator

/**
 * Emits an idiomatic Go table-driven test whose cases are spec-valid values (and their invalid
 * variants) from the shared engine — the fixtures a hand-rolled faker can't give you, because ours pass
 * the real checksum. Pure string in/out (deterministic under a seed), so it is unit-testable and the
 * IDE module only has to drop the text at the caret. A sibling of the pytest/TS emitters.
 */
object GoTableTestGen {

    enum class Kind(val funcName: String, val field: String) {
        IBAN("ValidateIBAN", "iban"),
        CARD("ValidateCard", "card"),
    }

    /** One `{name, input, valid}` row of the generated table. */
    data class Case(val name: String, val input: String, val valid: Boolean)

    /** The cases the generator will emit for [kind] — exposed so tests can assert their validity. */
    fun cases(kind: Kind, seed: Long = 42L): List<Case> = when (kind) {
        Kind.IBAN -> IbanRegistry.supportedCountries.take(4).flatMap { c ->
            listOf(
                Case("${c.code} valid", BankAccountGenerator.iban(c, Rng(seed), valid = true), true),
                Case("${c.code} bad checksum", BankAccountGenerator.iban(c, Rng(seed), valid = false), false),
            )
        }
        Kind.CARD -> CardGenerator.Network.entries.flatMap { n ->
            listOf(
                Case("${n.displayName} valid", CardGenerator.pan(n, Rng(seed), valid = true), true),
                Case("${n.displayName} fails Luhn", CardGenerator.pan(n, Rng(seed), valid = false), false),
            )
        }
    }

    /** A complete, compilable Go table-driven test for [kind] (idiomatic tab indentation). */
    fun generate(kind: Kind, seed: Long = 42L): String {
        val fn = kind.funcName
        val f = kind.field
        val sb = StringBuilder()
        sb.append("package validation_test\n\n")
        sb.append("import \"testing\"\n\n")
        sb.append("// $fn is the function under test — replace with your implementation.\n")
        sb.append("func Test$fn(t *testing.T) {\n")
        sb.append("\ttests := []struct {\n")
        sb.append("\t\tname  string\n")
        sb.append("\t\t$f  string\n")
        sb.append("\t\tvalid bool\n")
        sb.append("\t}{\n")
        for (c in cases(kind, seed)) sb.append("\t\t{${goString(c.name)}, ${goString(c.input)}, ${c.valid}},\n")
        sb.append("\t}\n")
        sb.append("\tfor _, tt := range tests {\n")
        sb.append("\t\tt.Run(tt.name, func(t *testing.T) {\n")
        sb.append("\t\t\tif got := $fn(tt.$f); got != tt.valid {\n")
        sb.append("\t\t\t\tt.Errorf(\"$fn(%q) = %v, want %v\", tt.$f, got, tt.valid)\n")
        sb.append("\t\t\t}\n")
        sb.append("\t\t})\n")
        sb.append("\t}\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun goString(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
