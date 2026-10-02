package io.github.vadimtoptunov.kassitestdata.nacha

import io.github.vadimtoptunov.kassitestdata.core.Rng

/**
 * NACHA ACH file support — the US automated-clearing-house batch format (Nacha Operating Rules).
 * Every record is exactly 94 characters; the file is blocked to a multiple of 10 records with all-9
 * filler rows. This object both GENERATES a spec-valid PPD file (plus a deliberately-invalid variant
 * for negative tests) and VALIDATES an existing file's structure and control totals.
 *
 * Fully offline and deterministic. The one externally-anchored check is the ABA routing-transit
 * check digit (weights 3,7,1 repeating, sum mod 10 == 0), verifiable against real published Federal
 * Reserve routing numbers — see the unit test. Everything else (entry hash, entry/addenda count,
 * total debit/credit, block count) is recomputed from the file and compared to what it declares.
 */
object Nacha {

    // ---- ABA routing-transit number (9 digits) -------------------------------------------------

    private val ABA_WEIGHTS = intArrayOf(3, 7, 1, 3, 7, 1, 3, 7, 1)

    /** Check digit (9th) for an 8-digit routing prefix: (10 - weightedSum mod 10) mod 10. */
    fun routingCheckDigit(first8: String): Int {
        require(first8.length == 8 && first8.all { it.isDigit() }) { "need 8 digits" }
        var sum = 0
        for (i in 0 until 8) sum += (first8[i] - '0') * ABA_WEIGHTS[i]
        return (10 - (sum % 10)) % 10
    }

    /** True when a 9-digit routing number satisfies the ABA weighted mod-10 check. */
    fun isValidRoutingNumber(routing: String): Boolean {
        if (routing.length != 9 || !routing.all { it.isDigit() }) return false
        var sum = 0
        for (i in 0 until 9) sum += (routing[i] - '0') * ABA_WEIGHTS[i]
        return sum % 10 == 0
    }

    /** A routing number whose first 8 digits come from [seed]-driven randomness, with a correct check digit. */
    fun generateRoutingNumber(rng: Rng): String {
        // First two digits 01..12 keep it in the valid Federal Reserve district range.
        val district = (rng.int(12) + 1).toString().padStart(2, '0')
        val first8 = district + (0 until 6).joinToString("") { rng.int(10).toString() }
        return first8 + routingCheckDigit(first8)
    }

    // ---- File model ----------------------------------------------------------------------------

    /** One credit/debit entry in a batch. [amountCents] is the transfer in cents. */
    data class Entry(
        val transactionCode: String, // "22" checking credit, "27" checking debit, "32"/"37" savings
        val routingNumber: String,   // 9-digit RDFI routing number (receiver's bank)
        val accountNumber: String,
        val amountCents: Long,
        val individualName: String,
        val individualId: String = "",
        /** Optional payment-related information; when set, a type-7 addenda (type code 05) follows the entry. */
        val addenda: String? = null,
    ) {
        val isDebit: Boolean get() = transactionCode.endsWith("7")
    }

    private const val W = 94

    // Recognised NACHA addenda type codes: 02 (POS/MTE/SHR), 05 (payment related, PPD/CCD/CTX),
    // 98 (notification of change), 99 (return).
    private val ADDENDA_TYPE_CODES = setOf("02", "05", "98", "99")

    // ---- Generation ----------------------------------------------------------------------------

    /**
     * A spec-valid single-batch PPD credit file for [entries]. When [valid] is false the file control's
     * total-credit amount is corrupted by one cent, so a validator flags exactly that field (a targeted
     * negative-test fixture) while the rest of the file stays well-formed.
     */
    fun generateFile(
        entries: List<Entry>,
        originRouting: String,
        companyName: String = "KASSI TEST CO",
        companyId: String = "1234567890",
        valid: Boolean = true,
    ): String {
        require(entries.isNotEmpty()) { "at least one entry" }
        val originDfi = originRouting.take(8)
        val creditCents = entries.filterNot { it.isDebit }.sumOf { it.amountCents }
        val debitCents = entries.filter { it.isDebit }.sumOf { it.amountCents }
        val hash = entryHash(entries.map { it.routingNumber })
        val serviceClass = serviceClassFor(entries)

        // The entry/addenda count in the batch and file control records counts type-6 AND type-7 records.
        val entryAddendaCount = entries.size + entries.count { it.addenda != null }

        val lines = ArrayList<String>()
        lines += fileHeader(originRouting)
        lines += batchHeader(serviceClass, companyName, companyId, originDfi)
        entries.forEachIndexed { i, e ->
            lines += entryDetail(e, originDfi, i + 1L)
            if (e.addenda != null) lines += addendaRecord(e.addenda, i + 1L)
        }
        lines += batchControl(serviceClass, entryAddendaCount, hash, debitCents, creditCents, companyId, originDfi)
        val declaredCredit = if (valid) creditCents else creditCents + 1
        lines += fileControl(batchCount = 1, entryCount = entryAddendaCount, hash = hash,
            debitCents = debitCents, creditCents = declaredCredit, dataRecords = lines.size + 1)
        // Block to a multiple of 10 with all-9 filler rows.
        while (lines.size % 10 != 0) lines += "9".repeat(W)
        return lines.joinToString("\n")
    }

    /** A ready-made sample file (seeded) using real published routing numbers, for the "insert sample" action. */
    fun sampleFile(rng: Rng, valid: Boolean = true): String {
        val entries = listOf(
            // The first entry carries a type-7 addenda (payment-related information).
            Entry("22", "021000021", "12345678", 125_00, "ALICE EXAMPLE", addenda = "INV 10042 PAYMENT"), // JPMorgan Chase
            Entry("22", "011000015", "98765432", 4_200_00, "BOB EXAMPLE"),    // FRB Boston
            Entry("27", "121000248", "55554444", 50_00, "CAROL EXAMPLE"),     // Wells Fargo (debit)
        )
        return generateFile(entries, originRouting = generateRoutingNumber(rng), valid = valid)
    }

    private fun serviceClassFor(entries: List<Entry>): String {
        val hasCredit = entries.any { !it.isDebit }
        val hasDebit = entries.any { it.isDebit }
        return when {
            hasCredit && hasDebit -> "200"
            hasDebit -> "225"
            else -> "220"
        }
    }

    /** Entry hash: sum of each entry's 8-digit routing prefix, keeping the rightmost 10 digits. */
    fun entryHash(routingNumbers: List<String>): Long {
        val sum = routingNumbers.sumOf { it.take(8).toLong() }
        return sum % 10_000_000_000L
    }

    private fun fileHeader(originRouting: String): String = buildString {
        append('1')                      // record type
        append("01")                     // priority code
        append(' ').append(originRouting)      // immediate destination: space + 9 digits
        append(' ').append(originRouting)      // immediate origin: space + 9 digits
        append("260101")                 // file creation date YYMMDD (fixed for determinism)
        append("0000")                   // file creation time HHMM
        append('A')                      // file ID modifier
        append("094")                    // record size
        append("10")                     // blocking factor
        append('1')                      // format code
        append(fit("KASSI TEST BANK", 23))
        append(fit("KASSI TEST CO", 23))
        append(fit("", 8))
    }

    private fun batchHeader(serviceClass: String, companyName: String, companyId: String, originDfi: String): String =
        buildString {
            append('5')
            append(serviceClass)
            append(fit(companyName, 16))
            append(fit("", 20))                 // company discretionary data
            append(fit(companyId, 10))
            append("PPD")                        // standard entry class
            append(fit("PAYROLL", 10))           // company entry description
            append(fit("", 6))                   // company descriptive date
            append("260102")                     // effective entry date YYMMDD
            append(fit("", 3))                   // settlement date (filled by operator)
            append('1')                          // originator status code
            append(originDfi)                    // originating DFI (8)
            append(numStr(1, 7))                 // batch number
        }

    private fun entryDetail(e: Entry, originDfi: String, sequence: Long): String = buildString {
        append('6')
        append(e.transactionCode)
        append(e.routingNumber.take(8))          // receiving DFI id (8)
        append(e.routingNumber[8])               // check digit (9th)
        append(fit(e.accountNumber, 17))
        append(numStr(e.amountCents, 10))
        append(fit(e.individualId, 15))
        append(fit(e.individualName, 22))
        append(fit("", 2))                       // discretionary data
        append(if (e.addenda != null) '1' else '0') // addenda record indicator
        append(originDfi).append(numStr(sequence, 7)) // trace number (15)
    }

    /**
     * A type-7 addenda record (addenda type code 05 — payment-related information). Carries the free-text
     * [info], an addenda sequence number (0001, one addenda per entry here), and the entry-detail
     * sequence number — the last 7 digits of the parent entry's trace number.
     */
    private fun addendaRecord(info: String, entryDetailSequence: Long): String = buildString {
        append('7')
        append("05")                             // addenda type code (payment related)
        append(fit(info, 80))
        append(numStr(1, 4))                     // addenda sequence number
        append(numStr(entryDetailSequence, 7))   // entry detail sequence number
    }

    private fun batchControl(
        serviceClass: String, entryCount: Int, hash: Long, debitCents: Long, creditCents: Long,
        companyId: String, originDfi: String,
    ): String = buildString {
        append('8')
        append(serviceClass)
        append(numStr(entryCount.toLong(), 6))   // entry/addenda count
        append(numStr(hash, 10))
        append(numStr(debitCents, 12))
        append(numStr(creditCents, 12))
        append(fit(companyId, 10))
        append(fit("", 19))                      // message authentication code
        append(fit("", 6))                       // reserved
        append(originDfi)
        append(numStr(1, 7))                     // batch number
    }

    private fun fileControl(
        batchCount: Int, entryCount: Int, hash: Long, debitCents: Long, creditCents: Long, dataRecords: Int,
    ): String = buildString {
        val blockCount = (dataRecords + 9) / 10
        append('9')
        append(numStr(batchCount.toLong(), 6))
        append(numStr(blockCount.toLong(), 6))
        append(numStr(entryCount.toLong(), 8))
        append(numStr(hash, 10))
        append(numStr(debitCents, 12))
        append(numStr(creditCents, 12))
        append(fit("", 39))                      // reserved
    }

    /** Left-justify text into [width], padding with spaces and truncating; uppercased ASCII. */
    private fun fit(s: String, width: Int): String {
        val up = s.uppercase().take(width)
        return up + " ".repeat(width - up.length)
    }

    private fun numStr(n: Long, width: Int): String = n.toString().padStart(width, '0').takeLast(width)

    // ---- Validation ----------------------------------------------------------------------------

    /** One structural problem found in a file: 1-based [line] (0 = file-level) and a message. */
    data class Problem(val line: Int, val message: String)

    /**
     * Validate a NACHA file's structure and control totals. Returns an empty list for a well-formed
     * file. Checks: 94-char records, record-type sequence, entry routing check digits, and the batch/
     * file control entry-hash, total-debit, total-credit, entry count and block count.
     */
    fun validate(text: String): List<Problem> {
        val problems = ArrayList<Problem>()
        val lines = text.split("\n").filter { it.isNotEmpty() }
        if (lines.isEmpty()) return listOf(Problem(0, "Empty file."))

        lines.forEachIndexed { i, line ->
            if (line.length != W) problems += Problem(i + 1, "Record is ${line.length} chars, must be $W.")
        }
        if (problems.isNotEmpty()) return problems // positional checks below assume 94-char rows

        if (lines.first()[0] != '1') problems += Problem(1, "File must start with a '1' File Header record.")
        val fileControl = lines.lastOrNull { it[0] == '9' && it.substring(0, 2) != "99" }
        if (lines.none { it[0] == '9' }) problems += Problem(lines.size, "File must end with a '9' File Control record.")

        // Collect entries (type 6) and recompute the batch/file controls.
        val entryLines = lines.withIndex().filter { it.value[0] == '6' }
        var creditCents = 0L
        var debitCents = 0L
        val routings = ArrayList<String>()
        for ((idx, line) in entryLines) {
            val routing = line.substring(3, 12)              // 8-digit id + check digit
            routings += routing
            if (!isValidRoutingNumber(routing)) {
                problems += Problem(idx + 1, "Entry routing number $routing fails the ABA check digit.")
            }
            val amount = line.substring(29, 39).toLongOrNull() ?: 0L
            if (line.substring(1, 3).endsWith("7")) debitCents += amount else creditCents += amount
        }
        val hash = entryHash(routings)

        // Addenda records (type 7): each must carry a recognised addenda type code; the entry/addenda
        // count in the controls counts them alongside the type-6 entries.
        val addendaLines = lines.withIndex().filter { it.value[0] == '7' }
        for ((idx, line) in addendaLines) {
            val addendaType = line.substring(1, 3)
            if (addendaType !in ADDENDA_TYPE_CODES) {
                problems += Problem(idx + 1, "Addenda type code '$addendaType' is not a recognised NACHA code.")
            }
        }
        val entryAddendaCount = (entryLines.size + addendaLines.size).toLong()

        // File Control (type 9, not a 99.. filler): positions per the Nacha spec.
        fileControl?.let { fc ->
            val fcLine = lines.indexOf(fc) + 1
            val declaredEntries = fc.substring(13, 21).toLongOrNull()
            if (declaredEntries != null && declaredEntries != entryAddendaCount) {
                problems += Problem(fcLine, "File Control entry/addenda count $declaredEntries != actual $entryAddendaCount.")
            }
            val declaredHash = fc.substring(21, 31).toLongOrNull()
            if (declaredHash != null && declaredHash != hash) {
                problems += Problem(fcLine, "File Control entry hash $declaredHash != recomputed $hash.")
            }
            val declaredDebit = fc.substring(31, 43).toLongOrNull()
            if (declaredDebit != null && declaredDebit != debitCents) {
                problems += Problem(fcLine, "File Control total debit $declaredDebit != recomputed $debitCents.")
            }
            val declaredCredit = fc.substring(43, 55).toLongOrNull()
            if (declaredCredit != null && declaredCredit != creditCents) {
                problems += Problem(fcLine, "File Control total credit $declaredCredit != recomputed $creditCents.")
            }
            val declaredBlocks = fc.substring(7, 13).toLongOrNull()
            val expectedBlocks = ((lines.size + 9) / 10).toLong()
            if (declaredBlocks != null && declaredBlocks != expectedBlocks) {
                problems += Problem(fcLine, "File Control block count $declaredBlocks != expected $expectedBlocks.")
            }
        }
        if (lines.size % 10 != 0) {
            problems += Problem(lines.size, "File has ${lines.size} records; must be blocked to a multiple of 10.")
        }
        return problems
    }
}
