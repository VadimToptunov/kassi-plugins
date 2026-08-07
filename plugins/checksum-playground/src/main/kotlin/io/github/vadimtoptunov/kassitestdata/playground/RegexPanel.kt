package io.github.vadimtoptunov.kassitestdata.playground

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.util.regex.PatternSyntaxException
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/** A live regex tester: pattern (+ flags, ready-made library) → matches with groups, plus regex replace. */
class RegexPanel : JPanel(BorderLayout()) {

    private val pattern = JBTextField()
    private val replacement = JBTextField()
    private val testText = JBTextArea(6, 40).apply { lineWrap = true; wrapStyleWord = true }
    private val matchesOut = JBTextArea().apply { isEditable = false; lineWrap = true }
    private val replaceOut = JBTextArea().apply { isEditable = false; lineWrap = true }

    private val ignoreCase = JBCheckBox("Ignore case")
    private val multiline = JBCheckBox("Multiline (^ \$)")
    private val dotAll = JBCheckBox("Dot matches newline")
    private val comments = JBCheckBox("Extended (ignore whitespace)")

    init {
        border = JBUI.Borders.empty(8)

        val library = ComboBox<String>().apply {
            addItem(PLACEHOLDER)
            RegexLibrary.PATTERNS.forEach { addItem(it.first) }
            addActionListener {
                val i = selectedIndex
                if (i > 0) {
                    pattern.text = RegexLibrary.PATTERNS[i - 1].second
                    selectedIndex = 0 // reset so the same entry can be picked again
                }
            }
        }

        val patternRow = JPanel(BorderLayout(6, 0)).apply {
            add(library, BorderLayout.WEST)
            add(pattern, BorderLayout.CENTER)
        }
        val flagsRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(ignoreCase); add(multiline); add(dotAll); add(comments)
        }
        val north = JPanel(BorderLayout(0, 4)).apply {
            add(labeled("Regex pattern (pick a ready-made one or type your own):", patternRow), BorderLayout.NORTH)
            add(flagsRow, BorderLayout.CENTER)
            add(labeled("Replace with (\$1 for groups; leave empty to only match):", replacement), BorderLayout.SOUTH)
        }

        val center = JPanel(GridLayout(3, 1, 0, 8)).apply {
            add(labeled("Test text:", JBScrollPane(testText)))
            add(labeled("Matches:", JBScrollPane(matchesOut)))
            add(labeled("Replace result:", JBScrollPane(replaceOut)))
        }
        add(north, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)

        val docListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = refresh()
        }
        pattern.document.addDocumentListener(docListener)
        replacement.document.addDocumentListener(docListener)
        testText.document.addDocumentListener(docListener)
        listOf(ignoreCase, multiline, dotAll, comments).forEach { it.addActionListener { refresh() } }
        refresh()
    }

    private fun labeled(title: String, component: JComponent): JPanel =
        JPanel(BorderLayout()).apply {
            add(JBLabel(title), BorderLayout.NORTH)
            add(component, BorderLayout.CENTER)
        }

    private fun selectedOptions(): Set<RegexOption> = buildSet {
        if (ignoreCase.isSelected) add(RegexOption.IGNORE_CASE)
        if (multiline.isSelected) add(RegexOption.MULTILINE)
        if (dotAll.isSelected) add(RegexOption.DOT_MATCHES_ALL)
        if (comments.isSelected) add(RegexOption.COMMENTS)
    }

    private fun refresh() {
        val patternText = pattern.text
        if (patternText.isEmpty()) {
            matchesOut.foreground = JBColor.GRAY
            matchesOut.text = "Enter a pattern above."
            replaceOut.text = ""
            return
        }
        val regex = try {
            Regex(patternText, selectedOptions())
        } catch (e: PatternSyntaxException) {
            matchesOut.foreground = JBColor(0xC62828, 0xEF5350)
            matchesOut.text = "Invalid pattern: ${e.description}"
            replaceOut.text = ""
            return
        }
        val matches = regex.findAll(testText.text).toList()
        matchesOut.foreground = JBColor.foreground()
        matchesOut.text = if (matches.isEmpty()) {
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
        replaceOut.text = if (replacement.text.isEmpty()) {
            ""
        } else {
            try {
                regex.replace(testText.text, replacement.text)
            } catch (e: RuntimeException) {
                // e.g. a $N group reference that doesn't exist in the pattern.
                "Invalid replacement: ${e.message}"
            }
        }
    }

    private companion object {
        const val PLACEHOLDER = "Load a pattern…"
    }
}
