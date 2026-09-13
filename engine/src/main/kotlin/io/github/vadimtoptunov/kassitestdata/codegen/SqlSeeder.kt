package io.github.vadimtoptunov.kassitestdata.codegen

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.Persona
import io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.CardGenerator

/**
 * Emits `INSERT` statements that fill a table with N **coherent, spec-valid** rows: each row is one
 * synthetic [PersonaGenerator] persona, and each column is mapped to a persona field by its name
 * (`iban`, `card`, `email`, `dob`, `name`, `vat`, `bic`, `id`, `amount`, …). Validity comes from the
 * shared, tested engine (IBAN mod-97, card Luhn); one persona per row keeps the row internally
 * consistent. Pure and deterministic under a seed — the DataGrip surface only drops the text.
 */
object SqlSeeder {

    fun generateInserts(
        table: String,
        columns: List<String>,
        rows: Int,
        seed: Long = 42L,
        country: Country = Country.DE,
    ): String {
        val cols = columns.map { it.trim() }.filter { it.isNotEmpty() }
        if (cols.isEmpty() || rows <= 0) return ""
        val colList = cols.joinToString(", ")
        val sb = StringBuilder()
        for (i in 0 until rows) {
            val persona = PersonaGenerator.generate(country, seed + i)
            val values = cols.joinToString(", ") { sqlValue(it, persona, i) }
            sb.append("INSERT INTO ").append(table).append(" (").append(colList).append(") VALUES (")
                .append(values).append(");\n")
        }
        return sb.toString().trimEnd()
    }

    /** Map a column to a SQL literal by name; unknown columns get a stable placeholder string. */
    private fun sqlValue(column: String, persona: Persona, rowIndex: Int): String {
        val c = column.lowercase()
        return when {
            c == "id" || c.endsWith("_id") -> (rowIndex + 1).toString()
            c.contains("iban") || c.contains("account") -> str(persona.bankValue)
            c.contains("card") || c.contains("pan") -> str(cardFor(persona))
            c.contains("bic") || c.contains("swift") -> str(persona.bic)
            c.contains("email") || c.contains("mail") -> str(emailFor(persona, rowIndex))
            c.contains("vat") || c.contains("tax") -> persona.taxId?.let { str(it) } ?: "NULL"
            c.contains("name") -> str(persona.fullName)
            c.contains("dob") || c.contains("birth") || c.contains("date") -> str(persona.dateOfBirth.toString())
            c.contains("address") || c.contains("street") -> str(persona.address)
            c.contains("amount") || c.contains("price") || c.contains("balance") -> "%d.%02d".format(rowIndex + 10, rowIndex % 100)
            else -> str("val_${column}_${rowIndex + 1}")
        }
    }

    // The persona carries an IBAN + BIC but no card, so derive a Luhn-valid card deterministically from it.
    private fun cardFor(persona: Persona): String =
        CardGenerator.pan(CardGenerator.Network.VISA, Rng(persona.bankValue.hashCode().toLong()), valid = true)
    private fun emailFor(persona: Persona, rowIndex: Int): String {
        val slug = persona.fullName.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
        return "$slug$rowIndex@example.com" // example.com is RFC 2606 reserved
    }

    private fun str(value: String): String = "'" + value.replace("'", "''") + "'"
}
