package io.github.vadimtoptunov.uuidtoolkit

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.generators.IdToolkit
import io.github.vadimtoptunov.kassitestdata.generators.UuidFormat
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import java.time.Instant
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.DocumentEvent

/** Generate and inspect UUID (v4/v7), ULID and NanoID — offline, using the shared engine. */
class UuidToolkitPanel : JPanel(BorderLayout()) {

    private val rng = Rng() // unseeded: fresh values on each click
    private val output = JBTextArea(8, 40).apply { isEditable = false }
    private val inspectInput = JBTextField()
    private val inspectResult = JBLabel(" ")

    // Bulk generation: how many to emit per click, and how to format UUID output.
    private val countSpinner = JSpinner(SpinnerNumberModel(1, 1, 1000, 1))
    private val formatCombo = ComboBox(UuidFormat.Style.entries.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create("") { it.label }
    }

    private val okColor = JBColor(0x2E7D32, 0x66BB6A)
    private val failColor = JBColor(0xC62828, 0xEF5350)

    private companion object {
        const val TWITTER_EPOCH_MS = 1_288_834_974_657L // Snowflake default epoch
        const val DISCORD_EPOCH_MS = 1_420_070_400_000L // Discord's Snowflake epoch (2015-01-01)
    }

    init {
        border = JBUI.Borders.empty(8)

        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(uuidButton("UUID v1") { IdToolkit.uuidV1(rng, System.currentTimeMillis()) })
            add(uuidButton("UUID v4") { IdToolkit.uuidV4(rng) })
            add(uuidButton("UUID v6") { IdToolkit.uuidV6(rng, System.currentTimeMillis()) })
            add(uuidButton("UUID v7") { IdToolkit.uuidV7(rng, System.currentTimeMillis()) })
            add(plainButton("ULID") { IdToolkit.ulid(rng, System.currentTimeMillis()) })
            add(plainButton("NanoID") { IdToolkit.nanoId(rng) })
        }

        // Bulk count + UUID format, and output actions.
        val bulkRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(JBLabel("Count:"))
            add(countSpinner)
            add(JBLabel("UUID format:"))
            add(formatCombo)
            add(JButton("Copy all").apply { addActionListener { copyAll() } })
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

        // TypeID (jetify): a type prefix + a UUID v7 encoded in lowercase Crockford base32.
        val typeIdPrefix = JBTextField(12).apply { text = "user" }
        val typeIdRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(JBLabel("TypeID prefix:"))
            add(typeIdPrefix)
            add(JButton("TypeID").apply {
                addActionListener { output.append(IdToolkit.typeId(typeIdPrefix.text.trim(), rng, System.currentTimeMillis()) + "\n") }
            })
        }
        val controls = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(buttons)
            add(bulkRow)
            add(nsRow)
            add(typeIdRow)
        }

        val top = JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel("Generate (click to append; select text to copy):"), BorderLayout.NORTH)
            add(controls, BorderLayout.CENTER)
            add(JBScrollPane(output), BorderLayout.SOUTH)
        }

        val bottom = JPanel(BorderLayout(0, 4)).apply {
            add(labeled("Inspect — paste a UUID / ULID / TypeID / NanoID:", inspectInput), BorderLayout.NORTH)
            add(inspectResult, BorderLayout.CENTER)
        }

        add(top, BorderLayout.NORTH)
        add(bottom, BorderLayout.CENTER)

        inspectInput.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = inspect()
        })
        inspect()
    }

    /** A UUID generator button: emits [count] values, each rendered in the selected [UuidFormat.Style]. */
    private fun uuidButton(label: String, produce: () -> String): JButton =
        JButton(label).apply {
            addActionListener {
                val style = formatCombo.item
                repeat(count()) { output.append(UuidFormat.format(produce(), style) + "\n") }
            }
        }

    /** A non-UUID generator button (ULID/NanoID): emits [count] values verbatim (no UUID format). */
    private fun plainButton(label: String, produce: () -> String): JButton =
        JButton(label).apply {
            addActionListener { repeat(count()) { output.append(produce() + "\n") } }
        }

    private fun count(): Int = countSpinner.value as Int

    private fun copyAll() {
        val text = output.text
        if (text.isNotEmpty()) CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

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
        val ksuidTime = IdToolkit.ksuidTimestampSeconds(text)
        // A prefixed value (contains '_') is tried as a TypeID; a bare 26-char base32 string stays a ULID.
        val typeId = if (text.contains('_')) IdToolkit.parseTypeId(text) else null
        // A 15–19 digit value is decoded as a Snowflake ID against the Twitter epoch by default.
        val snowflakeId = if (text.length in 15..19 && text.all { it.isDigit() }) text.toLongOrNull() else null
        val ok = uuid != null || typeId != null || IdToolkit.isValidUlid(text) || ksuidTime != null ||
            snowflakeId != null || IdToolkit.isValidNanoId(text)
        inspectResult.foreground = if (ok) okColor else failColor
        inspectResult.text = when {
            uuid != null -> buildString {
                append("UUID v${uuid.version} · variant ${uuid.variant}")
                uuid.timestampMillis?.let { append(" · time ${Instant.ofEpochMilli(it)}") }
                uuid.node?.let { append(" · node %012x".format(it)) }
                uuid.clockSeq?.let { append(" · clock-seq $it") }
            }
            typeId != null ->
                "TypeID · prefix \"${typeId.prefix}\" · UUID ${typeId.uuid}"
            IdToolkit.isValidUlid(text) ->
                "ULID · time ${Instant.ofEpochMilli(IdToolkit.ulidTimestampMillis(text)!!)}"
            ksuidTime != null -> "KSUID · time ${Instant.ofEpochSecond(ksuidTime)}"
            snowflakeId != null -> {
                val tw = IdToolkit.snowflakeInfo(snowflakeId, TWITTER_EPOCH_MS)
                val dc = IdToolkit.snowflakeInfo(snowflakeId, DISCORD_EPOCH_MS)
                "Snowflake · Twitter epoch ${Instant.ofEpochMilli(tw.timestampMillis)} · Discord epoch " +
                    "${Instant.ofEpochMilli(dc.timestampMillis)} · dc ${tw.datacenterId} worker ${tw.workerId} seq ${tw.sequence}"
            }
            IdToolkit.isValidNanoId(text) -> "NanoID · ${text.length} chars (URL-safe alphabet)"
            else -> "Not a recognized UUID / ULID / TypeID / NanoID."
        }
    }
}
