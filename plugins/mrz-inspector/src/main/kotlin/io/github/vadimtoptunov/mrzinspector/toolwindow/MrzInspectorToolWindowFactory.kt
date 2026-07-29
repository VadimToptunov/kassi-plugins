package io.github.vadimtoptunov.mrzinspector.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

/**
 * Entry point: registers the "MRZ Inspector" tool window with two tabs, MRZ and Barcode —
 * both offline, both backed directly by :engine.
 */
class MrzInspectorToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs = JBTabbedPane()
        tabs.addTab("MRZ", MrzPanel())
        tabs.addTab("Barcode (AAMVA)", BarcodePanel())

        val content = ContentFactory.getInstance().createContent(tabs, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
