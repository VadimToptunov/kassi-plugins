package io.github.vadimtoptunov.phptestdata

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the "PHP Test Data" tool window. */
class PhpTestDataToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        reviewPrompt.recordUse(project)
        val content = ContentFactory.getInstance().createContent(PhpTestDataPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
