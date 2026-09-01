package io.github.vadimtoptunov.httpdatainjector

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleListCellRenderer
import io.github.vadimtoptunov.kassitestdata.generators.CatalogItem
import io.github.vadimtoptunov.kassitestdata.generators.HttpBodyDataCatalog

/**
 * Shared popup + insertion logic behind both entry points (editor action and intention). Kept
 * separate from IntelliJ's Action/Intention classes so it stays trivially testable.
 */
object HttpDataInjector {

    /** `.http` (JetBrains HTTP Client) files only — where a bare token or JSON snippet is directly usable. */
    fun isHttpFile(file: VirtualFile?): Boolean = file?.extension.equals("http", ignoreCase = true)

    fun showPopupAndInsert(project: Project?, editor: Editor) {
        val items = HttpBodyDataCatalog.items()
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setTitle("Insert HTTP Test Data")
            .setNamerForFiltering { it.searchText }
            .setRenderer(SimpleListCellRenderer.create<CatalogItem> { label, value, _ -> label.text = value.label })
            .setItemChosenCallback { item ->
                WriteCommandAction.runWriteCommandAction(project) {
                    insertAtCaret(editor, item.produce(null))
                }
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun insertAtCaret(editor: Editor, text: String) {
        EditorModificationUtilEx.insertStringAtCaret(editor, text)
    }
}
