package io.github.vadimtoptunov.leakguard.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

/** Replaces the flagged range with a Kassi-generated, checksum-valid synthetic equivalent. */
class ReplaceWithSyntheticFix(
    private val kindLabel: String,
    private val replacement: String,
) : LocalQuickFix {

    override fun getFamilyName(): String = "Replace with synthetic test $kindLabel"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val file = element.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val range = descriptor.textRangeInElement.shiftRight(element.textRange.startOffset)

        WriteCommandAction.runWriteCommandAction(project, "Replace With Synthetic Test Data", null, {
            document.replaceString(range.startOffset, range.endOffset, replacement)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }, file)
    }
}
