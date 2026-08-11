package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.core.Rng
import java.security.MessageDigest

/**
 * UUID / ULID / NanoID generation and inspection. Unlike the checksum identifiers, these carry no
 * check digit — their verifiable structure is the version/variant bits (UUID) and the embedded
 * 48-bit millisecond timestamp (UUID v7, ULID). The tests anchor to published reference values
 * (RFC 9562 for UUID v7, the ULID spec's documented min/max timestamps) rather than round-tripping
 * our own output.
 */
object IdToolkit {

    // ---------------------------------------------------------------- UUID

    private fun randomBytes(rng: Rng, n: Int) = ByteArray(n) { rng.int(256).toByte() }

    private fun formatUuid(b: ByteArray): String {
        val hex = b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    }

    /** Random UUID version 4 (RFC 9562), variant 10xx. */
    fun uuidV4(rng: Rng): String {
        val b = randomBytes(rng, 16)
        b[6] = ((b[6].toInt() and 0x0F) or 0x40).toByte() // version 4
        b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte() // variant 10xx
        return formatUuid(b)
    }

    /** Time-ordered UUID version 7 (RFC 9562): 48-bit big-endian unix-ms prefix + random tail. */
    fun uuidV7(rng: Rng, unixMillis: Long): String {
        val b = randomBytes(rng, 16)
        for (i in 0 until 6) b[i] = ((unixMillis shr (8 * (5 - i))) and 0xFF).toByte()
        b[6] = ((b[6].toInt() and 0x0F) or 0x70).toByte() // version 7
        b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte() // variant 10xx
        return formatUuid(b)
    }

    data class UuidInfo(val version: Int, val variant: String, val timestampMillis: Long?)

    private val UUID_RE =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /** Parse a canonical UUID into version, variant and (for v7) its embedded timestamp — or null. */
    fun inspectUuid(value: String): UuidInfo? {
        val s = value.trim()
        if (!UUID_RE.matches(s)) return null
        val hex = s.replace("-", "")
        val version = Character.digit(hex[12], 16)
        val variantNibble = Character.digit(hex[16], 16)
        val variant = when {
            variantNibble and 0x8 == 0 -> "NCS (0xx)"
            variantNibble and 0xC == 0x8 -> "RFC 4122 (10x)"
            variantNibble and 0xE == 0xC -> "Microsoft (110x)"
            else -> "reserved (111x)"
        }
        val ts = if (version == 7) hex.substring(0, 12).toLong(16) else null
        return UuidInfo(version, variant, ts)
    }

    // ---------------------------------------------------------------- ULID

    // Crockford's base32 — the digits/letters, excluding I, L, O and U.
    private const val CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    /** A 26-char ULID: 48-bit ms timestamp (10 chars) + 80 bits of randomness (16 chars). */
    fun ulid(rng: Rng, unixMillis: Long): String = buildString {
        var ts = unixMillis and 0xFFFFFFFFFFFFL // 48 bits
        val tchars = CharArray(10)
        for (i in 9 downTo 0) {
            tchars[i] = CROCKFORD[(ts and 0x1F).toInt()]
            ts = ts shr 5
        }
        append(tchars)
        repeat(16) { append(CROCKFORD[rng.int(32)]) }
    }

    // Crockford decode is case-insensitive and forgives the I/L→1, O→0 look-alikes.
    private fun crockfordValue(c: Char): Int = when (val u = c.uppercaseChar()) {
        'I', 'L' -> 1
        'O' -> 0
        else -> CROCKFORD.indexOf(u)
    }

    fun isValidUlid(value: String): Boolean {
        val s = value.trim()
        return s.length == 26 && s.all { crockfordValue(it) >= 0 }
    }

    /** Decode a ULID's 48-bit timestamp (ms since the epoch), or null if it isn't a valid ULID. */
    fun ulidTimestampMillis(value: String): Long? {
        if (!isValidUlid(value)) return null
        val s = value.trim()
        var ts = 0L
        for (i in 0 until 10) ts = (ts shl 5) or crockfordValue(s[i]).toLong()
        return ts
    }

    // -------------------------------------------------------------- NanoID

    private const val NANOID_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-"

    /** A NanoID of [size] characters from the default URL-safe alphabet (21 is the library default). */
    fun nanoId(rng: Rng, size: Int = 21): String =
        buildString { repeat(size) { append(NANOID_ALPHABET[rng.int(NANOID_ALPHABET.length)]) } }

    /** NanoID has no checksum; validity here is: non-empty and drawn from the URL-safe alphabet. */
    fun isValidNanoId(value: String): Boolean = value.isNotEmpty() && value.all { it in NANOID_ALPHABET }

    // ------------------------------------------------- Namespace UUID (v5 SHA-1, v3 MD5)

    /** The predefined RFC 4122 / 9562 namespaces for name-based UUIDs. */
    enum class Namespace(val uuid: String) {
        DNS("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
        URL("6ba7b811-9dad-11d1-80b4-00c04fd430c8"),
        OID("6ba7b812-9dad-11d1-80b4-00c04fd430c8"),
        X500("6ba7b814-9dad-11d1-80b4-00c04fd430c8"),
    }

    private fun uuidBytes(uuid: String): ByteArray {
        val hex = uuid.replace("-", "")
        return ByteArray(16) { ((Character.digit(hex[it * 2], 16) shl 4) or Character.digit(hex[it * 2 + 1], 16)).toByte() }
    }

    private fun nameBased(namespace: Namespace, name: String, algorithm: String, version: Int): String {
        val digest = MessageDigest.getInstance(algorithm)
            .digest(uuidBytes(namespace.uuid) + name.toByteArray(Charsets.UTF_8))
        val b = digest.copyOf(16)
        b[6] = ((b[6].toInt() and 0x0F) or (version shl 4)).toByte()
        b[8] = ((b[8].toInt() and 0x3F) or 0x80).toByte() // RFC 4122 variant
        val hex = b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    }

    /** Deterministic name-based UUID version 5 (SHA-1 of namespace + name). */
    fun uuidV5(namespace: Namespace, name: String): String = nameBased(namespace, name, "SHA-1", 5)

    /** Deterministic name-based UUID version 3 (MD5 of namespace + name). */
    fun uuidV3(namespace: Namespace, name: String): String = nameBased(namespace, name, "MD5", 3)
}
