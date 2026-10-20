package io.github.vadimtoptunov.kassi.review

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.github.vadimtoptunov.kassitestdata.review.ReviewPromptPolicy
import io.github.vadimtoptunov.kassitestdata.review.ReviewPromptState

/**
 * One-time, non-modal "leave a review" balloon; the rules live in [ReviewPromptPolicy].
 *
 * Shared source compiled into every Kassi plugin (see the `shared-ide` srcDir in each plugin's
 * build script). Counters are stored application-wide in [PropertiesComponent] under keys prefixed
 * with [pluginKey], so each plugin keeps its own and the only per-plugin registration is the
 * `notificationGroup` in plugin.xml.
 */
class ReviewPrompt(
    private val pluginKey: String,
    private val pluginName: String,
    private val reviewUrl: String,
    private val notificationGroupId: String,
) {
    private val prefix = "kassi.review.$pluginKey."

    /** Call after a successful use of the plugin; shows the balloon only when the policy says so. */
    fun recordUse(project: Project?) {
        val decision = ReviewPromptPolicy.onUse(load(), System.currentTimeMillis())
        save(decision.state)
        if (decision.ask) ApplicationManager.getApplication().invokeLater { show(project) }
    }

    private fun show(project: Project?) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(notificationGroupId) ?: return
        group.createNotification(
                "Enjoying $pluginName?",
                "A short review on the JetBrains Marketplace helps other engineers find it.",
                NotificationType.INFORMATION,
            )
            .addAction(NotificationAction.createSimpleExpiring("Leave a review") {
                finish()
                BrowserUtil.browse(reviewUrl)
            })
            .addAction(NotificationAction.createSimpleExpiring("Not now") { })
            .addAction(NotificationAction.createSimpleExpiring("Don't ask again") { finish() })
            .notify(project)
    }

    private fun finish() = save(ReviewPromptPolicy.finished(load()))

    private fun load(): ReviewPromptState {
        val props = PropertiesComponent.getInstance()
        return ReviewPromptState(
            firstUseAt = props.getValue(prefix + "firstUseAt")?.toLongOrNull() ?: 0L,
            usesSinceAsk = props.getValue(prefix + "uses")?.toIntOrNull() ?: 0,
            asks = props.getValue(prefix + "asks")?.toIntOrNull() ?: 0,
            done = props.getBoolean(prefix + "done", false),
        )
    }

    private fun save(state: ReviewPromptState) {
        val props = PropertiesComponent.getInstance()
        props.setValue(prefix + "firstUseAt", state.firstUseAt.toString())
        props.setValue(prefix + "uses", state.usesSinceAsk.toString())
        props.setValue(prefix + "asks", state.asks.toString())
        props.setValue(prefix + "done", state.done, false)
    }
}
