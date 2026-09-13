package io.github.vadimtoptunov.piianonymizer

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import io.github.vadimtoptunov.kassitestdata.inspect.PiiAnonymizer

/**
 * Editor action: replace the PII in the current file (or selection) with valid-synthetic equivalents,
 * so the file can be attached to a ticket without leaking real data. The detection + replacement is the
 * shared, unit-tested [PiiAnonymizer]; here we only read the selected/whole text and write the result.
 */
class AnonymizePiiAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null && editor.document.isWritable
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val selection = editor.selectionModel
        val hasSelection = selection.hasSelection()
        val source = if (hasSelection) selection.selectedText.orEmpty() else document.text
        val result = PiiAnonymizer.anonymize(source)
        if (result.count == 0) return

        WriteCommandAction.runWriteCommandAction(project, "Anonymize PII", null, {
            if (hasSelection) {
                document.replaceString(selection.selectionStart, selection.selectionEnd, result.text)
            } else {
                document.setText(result.text)
            }
        }, )
    }
}
