package io.github.vadimtoptunov.sqlseeder

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the SQL Seeder tool window. */
class SqlSeederToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(SqlSeederPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
