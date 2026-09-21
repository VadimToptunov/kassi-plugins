package io.github.vadimtoptunov.leakguard.cli

/**
 * A dependency-free SARIF 2.1.0 writer. It emits the envelope, the `tool.driver` block with one
 * `reportingDescriptor` per finding kind, and one `result` per finding — carrying the
 * line-independent [RunResult.fingerprint] under `partialFingerprints["leakGuard/v1"]` and the
 * `baselineState`. The matched secret is never written to the report (only its location and kind).
 *
 * The JSON is produced by a tiny internal serializer over Kotlin maps/lists so the output is always
 * well-formed and correctly escaped — no external JSON library on the classpath.
 */

/** One SARIF result row, fully resolved by the runner (level, location, fingerprint, baseline). */
data class RunResult(
    val ruleId: String,
    val level: String,
    val message: String,
    val uri: String,
    val startLine: Int,
    val startColumn: Int,
    val fingerprint: String,
    val baselineState: String, // "new" | "unchanged"
)

/** Static metadata for a kind's SARIF `reportingDescriptor`. */
data class SarifRuleDescriptor(
    val id: String,
    val name: String,
    val shortDescription: String,
    val fullDescription: String,
    val level: String,
)

/** The kinds this runner knows about, in the order they appear in `tool.driver.rules` (index = ruleIndex). */
val SARIF_RULES: List<SarifRuleDescriptor> = listOf(
    SarifRuleDescriptor(
        id = "CARD_PAN",
        name = "CardNumber",
        shortDescription = "Real-looking card number (PAN)",
        fullDescription = "A Luhn-valid card number whose BIN is outside every known reserved test-BIN " +
            "range — either a real PAN or one that will be mistaken for one. Replace it with a reserved " +
            "test card number.",
        level = "warning",
    ),
    SarifRuleDescriptor(
        id = "IBAN",
        name = "Iban",
        shortDescription = "Real-looking IBAN",
        fullDescription = "A mod-97-valid IBAN outside the reserved documentation ranges. Replace it with " +
            "a reserved test IBAN.",
        level = "warning",
    ),
    SarifRuleDescriptor(
        id = "US_SSN",
        name = "UsSsn",
        shortDescription = "Real-looking US Social Security Number",
        fullDescription = "A US SSN in an area/group the SSA can actually have issued (not a reserved " +
            "never-issued range). Replace it with a reserved test SSN.",
        level = "warning",
    ),
    SarifRuleDescriptor(
        id = "AWS_ACCESS_KEY",
        name = "AwsAccessKeyId",
        shortDescription = "AWS access key ID",
        fullDescription = "An AWS access key ID (AKIA/ASIA + 16 upper-alphanumerics). Rotate the key and " +
            "remove it from the source; use an environment variable or secret store instead.",
        level = "error",
    ),
    SarifRuleDescriptor(
        id = "JWT",
        name = "Jwt",
        shortDescription = "JSON Web Token",
        fullDescription = "A JWT (three base64url segments, header starting `eyJ`). Tokens are bearer " +
            "credentials — remove it and issue a fresh one out of band.",
        level = "error",
    ),
    SarifRuleDescriptor(
        id = "PRIVATE_KEY",
        name = "PrivateKey",
        shortDescription = "Private key block",
        fullDescription = "A PEM private-key block header (`-----BEGIN … PRIVATE KEY-----`). Remove the " +
            "key material from the repository and rotate it.",
        level = "error",
    ),
)

private val RULE_INDEX: Map<String, Int> = SARIF_RULES.withIndex().associate { (i, r) -> r.id to i }

const val LEAK_GUARD_INFORMATION_URI = "https://plugins.jetbrains.com/plugin/leak-guard"

/** Renders the [results] as a SARIF 2.1.0 document string. */
fun toSarif(driverVersion: String, results: List<RunResult>): String {
    val ruleObjects = SARIF_RULES.map { r ->
        linkedMapOf<String, Any?>(
            "id" to r.id,
            "name" to r.name,
            "shortDescription" to linkedMapOf("text" to r.shortDescription),
            "fullDescription" to linkedMapOf("text" to r.fullDescription),
            "defaultConfiguration" to linkedMapOf("level" to r.level),
        )
    }

    val resultObjects = results.map { res ->
        linkedMapOf<String, Any?>(
            "ruleId" to res.ruleId,
            "ruleIndex" to (RULE_INDEX[res.ruleId] ?: -1),
            "level" to res.level,
            "message" to linkedMapOf("text" to res.message),
            "locations" to listOf(
                linkedMapOf(
                    "physicalLocation" to linkedMapOf(
                        "artifactLocation" to linkedMapOf("uri" to res.uri),
                        "region" to linkedMapOf(
                            "startLine" to res.startLine,
                            "startColumn" to res.startColumn,
                        ),
                    ),
                ),
            ),
            "partialFingerprints" to linkedMapOf("leakGuard/v1" to res.fingerprint),
            "baselineState" to res.baselineState,
        )
    }

    val root = linkedMapOf<String, Any?>(
        "\$schema" to "https://json.schemastore.org/sarif-2.1.0.json",
        "version" to "2.1.0",
        "runs" to listOf(
            linkedMapOf(
                "tool" to linkedMapOf(
                    "driver" to linkedMapOf(
                        "name" to "leak-guard",
                        "version" to driverVersion,
                        "informationUri" to LEAK_GUARD_INFORMATION_URI,
                        "rules" to ruleObjects,
                    ),
                ),
                "results" to resultObjects,
            ),
        ),
    )

    return buildString {
        writeJson(this, root, 0)
        append('\n')
    }
}

// -------------------------------------------------------------------------------------------------
// Minimal, dependency-free JSON serializer (objects = LinkedHashMap, arrays = List, plus String /
// Number / Boolean / null). Enough for the fixed SARIF shape above; not a general-purpose library.
// -------------------------------------------------------------------------------------------------

private fun writeJson(sb: StringBuilder, value: Any?, indent: Int) {
    when (value) {
        null -> sb.append("null")
        is String -> writeJsonString(sb, value)
        is Boolean -> sb.append(value.toString())
        is Int, is Long -> sb.append(value.toString())
        is Map<*, *> -> writeJsonObject(sb, value, indent)
        is List<*> -> writeJsonArray(sb, value, indent)
        else -> writeJsonString(sb, value.toString())
    }
}

private fun writeJsonObject(sb: StringBuilder, map: Map<*, *>, indent: Int) {
    if (map.isEmpty()) {
        sb.append("{}")
        return
    }
    sb.append("{\n")
    val pad = "  ".repeat(indent + 1)
    val entries = map.entries.toList()
    for ((i, e) in entries.withIndex()) {
        sb.append(pad)
        writeJsonString(sb, e.key.toString())
        sb.append(": ")
        writeJson(sb, e.value, indent + 1)
        if (i < entries.size - 1) sb.append(',')
        sb.append('\n')
    }
    sb.append("  ".repeat(indent)).append('}')
}

private fun writeJsonArray(sb: StringBuilder, list: List<*>, indent: Int) {
    if (list.isEmpty()) {
        sb.append("[]")
        return
    }
    sb.append("[\n")
    val pad = "  ".repeat(indent + 1)
    for ((i, item) in list.withIndex()) {
        sb.append(pad)
        writeJson(sb, item, indent + 1)
        if (i < list.size - 1) sb.append(',')
        sb.append('\n')
    }
    sb.append("  ".repeat(indent)).append(']')
}

private fun writeJsonString(sb: StringBuilder, s: String) {
    sb.append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
    }
    sb.append('"')
}
