package io.github.vadimtoptunov.httpdatainjector.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import io.github.vadimtoptunov.httpdatainjector.HttpDataInjector

/**
 * Alt+Enter entry point for `.http` files: "Insert test data (IBAN / PAN / BIC / Persona)".
 * Shares [HttpDataInjector] with the editor-popup action so both surfaces stay in sync.
 */
class InsertHttpTestDataIntention : IntentionAction {

    override fun getText(): String = "Insert test data (IBAN / PAN / BIC / Persona)…"

    override fun getFamilyName(): String = "Kassi HTTP test data"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        editor != null && HttpDataInjector.isHttpFile(file?.virtualFile)

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null) return
        HttpDataInjector.showPopupAndInsert(project, editor)
    }

    override fun startInWriteAction(): Boolean = false
}
