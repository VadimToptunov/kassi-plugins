package io.github.vadimtoptunov.kassitestdata.codegen

/**
 * Emits an idiomatic PHP PHPUnit `@dataProvider` test whose rows are spec-valid values (and their
 * invalid variants) from the shared engine — fixtures that pass the real checksum, unlike a hand-rolled
 * Faker. Reuses [GoTableTestGen.Kind] + [GoTableTestGen.cases]. Pure string in/out.
 */
object PhpFactoryGen {

    /** A complete PHPUnit test for [kind]; replace `Validator::<fn>` with your implementation. */
    fun generate(kind: GoTableTestGen.Kind, seed: Long = 42L): String {
        val fn = kind.funcName
        val sb = StringBuilder()
        sb.append("<?php\n\n")
        sb.append("use PHPUnit\\Framework\\TestCase;\n\n")
        sb.append("// Validator::$fn is the method under test — replace with your implementation.\n")
        sb.append("final class ${fn}Test extends TestCase\n{\n")
        sb.append("    /** @dataProvider cases */\n")
        sb.append("    public function test$fn(string \$value, bool \$expected): void\n")
        sb.append("    {\n")
        sb.append("        \$this->assertSame(\$expected, Validator::$fn(\$value));\n")
        sb.append("    }\n\n")
        sb.append("    public static function cases(): array\n")
        sb.append("    {\n")
        sb.append("        return [\n")
        for (c in GoTableTestGen.cases(kind, seed)) {
            sb.append("            ['${c.input}', ${c.valid}], // ${c.name}\n")
        }
        sb.append("        ];\n")
        sb.append("    }\n")
        sb.append("}\n")
        return sb.toString()
    }
}
