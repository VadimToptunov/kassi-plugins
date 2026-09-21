package io.github.vadimtoptunov.leakguard.cli

import io.github.vadimtoptunov.kassitestdata.inspect.LeakGuard
import java.io.File
import kotlin.system.exitProcess

/**
 * A dependency-free CLI / CI runner that runs the Leak Guard scan on files WITHOUT an IDE, so the
 * same "real value vs safe test data" check that highlights in the editor can gate a commit or PR.
 *
 * It walks the given file/dir paths, scans every readable text file (skipping binary and very large
 * files), and reports card numbers / IBANs / US SSNs that pass their checksum and fall outside every
 * reserved test range, plus AWS keys / JWTs / private-key blocks.
 *
 *   --sarif <file>              also write a SARIF 2.1.0 report to <file>
 *   --baseline <file>           read it; only NEW (non-baseline) findings count toward the exit code
 *   --write-baseline <file>     write the current findings' fingerprints as a fresh baseline; exit 0
 *   --fail-on-severity <lvl>    fail only on findings at/above <error|warning> (default: fail on any)
 *   --fail-on-any               fail on any finding regardless of severity (the default gate)
 *
 * The matched secret is never printed or written to the SARIF report — only its location and kind.
 * The IntelliJ inspection remains the authoritative in-IDE experience; this is the headless path.
 */
private const val DRIVER_VERSION = "1.2.0"

// A generated/vendored file that happens to be huge shouldn't stall the scan (matches the inspection).
private const val MAX_SCAN_LENGTH = 2_000_000

private class Options(
    val roots: List<String>,
    val sarif: String?,
    val baseline: String?,
    val writeBaseline: String?,
    val gate: FailGate,
)

fun main(args: Array<String>) {
    val opts = parseArgs(args)

    val roots = opts.roots.map { File(it) }
    val missing = roots.filter { !it.exists() }
    if (missing.isNotEmpty()) {
        missing.forEach { System.err.println("no such file or directory: ${it.path}") }
        exitProcess(2)
    }

    val base = File(".").canonicalFile
    val files = roots.flatMap { root ->
        if (root.isDirectory) root.walkTopDown().filter { it.isFile }.toList() else listOf(root)
    }

    val fileFindings = ArrayList<FileFindings>()
    var scanned = 0
    for (file in files) {
        val text = readTextOrNull(file) ?: continue
        scanned++
        val located = LeakGuard.scan(text).map { f ->
            val (line, col) = locate(text, f.range.first)
            LocatedFinding(f.kind, line, col, f.matched)
        }
        if (located.isNotEmpty()) fileFindings += FileFindings(relativePath(base, file), located)
    }

    val baseline = opts.baseline?.let { Baseline.read(File(it)) } ?: Baseline.EMPTY
    val results = buildResults(fileFindings, baseline)

    // --write-baseline: snapshot the current findings and exit clean (existing matches accepted).
    if (opts.writeBaseline != null) {
        Baseline.write(File(opts.writeBaseline), allFingerprints(results))
        println("leak-guard: wrote baseline ${opts.writeBaseline} with ${results.size} fingerprint(s).")
        exitProcess(0)
    }

    if (opts.sarif != null) {
        File(opts.sarif).writeText(toSarif(DRIVER_VERSION, results))
    }

    val baselineProvided = opts.baseline != null
    for (r in results) {
        val suffix = if (baselineProvided) " (${r.baselineState})" else ""
        println("${r.uri}:${r.startLine}:${r.startColumn}: [${r.ruleId}] ${r.message}$suffix")
    }

    val failing = failingResults(results, opts.gate)
    println("leak-guard: scanned $scanned file(s), ${results.size} finding(s), ${failing.size} failing.")
    exitProcess(if (failing.isNotEmpty()) 1 else 0)
}

private fun parseArgs(args: Array<String>): Options {
    val roots = ArrayList<String>()
    var sarif: String? = null
    var baseline: String? = null
    var writeBaseline: String? = null
    var gate = FailGate.ANY

    fun value(name: String, i: Int): String {
        if (i + 1 >= args.size) {
            System.err.println("$name requires an argument")
            exitProcess(2)
        }
        return args[i + 1]
    }

    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when (arg) {
            "--sarif" -> { sarif = value(arg, i); i += 2 }
            "--baseline" -> { baseline = value(arg, i); i += 2 }
            "--write-baseline" -> { writeBaseline = value(arg, i); i += 2 }
            "--fail-on-any" -> { gate = FailGate.ANY; i += 1 }
            "--fail-on-severity" -> {
                gate = when (val v = value(arg, i).lowercase()) {
                    "error" -> FailGate.ERROR
                    "warning" -> FailGate.WARNING
                    else -> {
                        System.err.println("--fail-on-severity must be 'error' or 'warning' (got '$v')")
                        exitProcess(2)
                    }
                }
                i += 2
            }
            else -> {
                if (arg.startsWith("--")) {
                    System.err.println("unknown option: $arg")
                    exitProcess(2)
                }
                roots += arg
                i += 1
            }
        }
    }

    if (roots.isEmpty()) {
        System.err.println(
            "usage: leak-guard [--sarif <file>] [--baseline <file>] [--write-baseline <file>] " +
                "[--fail-on-severity <error|warning>] [--fail-on-any] <file-or-dir> [<file-or-dir> ...]",
        )
        exitProcess(2)
    }

    return Options(roots, sarif, baseline, writeBaseline, gate)
}

/** Reads a file as UTF-8 text, or null if it is too large or looks binary (a NUL byte in the head). */
private fun readTextOrNull(file: File): String? {
    if (file.length() > MAX_SCAN_LENGTH) return null
    val bytes = try {
        file.readBytes()
    } catch (_: Exception) {
        return null
    }
    val head = minOf(bytes.size, 8000)
    for (i in 0 until head) if (bytes[i].toInt() == 0) return null
    return String(bytes, Charsets.UTF_8)
}

/** 1-based (line, column) of a character [offset] into [text]. */
private fun locate(text: String, offset: Int): Pair<Int, Int> {
    var line = 1
    var col = 1
    val end = offset.coerceIn(0, text.length)
    var i = 0
    while (i < end) {
        if (text[i] == '\n') {
            line++
            col = 1
        } else {
            col++
        }
        i++
    }
    return line to col
}

private fun relativePath(base: File, file: File): String {
    val abs = file.canonicalFile
    return (abs.relativeToOrNull(base)?.path ?: abs.path).replace(File.separatorChar, '/')
}
