package io.github.vadimtoptunov.pytestfixtures

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "pytest-fixtures",
    pluginName = "pytest Fixtures",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.pytest-fixtures",
    notificationGroupId = "Kassi pytest Fixtures",
)
