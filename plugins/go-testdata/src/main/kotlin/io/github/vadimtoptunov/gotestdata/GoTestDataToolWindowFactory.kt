package io.github.vadimtoptunov.gotestdata

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the Go Test Data tool window. */
class GoTestDataToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(GoTestDataPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
