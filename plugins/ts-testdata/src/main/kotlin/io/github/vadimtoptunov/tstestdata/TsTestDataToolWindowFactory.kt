package io.github.vadimtoptunov.tstestdata

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Registers the "TS Test Data" tool window. */
class TsTestDataToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        reviewPrompt.recordUse(project)
        val content = ContentFactory.getInstance().createContent(TsTestDataPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
