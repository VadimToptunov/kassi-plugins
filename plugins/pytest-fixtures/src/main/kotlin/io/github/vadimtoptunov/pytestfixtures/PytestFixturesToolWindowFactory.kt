package io.github.vadimtoptunov.pytestfixtures

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the pytest Fixtures tool window. */
class PytestFixturesToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(PytestFixturesPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
