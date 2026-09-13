package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.codegen.SqlSeeder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SqlSeederTest {

    private val cols = listOf("id", "full_name", "iban", "card_no", "email", "dob")

    @Test
    fun `emits N coherent INSERT rows mapped by column name, with valid iban and card`() {
        val sql = SqlSeeder.generateInserts("customers", cols, 3)
        val lines = sql.lines()
        assertEquals(3, lines.size)
        val prefix = "INSERT INTO customers (id, full_name, iban, card_no, email, dob) VALUES ("
        assertTrue(lines.all { it.startsWith(prefix) && it.endsWith(");") }, sql)

        // id is a 1-based sequence, unquoted
        assertTrue(lines[0].startsWith(prefix + "1, "))
        assertTrue(lines[2].startsWith(prefix + "3, "))

        // the iban column is a mod-97-valid IBAN, the card column a Luhn-valid 16-digit PAN
        val iban = Regex("'(DE[0-9 ]+)'").find(sql)!!.groupValues[1].replace(" ", "")
        assertTrue(Checksums.isValidIbanMod97(iban), "iban must pass mod-97: $iban")
        val card = Regex("'(\\d{16})'").find(sql)!!.groupValues[1]
        assertTrue(Checksums.isLuhnValid(card), "card must pass Luhn: $card")
        assertTrue(sql.contains("@example.com"))
    }

    @Test
    fun `is deterministic and safe on empty input`() {
        assertEquals(SqlSeeder.generateInserts("t", cols, 2), SqlSeeder.generateInserts("t", cols, 2))
        assertEquals("", SqlSeeder.generateInserts("t", emptyList(), 3))
        assertEquals("", SqlSeeder.generateInserts("t", cols, 0))
    }

    @Test
    fun `unknown columns get a stable quoted placeholder`() {
        val sql = SqlSeeder.generateInserts("t", listOf("widget_kind"), 1)
        assertTrue(sql.contains("'val_widget_kind_1'"), sql)
    }
}
