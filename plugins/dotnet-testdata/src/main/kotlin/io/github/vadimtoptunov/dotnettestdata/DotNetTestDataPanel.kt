package io.github.vadimtoptunov.dotnettestdata

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.codegen.DotNetFakerGen
import io.github.vadimtoptunov.kassitestdata.codegen.GoTableTestGen
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Generates an idiomatic C# xUnit `[Theory]` test whose `[InlineData]` rows are spec-valid values (and
 * their invalid variants) from the shared engine — fixtures that pass the real checksum, unlike a
 * hand-rolled Bogus faker. Pick a data kind, Generate, then Copy the C# into your test project.
 */
class DotNetTestDataPanel : JPanel(BorderLayout()) {

    private val kindPicker = ComboBox(GoTableTestGen.Kind.entries.toTypedArray())
    private val output = JBTextArea(18, 60).apply { isEditable = false }

    init {
        border = JBUI.Borders.empty(8)
        val top = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(JBLabel("Kind:"))
            add(kindPicker)
            add(JButton("Generate").apply { addActionListener { generate() } })
            add(JButton("Copy").apply { addActionListener { copy() } })
        }
        add(top, BorderLayout.NORTH)
        add(JBScrollPane(output), BorderLayout.CENTER)
        generate()
    }

    private fun generate() {
        output.text = DotNetFakerGen.generate(kindPicker.item)
        output.caretPosition = 0
    }

    private fun copy() {
        if (output.text.isNotEmpty()) CopyPasteManager.getInstance().setContents(StringSelection(output.text))
    }
}
