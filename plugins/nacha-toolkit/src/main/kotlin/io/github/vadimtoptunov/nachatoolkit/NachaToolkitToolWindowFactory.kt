package io.github.vadimtoptunov.nachatoolkit

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the ACH / NACHA tool window. */
class NachaToolkitToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(NachaToolkitPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
