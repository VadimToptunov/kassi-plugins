package io.github.vadimtoptunov.uuidtoolkit

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the ID Toolkit tool window. */
class UuidToolkitToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(UuidToolkitPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
