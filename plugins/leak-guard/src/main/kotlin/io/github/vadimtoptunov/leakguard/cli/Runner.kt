package io.github.vadimtoptunov.leakguard.cli

import io.github.vadimtoptunov.kassitestdata.inspect.LeakGuard

/**
 * The pure core of the CI runner: turns located findings (per file) into resolved [RunResult]s —
 * assigning the line-independent fingerprint and the baseline state — and computes the failing set
 * for a gate. Free of file I/O and `exitProcess` so it is unit testable; `Cli.kt` wires it to the
 * filesystem and the process exit code.
 */

/** One finding already resolved to a source position, plus the matched token (for the fingerprint). */
data class LocatedFinding(
    val kind: LeakGuard.Kind,
    val startLine: Int,
    val startColumn: Int,
    val token: String,
)

/** A scanned file's findings plus its repo-relative path. */
data class FileFindings(val repoRelativePath: String, val findings: List<LocatedFinding>)

/** Which findings fail the build. Default (and `--fail-on-any`) = ANY level. */
enum class FailGate { ANY, WARNING, ERROR }

/** Secrets (no safe synthetic swap — must be redacted) are errors; checksum-values are warnings. */
fun levelOf(kind: LeakGuard.Kind): String = if (kind.synthetic) "warning" else "error"

fun messageOf(kind: LeakGuard.Kind): String = if (kind.synthetic) {
    "Possible real ${kind.label} — passes its checksum and isn't in a known reserved test range"
} else {
    "Possible leaked ${kind.label} — remove it before committing"
}

private fun levelRank(level: String): Int = when (level) {
    "error" -> 2
    "warning" -> 1
    else -> 0
}

private fun gateThreshold(gate: FailGate): Int = when (gate) {
    FailGate.ANY -> 0
    FailGate.WARNING -> 1
    FailGate.ERROR -> 2
}

/**
 * Resolves every finding into a [RunResult]. Within each file, findings sharing a [LeakGuard.Kind]
 * are ordered by position and given a 0-based `occurrenceIndex`, which (with the matched token) feeds
 * the fingerprint. Results are emitted in position order per file.
 */
fun buildResults(files: List<FileFindings>, baseline: Baseline): List<RunResult> {
    val out = ArrayList<RunResult>()
    for (file in files) {
        val counters = HashMap<LeakGuard.Kind, Int>()
        val ordered = file.findings.sortedWith(compareBy({ it.startLine }, { it.startColumn }))
        for (f in ordered) {
            val occurrenceIndex = counters.getOrDefault(f.kind, 0)
            counters[f.kind] = occurrenceIndex + 1

            val fp = fingerprint(file.repoRelativePath, f.kind.name, occurrenceIndex, f.token)
            out += RunResult(
                ruleId = f.kind.name,
                level = levelOf(f.kind),
                message = messageOf(f.kind),
                uri = file.repoRelativePath,
                startLine = f.startLine,
                startColumn = f.startColumn,
                fingerprint = fp,
                baselineState = if (baseline.isNew(fp)) "new" else "unchanged",
            )
        }
    }
    return out
}

/** The findings that count toward the exit code: NEW (not in baseline) and meeting the gate. */
fun failingResults(results: List<RunResult>, gate: FailGate): List<RunResult> {
    val threshold = gateThreshold(gate)
    return results.filter { it.baselineState == "new" && levelRank(it.level) >= threshold }
}

/** All fingerprints in this run (used by `--write-baseline`). */
fun allFingerprints(results: List<RunResult>): List<String> = results.map { it.fingerprint }
