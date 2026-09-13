package io.github.vadimtoptunov.sqlseeder

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.codegen.SqlSeeder
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SpinnerNumberModel

/**
 * Generates `INSERT` statements filling a table with N coherent, spec-valid rows: columns are mapped to
 * synthetic-persona fields by name (iban, card, email, dob, name, vat, bic, id, amount). Fill the table +
 * columns, Generate, Copy. The mapping + validity is the shared, tested [SqlSeeder].
 */
class SqlSeederPanel : JPanel(BorderLayout()) {

    private val table = JBTextField("customers", 16)
    private val columns = JBTextField("id, full_name, iban, card_no, email, dob", 40)
    private val rows = javax.swing.JSpinner(SpinnerNumberModel(5, 1, 1000, 1))
    private val output = JBTextArea(16, 70).apply { isEditable = false }

    init {
        border = JBUI.Borders.empty(8)
        val top = JPanel(FlowLayout(FlowLayout.LEFT, 6, 4)).apply {
            add(JBLabel("Table:")); add(table)
            add(JBLabel("Rows:")); add(rows)
            add(JButton("Generate").apply { addActionListener { generate() } })
            add(JButton("Copy").apply { addActionListener { copy() } })
        }
        val cols = JPanel(BorderLayout(6, 0)).apply {
            add(JBLabel("Columns (comma-separated):"), BorderLayout.WEST)
            add(columns, BorderLayout.CENTER)
        }
        val header = JPanel(BorderLayout(0, 4)).apply {
            add(top, BorderLayout.NORTH)
            add(cols, BorderLayout.CENTER)
        }
        add(header, BorderLayout.NORTH)
        add(JBScrollPane(output), BorderLayout.CENTER)
        generate()
    }

    private fun generate() {
        val n = (rows.value as? Int) ?: 5
        output.text = SqlSeeder.generateInserts(table.text.trim().ifEmpty { "t" }, columns.text.split(","), n)
        output.caretPosition = 0
    }

    private fun copy() {
        if (output.text.isNotEmpty()) CopyPasteManager.getInstance().setContents(StringSelection(output.text))
    }
}
