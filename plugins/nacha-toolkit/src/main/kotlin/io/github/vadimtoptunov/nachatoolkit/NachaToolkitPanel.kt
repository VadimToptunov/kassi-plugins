package io.github.vadimtoptunov.nachatoolkit

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.nacha.Nacha
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Validate / generate NACHA ACH files, offline. The file text sits in the top editor; validation
 * results (or generation confirmations) show in the read-only pane below. All logic is in the shared,
 * unit-tested [Nacha] engine — this class is only Swing wiring.
 */
class NachaToolkitPanel : JPanel(BorderLayout()) {

    private val fileArea = JBTextArea(16, 96).apply {
        lineWrap = false
        emptyText.text = "Paste a NACHA ACH file here, or generate a sample."
    }
    private val resultArea = JBTextArea(8, 96).apply { isEditable = false }
    private val rng = Rng() // unseeded: a fresh sample each click

    init {
        border = JBUI.Borders.empty(8)

        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JButton("Validate").apply { addActionListener { runValidation() } })
            add(JButton("Generate sample").apply { addActionListener { generate(valid = true) } })
            add(JButton("Generate invalid sample").apply { addActionListener { generate(valid = false) } })
            add(JButton("Copy").apply { addActionListener { copyFile() } })
        }

        val center = JPanel(BorderLayout(0, 6)).apply {
            add(JBLabel("ACH file"), BorderLayout.NORTH)
            add(JBScrollPane(fileArea), BorderLayout.CENTER)
        }
        val south = JPanel(BorderLayout(0, 6)).apply {
            add(JBLabel("Result"), BorderLayout.NORTH)
            add(JBScrollPane(resultArea), BorderLayout.CENTER)
        }

        add(buttons, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)
        add(south, BorderLayout.SOUTH)
    }

    private fun runValidation() {
        val text = fileArea.text
        if (text.isBlank()) {
            resultArea.text = "Nothing to validate — paste an ACH file or generate a sample."
            return
        }
        val problems = Nacha.validate(text)
        resultArea.text = if (problems.isEmpty()) {
            "✔ Valid NACHA file — no structural problems found."
        } else {
            buildString {
                append("✘ ${problems.size} problem(s):\n")
                problems.forEach { p ->
                    if (p.line > 0) append("  Line ${p.line}: ${p.message}\n") else append("  ${p.message}\n")
                }
            }
        }
    }

    private fun generate(valid: Boolean) {
        fileArea.text = Nacha.sampleFile(rng, valid = valid)
        resultArea.text = if (valid) {
            "Generated a spec-valid PPD sample (3 entries). Click Validate to confirm."
        } else {
            "Generated a deliberately-invalid sample (total-credit control is off by one cent) — " +
                "click Validate to see it flagged."
        }
    }

    private fun copyFile() {
        val text = fileArea.text
        if (text.isNotEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(text))
            resultArea.text = "Copied the ACH file to the clipboard."
        }
    }
}
