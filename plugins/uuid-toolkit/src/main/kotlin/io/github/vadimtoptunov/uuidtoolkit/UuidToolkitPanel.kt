package io.github.vadimtoptunov.uuidtoolkit

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.IdToolkit
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.time.Instant
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/** Generate and inspect UUID (v4/v7), ULID and NanoID — offline, using the shared engine. */
class UuidToolkitPanel : JPanel(BorderLayout()) {

    private val rng = Rng() // unseeded: fresh values on each click
    private val output = JBTextArea(8, 40).apply { isEditable = false }
    private val inspectInput = JBTextField()
    private val inspectResult = JBLabel(" ")

    private val okColor = JBColor(0x2E7D32, 0x66BB6A)
    private val failColor = JBColor(0xC62828, 0xEF5350)

    init {
        border = JBUI.Borders.empty(8)

        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(genButton("UUID v4") { IdToolkit.uuidV4(rng) })
            add(genButton("UUID v6") { IdToolkit.uuidV6(rng, System.currentTimeMillis()) })
            add(genButton("UUID v7") { IdToolkit.uuidV7(rng, System.currentTimeMillis()) })
            add(genButton("ULID") { IdToolkit.ulid(rng, System.currentTimeMillis()) })
            add(genButton("NanoID") { IdToolkit.nanoId(rng) })
            add(JButton("Clear").apply { addActionListener { output.text = "" } })
        }

        // Name-based UUIDs (v5 SHA-1 / v3 MD5): deterministic from a namespace + name.
        val nsPicker = ComboBox(IdToolkit.Namespace.entries.toTypedArray())
        val nameField = JBTextField(16)
        fun nameBased(v: (IdToolkit.Namespace, String) -> String) {
            val name = nameField.text
            if (name.isNotEmpty()) output.append(v(nsPicker.item, name) + "\n")
        }
        val nsRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(JBLabel("Name-based:"))
            add(nsPicker)
            add(nameField)
            add(JButton("v5").apply { addActionListener { nameBased(IdToolkit::uuidV5) } })
            add(JButton("v3").apply { addActionListener { nameBased(IdToolkit::uuidV3) } })
        }
        val controls = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(buttons)
            add(nsRow)
        }

        val top = JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel("Generate (click to append; select text to copy):"), BorderLayout.NORTH)
            add(controls, BorderLayout.CENTER)
            add(JBScrollPane(output), BorderLayout.SOUTH)
        }

        val bottom = JPanel(BorderLayout(0, 4)).apply {
            add(labeled("Inspect — paste a UUID / ULID / NanoID:", inspectInput), BorderLayout.NORTH)
            add(inspectResult, BorderLayout.CENTER)
        }

        add(top, BorderLayout.NORTH)
        add(bottom, BorderLayout.CENTER)

        inspectInput.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = inspect()
        })
        inspect()
    }

    private fun genButton(label: String, produce: () -> String): JButton =
        JButton(label).apply { addActionListener { output.append(produce() + "\n") } }

    private fun labeled(title: String, component: JComponent): JPanel =
        JPanel(BorderLayout(0, 2)).apply {
            add(JBLabel(title), BorderLayout.NORTH)
            add(component, BorderLayout.CENTER)
        }

    private fun inspect() {
        val text = inspectInput.text.trim()
        if (text.isEmpty()) {
            inspectResult.foreground = JBColor.GRAY
            inspectResult.text = "Paste a value above to identify it."
            return
        }
        val uuid = IdToolkit.inspectUuid(text)
        val ok = uuid != null || IdToolkit.isValidUlid(text) || IdToolkit.isValidNanoId(text)
        inspectResult.foreground = if (ok) okColor else failColor
        inspectResult.text = when {
            uuid != null -> buildString {
                append("UUID v${uuid.version} · variant ${uuid.variant}")
                uuid.timestampMillis?.let { append(" · time ${Instant.ofEpochMilli(it)}") }
            }
            IdToolkit.isValidUlid(text) ->
                "ULID · time ${Instant.ofEpochMilli(IdToolkit.ulidTimestampMillis(text)!!)}"
            IdToolkit.isValidNanoId(text) -> "NanoID · ${text.length} chars (URL-safe alphabet)"
            else -> "Not a recognized UUID / ULID / NanoID."
        }
    }
}
