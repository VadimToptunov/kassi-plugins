package io.github.vadimtoptunov.kassitestdata.playground

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.inspect.DataInspector
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/** Live checksum inspector: type/paste a value, see every applicable check pass or fail. */
class ChecksumPanel : JPanel(BorderLayout()) {

    private val input = JBTextField()
    private val results = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val passColor = JBColor(0x2E7D32, 0x66BB6A)
    private val failColor = JBColor(0xC62828, 0xEF5350)

    init {
        border = JBUI.Borders.empty(8)

        val header = JPanel(BorderLayout()).apply {
            add(JBLabel("Paste a value (IBAN, card, VAT, ISIN, IMEI, EAN, LEI, ИНН, СНИЛС, ОГРН…):"), BorderLayout.NORTH)
            add(input, BorderLayout.CENTER)
        }
        add(header, BorderLayout.NORTH)
        add(JBScrollPane(results), BorderLayout.CENTER)

        input.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = refresh()
        })
        refresh()
    }

    private fun refresh() {
        results.removeAll()
        val checks = DataInspector.inspect(input.text)
        if (checks.isEmpty()) {
            results.add(JBLabel("No applicable checks yet — type a value above.").apply {
                border = JBUI.Borders.empty(4, 0)
                foreground = JBColor.GRAY
            })
        } else {
            for (c in checks) {
                results.add(JBLabel("${if (c.passed) "✔" else "✘"}  ${c.name}").apply {
                    foreground = if (c.passed) passColor else failColor
                    border = JBUI.Borders.empty(2, 0)
                })
            }
        }
        results.revalidate()
        results.repaint()
    }
}
