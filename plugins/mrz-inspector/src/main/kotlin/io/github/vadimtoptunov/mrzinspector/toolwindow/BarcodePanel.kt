package io.github.vadimtoptunov.mrzinspector.toolwindow

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.inspect.AamvaInspector
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Paste the decoded text payload of an AAMVA PDF417 barcode (US/Canada driver's license or ID
 * card); below, the parsed header and every subfile's data elements, or the exact structural
 * error, refreshed on every keystroke via [AamvaInspector].
 */
class BarcodePanel : JPanel(BorderLayout()) {

    private val input = JBTextArea(4, 44)
    private val output = JBTextArea().apply { isEditable = false }

    init {
        border = JBUI.Borders.empty(8)

        val top = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
            add(JBLabel("AAMVA PDF417 payload (raw decoded text, as read by a barcode scanner):"), BorderLayout.NORTH)
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
        if (input.text.isBlank()) {
            output.text = "Paste an AAMVA barcode payload above to parse its header and subfiles."
            return
        }
        when (val outcome = AamvaInspector.inspect(input.text)) {
            is AamvaInspector.Outcome.Invalid -> output.text = "Invalid payload: ${outcome.reason}"
            is AamvaInspector.Outcome.Success -> output.text = render(outcome.result)
        }
    }

    private fun render(r: AamvaInspector.AamvaResult): String = buildString {
        appendLine("IIN: ${r.iin}   AAMVA version: ${r.aamvaVersion}   Jurisdiction version: ${r.jurisdictionVersion}")
        for (subfile in r.subfiles) {
            appendLine()
            appendLine("Subfile ${subfile.type} (${subfile.fields.size} field(s)):")
            for (field in subfile.fields) {
                appendLine("  ${field.elementId}: ${field.value}")
            }
        }
    }
}
