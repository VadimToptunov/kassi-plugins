package io.github.vadimtoptunov.codectoolkit

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import io.github.vadimtoptunov.kassitestdata.codec.Bech32
import io.github.vadimtoptunov.kassitestdata.codec.Codec
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/**
 * Convert a value between Text / Hex / Base58 / Base58Check / Base32 — decode the input (per the "From"
 * format) to raw bytes, then re-encode to the "To" format. All logic is in the shared, unit-tested
 * [Codec]; this class is only Swing wiring.
 */
class CodecToolkitPanel : JPanel(BorderLayout()) {

    /** A representation that can decode a string to bytes and encode bytes back to a string. */
    private enum class Format(private val label: String) {
        TEXT("Text") {
            override fun decode(s: String) = s.toByteArray(Charsets.UTF_8)
            override fun encode(b: ByteArray) = String(b, Charsets.UTF_8)
        },
        HEX("Hex") {
            override fun decode(s: String) = Codec.hexDecode(s)
            override fun encode(b: ByteArray) = Codec.hexEncode(b)
        },
        BASE58("Base58") {
            override fun decode(s: String) = Codec.base58Decode(s.trim())
            override fun encode(b: ByteArray) = Codec.base58Encode(b)
        },
        BASE58CHECK("Base58Check") {
            override fun decode(s: String) = Codec.base58CheckDecode(s.trim())
            override fun encode(b: ByteArray) = Codec.base58CheckEncode(b)
        },
        BASE32("Base32") {
            override fun decode(s: String) = Codec.base32Decode(s.trim())
            override fun encode(b: ByteArray) = Codec.base32Encode(b)
        };

        abstract fun decode(s: String): ByteArray?
        abstract fun encode(b: ByteArray): String
        override fun toString() = label
    }

    private val input = JBTextArea(6, 60)
    private val output = JBTextArea(6, 60).apply { isEditable = false }
    private val fromCombo = ComboBox(Format.entries.toTypedArray()).apply { selectedItem = Format.HEX }
    private val toCombo = ComboBox(Format.entries.toTypedArray()).apply { selectedItem = Format.BASE58 }

    init {
        border = JBUI.Borders.empty(8)

        val controls = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JBLabel("From"))
            add(fromCombo)
            add(JBLabel("To"))
            add(toCombo)
            add(JButton("Decode SegWit / Bech32").apply { addActionListener { decodeBech32() } })
            add(JButton("Copy").apply { addActionListener { copyOutput() } })
        }

        val center = JPanel(BorderLayout(0, 6)).apply {
            add(JBLabel("Input"), BorderLayout.NORTH)
            add(JBScrollPane(input), BorderLayout.CENTER)
        }
        val south = JPanel(BorderLayout(0, 6)).apply {
            add(JBLabel("Output"), BorderLayout.NORTH)
            add(JBScrollPane(output), BorderLayout.CENTER)
        }

        add(controls, BorderLayout.NORTH)
        add(center, BorderLayout.CENTER)
        add(south, BorderLayout.SOUTH)

        input.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = convert()
        })
        fromCombo.addActionListener { convert() }
        toCombo.addActionListener { convert() }
    }

    private fun convert() {
        val text = input.text
        if (text.isBlank()) {
            output.text = ""
            return
        }
        val from = fromCombo.selectedItem as Format
        val to = toCombo.selectedItem as Format
        val bytes = from.decode(text)
        output.text = if (bytes == null) {
            "⚠ Not valid $from input."
        } else {
            to.encode(bytes)
        }
    }

    /** Decode the Input as a Bech32/Bech32m string, breaking out a SegWit address's parts when it is one. */
    private fun decodeBech32() {
        val s = input.text.trim()
        if (s.isEmpty()) {
            output.text = ""
            return
        }
        val seg = Bech32.decodeSegwit(s)
        if (seg != null) {
            output.text = buildString {
                append("SegWit address\n")
                append("HRP: ${seg.hrp}\n")
                append("Witness version: ${seg.witnessVersion}\n")
                append("Program (hex): ${Codec.hexEncode(seg.program)}\n")
                append("Encoding: ${if (seg.witnessVersion == 0) "Bech32" else "Bech32m"}")
            }
            return
        }
        val d = Bech32.decode(s)
        output.text = if (d != null) {
            "Bech32 string\nHRP: ${d.hrp}\nData: ${d.data.size} 5-bit group(s)\nEncoding: ${if (d.variant == Bech32.Variant.BECH32) "Bech32" else "Bech32m"}"
        } else {
            "⚠ Not a valid Bech32 / SegWit string."
        }
    }

    private fun copyOutput() {
        val text = output.text
        if (text.isNotEmpty() && !text.startsWith("⚠")) {
            CopyPasteManager.getInstance().setContents(StringSelection(text))
        }
    }
}
