package io.github.vadimtoptunov.kassitestdata.playground

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

/** Registers the "Test Data Playground" tool window with two tabs: checksum inspector + regex tester. */
class PlaygroundToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs = JBTabbedPane().apply {
            addTab("Checksums", ChecksumPanel())
            addTab("Regex", RegexPanel())
        }
        val content = ContentFactory.getInstance().createContent(tabs, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
