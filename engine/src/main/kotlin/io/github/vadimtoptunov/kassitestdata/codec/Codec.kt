package io.github.vadimtoptunov.kassitestdata.codec

import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Offline encoders/decoders for the crypto/fintech representations that general "string manipulation"
 * plugins do not cover: Base58 and Base58Check (Bitcoin), Base32 (RFC 4648) and hex. Pure Kotlin, no
 * dependency, deterministic — so a wallet payload, an address or a key never has to be pasted into a
 * web codec. Anchored in tests to published vectors (RFC 4648 §10 and the Bitcoin wiki Base58Check
 * worked example).
 */
object Codec {

    private const val BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" // RFC 4648

    private fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)
    private fun doubleSha256(b: ByteArray): ByteArray = sha256(sha256(b))

    // ---- Base58 (Bitcoin alphabet) -------------------------------------------------------------

    fun base58Encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val b = input.copyOf()
        var zeros = 0
        while (zeros < b.size && b[zeros].toInt() == 0) zeros++
        val encoded = StringBuilder()
        var start = zeros
        while (start < b.size) {
            var remainder = 0
            for (i in start until b.size) {
                val num = (remainder shl 8) + (b[i].toInt() and 0xff)
                b[i] = (num / 58).toByte()
                remainder = num % 58
            }
            encoded.append(BASE58[remainder])
            if (b[start].toInt() == 0) start++
        }
        repeat(zeros) { encoded.append(BASE58[0]) } // leading zero bytes -> '1'
        return encoded.reverse().toString()
    }

    /** Decode a Base58 string, or null if it contains a non-Base58 character. */
    fun base58Decode(s: String): ByteArray? {
        if (s.isEmpty()) return ByteArray(0)
        val input58 = IntArray(s.length)
        for (i in s.indices) {
            val idx = BASE58.indexOf(s[i])
            if (idx < 0) return null
            input58[i] = idx
        }
        var zeros = 0
        while (zeros < input58.size && input58[zeros] == 0) zeros++
        val decoded = ByteArray(s.length)
        var outputStart = decoded.size
        var start = zeros
        while (start < input58.size) {
            var remainder = 0
            for (i in start until input58.size) {
                val num = remainder * 58 + input58[i]
                input58[i] = num / 256
                remainder = num % 256
            }
            decoded[--outputStart] = remainder.toByte()
            if (input58[start] == 0) start++
        }
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) outputStart++
        val result = ByteArray(zeros + (decoded.size - outputStart))
        System.arraycopy(decoded, outputStart, result, zeros, decoded.size - outputStart)
        return result
    }

    // ---- Base58Check (version byte(s) + payload + 4-byte double-SHA-256 checksum) ---------------

    fun base58CheckEncode(payload: ByteArray): String =
        base58Encode(payload + doubleSha256(payload).copyOf(4))

    /** Decode a Base58Check string to its payload (version + data, without the checksum), or null if
     *  the string is not Base58, is too short, or the 4-byte checksum does not verify. */
    fun base58CheckDecode(s: String): ByteArray? {
        val raw = base58Decode(s) ?: return null
        if (raw.size < 4) return null // needs at least the 4-byte checksum (payload may be empty)
        val payload = raw.copyOf(raw.size - 4)
        val checksum = raw.copyOfRange(raw.size - 4, raw.size)
        return if (checksum.contentEquals(doubleSha256(payload).copyOf(4))) payload else null
    }

    // ---- Base32 (RFC 4648) ---------------------------------------------------------------------

    fun base32Encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                sb.append(BASE32[(buffer ushr (bits - 5)) and 0x1f])
                bits -= 5
            }
        }
        if (bits > 0) sb.append(BASE32[(buffer shl (5 - bits)) and 0x1f])
        while (sb.length % 8 != 0) sb.append('=')
        return sb.toString()
    }

    /** Decode a Base32 (RFC 4648) string, padding optional; null on an out-of-alphabet character. */
    fun base32Decode(s: String): ByteArray? {
        val clean = s.trimEnd('=').uppercase()
        if (clean.isEmpty()) return ByteArray(0)
        val out = ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (c in clean) {
            val idx = BASE32.indexOf(c)
            if (idx < 0) return null
            buffer = (buffer shl 5) or idx
            bits += 5
            if (bits >= 8) {
                out.write((buffer ushr (bits - 8)) and 0xff)
                bits -= 8
            }
        }
        return out.toByteArray()
    }

    // ---- Hex -----------------------------------------------------------------------------------

    fun hexEncode(data: ByteArray): String = data.joinToString("") { "%02x".format(it) }

    /** Decode a hex string (spaces/newlines ignored), or null if odd-length or non-hex. */
    fun hexDecode(s: String): ByteArray? {
        val clean = s.filterNot { it == ' ' || it == '\n' || it == '\r' || it == '\t' }
        if (clean.length % 2 != 0) return null
        if (!clean.all { it in "0123456789abcdefABCDEF" }) return null
        return ByteArray(clean.length / 2) {
            ((Character.digit(clean[it * 2], 16) shl 4) + Character.digit(clean[it * 2 + 1], 16)).toByte()
        }
    }
}
