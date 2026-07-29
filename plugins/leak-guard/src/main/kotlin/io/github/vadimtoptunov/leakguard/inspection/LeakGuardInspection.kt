package io.github.vadimtoptunov.leakguard.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.github.vadimtoptunov.kassitestdata.inspect.LeakGuard

/**
 * Scans every file's raw text for card numbers, IBANs and US SSNs that pass their real-world
 * checksum/format AND fall outside every publicly known reserved-for-testing range — see
 * [LeakGuard] for the algorithmic split between "safe test fixture" and "looks real". This is a
 * whole-file text scan rather than a language-specific one: a leaked PAN/IBAN/SSN is just as real
 * sitting in a `.env`, a JSON fixture or a log file as it is in source code.
 */
class LeakGuardInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file.virtualFile?.fileType?.isBinary == true) return
                val text = file.text
                if (text.length > MAX_SCAN_LENGTH) return
                val fileStart = file.textRange.startOffset

                for (finding in LeakGuard.scan(text)) {
                    val rangeInElement = TextRange(
                        finding.range.first - fileStart,
                        finding.range.last + 1 - fileStart,
                    )
                    holder.registerProblem(
                        file,
                        "Possible real ${finding.kind.label} — passes its checksum and isn't in a known reserved test range",
                        ProblemHighlightType.WARNING,
                        rangeInElement,
                        ReplaceWithSyntheticFix(finding.kind.label, finding.replacement),
                    )
                }
            }
        }

    private companion object {
        // A generated file that happens to be huge shouldn't stall the highlighting pass.
        const val MAX_SCAN_LENGTH = 2_000_000
    }
}
