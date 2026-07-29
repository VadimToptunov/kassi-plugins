package io.github.vadimtoptunov.httpdatainjector.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import io.github.vadimtoptunov.httpdatainjector.HttpDataInjector

/**
 * Editor context-menu entry point. Visible only in `.http` files, where the inserted value
 * (a bare token or a JSON object) is directly usable in a request URL, header, or body.
 */
class InsertHttpTestDataAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = editor != null && HttpDataInjector.isHttpFile(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        HttpDataInjector.showPopupAndInsert(e.project, editor)
    }
}
