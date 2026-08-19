package io.github.vadimtoptunov.mrzinspector.toolwindow

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.inspect.MrzInspector
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Paste 2 or 3 MRZ lines (TD3 passport, TD2, or TD1 ID card); below, every field the format
 * carries plus a pass/fail verdict for each check digit, refreshed on every keystroke via
 * [MrzInspector].
 */
class MrzPanel : JPanel(BorderLayout()) {

    private val input = JBTextArea(3, 44)
    private val output = JBTextArea().apply { isEditable = false }

    init {
        border = JBUI.Borders.empty(8)

        val top = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
            add(JBLabel("MRZ lines (one per line — TD3 2×44, TD2 2×36, or TD1 3×30):"), BorderLayout.NORTH)
            add(JBScrollPane(input), BorderLayout.CENTER)
        }
        add(top, BorderLayout.NORTH)
        add(JBScrollPane(output), BorderLayout.CENTER)

        input.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refresh()
            override fun removeUpdate(e: DocumentEvent) = refresh()
            override fun changedUpdate(e: DocumentEvent) = refresh()
        })

        refresh()
    }

    private fun refresh() {
        val lines = input.text.split("\n")
        if (lines.all { it.isBlank() }) {
            output.text = "Paste an MRZ above (one line per row) to parse and validate it."
            return
        }
        when (val outcome = MrzInspector.inspect(lines)) {
            is MrzInspector.Outcome.Invalid -> output.text = "Invalid MRZ: ${outcome.reason}"
            is MrzInspector.Outcome.Success -> output.text = render(outcome.result)
        }
    }

    private fun render(r: MrzInspector.MrzResult): String = buildString {
        appendLine("Format: ${r.format}")
        appendLine("Document type: ${r.documentType}   Issuing country: ${r.issuingCountry}   Nationality: ${r.nationality}")
        appendLine("Name: ${r.surname} / ${r.givenNames}   Sex: ${r.sex}")
        appendLine()
        appendField("Document number", r.documentNumber)
        appendField("Date of birth", r.dateOfBirth)
        appendField("Expiry date", r.expiryDate)
        r.personalNumber?.let { appendField("Personal number", it) }
        r.composite?.let { appendField("Composite", it) } // absent for MRV-A/MRV-B visas
        appendLine()
        appendLine(if (r.allChecksValid) "✓ All check digits valid." else "✗ At least one check digit is invalid — see above.")
    }

    private fun StringBuilder.appendField(label: String, field: MrzInspector.FieldCheck) {
        val mark = if (field.valid) "✓" else "✗"
        val fix = if (field.valid) "" else " — expected '${field.expectedCheckDigit}'"
        appendLine("$mark $label: \"${field.value}\" check digit '${field.checkDigit}'$fix")
    }
}
