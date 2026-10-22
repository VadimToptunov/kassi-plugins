package io.github.vadimtoptunov.kassitestdata.codegen

/**
 * Emits an idiomatic C# xUnit data-driven test whose `[InlineData]` rows are spec-valid values (and
 * their invalid variants) from the shared engine — fixtures that pass the real checksum, unlike a
 * hand-rolled Bogus faker. Reuses [GoTableTestGen.Kind] + [GoTableTestGen.cases] so every language
 * emitter draws the same validated rows. Pure string in/out (deterministic under a seed).
 */
object DotNetFakerGen {

    /** A complete xUnit `[Theory]` test for [kind]; replace `Validator.<fn>` with your implementation. */
    fun generate(kind: GoTableTestGen.Kind, seed: Long = 42L): String {
        val fn = kind.funcName
        val sb = StringBuilder()
        sb.append("using Xunit;\n\n")
        sb.append("// $fn is the method under test — replace Validator.$fn with your implementation.\n")
        sb.append("public class ${fn}Tests\n{\n")
        sb.append("    [Theory]\n")
        for (c in GoTableTestGen.cases(kind, seed)) {
            sb.append("    [InlineData(\"${c.input}\", ${c.valid})] // ${c.name}\n")
        }
        sb.append("    public void ${fn}_MatchesSpec(string value, bool expectedValid)\n")
        sb.append("    {\n")
        sb.append("        Assert.Equal(expectedValid, Validator.$fn(value));\n")
        sb.append("    }\n")
        sb.append("}\n")
        return sb.toString()
    }
}
