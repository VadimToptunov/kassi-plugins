package io.github.vadimtoptunov.dotnettestdata

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the "C# Test Data" tool window. */
class DotNetTestDataToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        reviewPrompt.recordUse(project)
        val content = ContentFactory.getInstance().createContent(DotNetTestDataPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
