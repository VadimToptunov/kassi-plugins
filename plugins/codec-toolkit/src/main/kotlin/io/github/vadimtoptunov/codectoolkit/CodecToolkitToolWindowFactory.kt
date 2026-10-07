package io.github.vadimtoptunov.codectoolkit

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the Codec tool window. */
class CodecToolkitToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(CodecToolkitPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
