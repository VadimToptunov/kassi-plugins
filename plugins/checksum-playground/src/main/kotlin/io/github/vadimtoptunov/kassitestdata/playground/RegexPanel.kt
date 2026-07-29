package io.github.vadimtoptunov.kassitestdata.playground

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.regex.PatternSyntaxException
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/** A small live regex tester: pattern + test text → matches (with capture groups). */
class RegexPanel : JPanel(BorderLayout()) {

    private val pattern = JBTextField()
    private val testText = JBTextArea(6, 40).apply { lineWrap = true; wrapStyleWord = true }
    private val output = JBTextArea().apply { isEditable = false; lineWrap = true }

    init {
        border = JBUI.Borders.empty(8)

        val north = JPanel(BorderLayout()).apply {
            add(JBLabel("Regex pattern:"), BorderLayout.NORTH)
            add(pattern, BorderLayout.CENTER)
        }
        val center = JPanel(GridLayout(2, 1, 0, 8)).apply {
            add(labeled("Test text:", JBScrollPane(testText)))
            add(labeled("Matches:", JBScrollPane(output)))
        }
        add(north, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)

        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = refresh()
        }
        pattern.document.addDocumentListener(listener)
        testText.document.addDocumentListener(listener)
        refresh()
    }

    private fun labeled(title: String, component: JComponent): JPanel =
        JPanel(BorderLayout()).apply {
            add(JBLabel(title), BorderLayout.NORTH)
            add(component, BorderLayout.CENTER)
        }

    private fun refresh() {
        val patternText = pattern.text
        if (patternText.isEmpty()) {
            output.foreground = JBColor.GRAY
            output.text = "Enter a pattern above."
            return
        }
        val regex = try {
            Regex(patternText)
        } catch (e: PatternSyntaxException) {
            output.foreground = JBColor(0xC62828, 0xEF5350)
            output.text = "Invalid pattern: ${e.description}"
            return
        }
        val matches = regex.findAll(testText.text).toList()
        output.foreground = JBColor.foreground()
        output.text = if (matches.isEmpty()) {
            "No matches."
        } else {
            buildString {
                appendLine("${matches.size} match(es):")
                matches.forEachIndexed { i, m ->
                    appendLine("[${i + 1}] \"${m.value}\" @ ${m.range.first}..${m.range.last}")
                    if (m.groupValues.size > 1) {
                        m.groupValues.drop(1).forEachIndexed { g, gv ->
                            appendLine("      group ${g + 1}: \"$gv\"")
                        }
                    }
                }
            }
        }
    }
}
