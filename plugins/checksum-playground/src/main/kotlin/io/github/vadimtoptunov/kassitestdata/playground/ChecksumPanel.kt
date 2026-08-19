package io.github.vadimtoptunov.kassitestdata.playground

import com.intellij.openapi.ui.ComboBox
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

/** Live checksum inspector: pick a type to load a valid example, or paste your own — see every applicable check pass or fail. */
class ChecksumPanel : JPanel(BorderLayout()) {

    private val input = JBTextField()
    private val results = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    private val passColor = JBColor(0x2E7D32, 0x66BB6A)
    private val failColor = JBColor(0xC62828, 0xEF5350)

    init {
        border = JBUI.Borders.empty(8)

        val typePicker = ComboBox<String>().apply {
            addItem(PLACEHOLDER)
            EXAMPLES.forEach { addItem(it.first) }
            addActionListener {
                val i = selectedIndex
                if (i > 0) {
                    input.text = EXAMPLES[i - 1].second
                    selectedIndex = 0 // reset so the same type can be picked again
                }
            }
        }

        val row = JPanel(BorderLayout(6, 0)).apply {
            add(typePicker, BorderLayout.WEST)
            add(input, BorderLayout.CENTER)
        }
        val header = JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel("Pick a type to load a valid example, or paste your own value:"), BorderLayout.NORTH)
            add(row, BorderLayout.CENTER)
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
            results.add(JBLabel("No applicable checks yet — pick a type above or paste a value.").apply {
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

    private companion object {
        const val PLACEHOLDER = "Load an example…"

        // Label → a valid example value (verified against the engine's checks).
        val EXAMPLES: List<Pair<String, String>> = listOf(
            "IBAN (GB)" to "GB82 WEST 1234 5698 7654 32",
            "Card (Luhn)" to "4242 4242 4242 4242",
            "ISIN" to "US0378331005",
            "IMEI" to "490154203237518",
            "EAN-13" to "5901234123457",
            "ISBN-10" to "0-306-40615-2",
            "ISBN-13" to "978-0-306-40615-7",
            "VIN" to "1M8GDM9AXKP042788",
            "JMBG" to "0101006500006",
            "Bitcoin address" to "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
            "Ethereum address" to "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
            "LEI" to "5493001KJTIIGC8Y1R12",
            "BIC / SWIFT" to "DEUTDEFF",
            "NL BSN" to "123456782",
            "AU TFN" to "123456782",
            "AU ABN" to "51824753556",
            "DE VAT" to "DE136695976",
            "CY VAT" to "CY10259033P",
            "GB NINo" to "AA123456C",
            "RU ИНН (юр.)" to "7830002293",
            "RU ИНН (физ.)" to "500100732259",
            "RU СНИЛС" to "112-233-445 95",
            "RU ОГРН" to "1027700132195",
            "ISO 6346 container" to "CSQU3054383",
        )
    }
}
