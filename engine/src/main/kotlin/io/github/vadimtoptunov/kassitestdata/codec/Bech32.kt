package io.github.vadimtoptunov.kassitestdata.codec

/**
 * Bech32 / Bech32m (BIP-173 / BIP-350) — the encoding of Bitcoin SegWit (bc1q…) and Taproot (bc1p…)
 * addresses: a human-readable part, a `1` separator, the data as 5-bit groups and a 6-character
 * checksum. Pure Kotlin, offline; anchored in tests to the BIP-173 / BIP-350 published test vectors.
 */
object Bech32 {

    enum class Variant { BECH32, BECH32M }

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private const val BECH32_CONST = 1
    private const val BECH32M_CONST = 0x2bc830a3

    data class Decoded(val hrp: String, val data: IntArray, val variant: Variant)

    /** A decoded SegWit address: witness version (0 for v0, 1 for Taproot) + the witness program bytes. */
    data class Segwit(val hrp: String, val witnessVersion: Int, val program: ByteArray)

    private fun polymod(values: IntArray): Int {
        val gen = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        var chk = 1
        for (v in values) {
            val top = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor v
            for (i in 0 until 5) if (((top ushr i) and 1) != 0) chk = chk xor gen[i]
        }
        return chk
    }

    private fun hrpExpand(hrp: String): IntArray {
        val out = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) out[i] = hrp[i].code ushr 5
        out[hrp.length] = 0
        for (i in hrp.indices) out[hrp.length + 1 + i] = hrp[i].code and 31
        return out
    }

    private fun constFor(v: Variant) = if (v == Variant.BECH32) BECH32_CONST else BECH32M_CONST

    private fun createChecksum(hrp: String, data: IntArray, variant: Variant): IntArray {
        val values = hrpExpand(hrp) + data + intArrayOf(0, 0, 0, 0, 0, 0)
        val mod = polymod(values) xor constFor(variant)
        return IntArray(6) { (mod ushr (5 * (5 - it))) and 31 }
    }

    /** Encode a human-readable part + 5-bit data groups into a Bech32/Bech32m string. */
    fun encode(hrp: String, data: IntArray, variant: Variant): String {
        val checksum = createChecksum(hrp, data, variant)
        val sb = StringBuilder(hrp).append('1')
        for (d in data + checksum) sb.append(CHARSET[d])
        return sb.toString()
    }

    /** Decode a Bech32/Bech32m string (validating the checksum and casing), or null if invalid. */
    fun decode(s: String): Decoded? {
        if (s.length < 8 || s.length > 90) return null
        val lower = s.lowercase()
        val upper = s.uppercase()
        if (s != lower && s != upper) return null // mixed case is invalid
        val norm = lower
        val pos = norm.lastIndexOf('1')
        if (pos < 1 || pos + 7 > norm.length) return null // need a non-empty hrp and 6-char checksum
        val hrp = norm.substring(0, pos)
        if (hrp.any { it.code < 33 || it.code > 126 }) return null
        val dataPart = norm.substring(pos + 1)
        val data = IntArray(dataPart.length)
        for (i in dataPart.indices) {
            val v = CHARSET.indexOf(dataPart[i])
            if (v < 0) return null
            data[i] = v
        }
        val values = hrpExpand(hrp) + data
        val variant = when (polymod(values)) {
            BECH32_CONST -> Variant.BECH32
            BECH32M_CONST -> Variant.BECH32M
            else -> return null
        }
        return Decoded(hrp, data.copyOf(data.size - 6), variant)
    }

    /** Convert between bit groups (e.g. 8→5 to encode bytes, 5→8 to recover them). */
    private fun convertBits(data: IntArray, from: Int, to: Int, pad: Boolean): IntArray? {
        var acc = 0
        var bits = 0
        val out = ArrayList<Int>()
        val maxv = (1 shl to) - 1
        for (value in data) {
            if (value < 0 || (value ushr from) != 0) return null
            acc = (acc shl from) or value
            bits += from
            while (bits >= to) {
                bits -= to
                out.add((acc ushr bits) and maxv)
            }
        }
        if (pad) {
            if (bits > 0) out.add((acc shl (to - bits)) and maxv)
        } else if (bits >= from || ((acc shl (to - bits)) and maxv) != 0) {
            return null
        }
        return out.toIntArray()
    }

    /** Decode a SegWit address to its witness version + program, enforcing the BIP-173/350 rules, or null. */
    fun decodeSegwit(address: String): Segwit? {
        val d = decode(address) ?: return null
        if (d.data.isEmpty()) return null
        val witver = d.data[0]
        if (witver > 16) return null
        val program = convertBits(d.data.copyOfRange(1, d.data.size), 5, 8, pad = false) ?: return null
        if (program.size < 2 || program.size > 40) return null
        if (witver == 0 && program.size != 20 && program.size != 32) return null
        // BIP-350: v0 uses Bech32, v1+ uses Bech32m.
        val expected = if (witver == 0) Variant.BECH32 else Variant.BECH32M
        if (d.variant != expected) return null
        return Segwit(d.hrp, witver, ByteArray(program.size) { program[it].toByte() })
    }

    /** Encode a witness version + program into a SegWit address under [hrp] (e.g. "bc"), or null if invalid. */
    fun encodeSegwit(hrp: String, witnessVersion: Int, program: ByteArray): String? {
        if (witnessVersion < 0 || witnessVersion > 16) return null
        if (program.size < 2 || program.size > 40) return null
        if (witnessVersion == 0 && program.size != 20 && program.size != 32) return null
        val converted = convertBits(IntArray(program.size) { program[it].toInt() and 0xFF }, 8, 5, pad = true) ?: return null
        val data = intArrayOf(witnessVersion) + converted
        val variant = if (witnessVersion == 0) Variant.BECH32 else Variant.BECH32M
        val addr = encode(hrp, data, variant)
        return if (decodeSegwit(addr) != null) addr else null
    }
}
