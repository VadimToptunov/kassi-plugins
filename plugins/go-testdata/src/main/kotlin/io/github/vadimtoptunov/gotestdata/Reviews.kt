package io.github.vadimtoptunov.gotestdata

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "go-testdata",
    pluginName = "Go Test Data",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.go-testdata",
    notificationGroupId = "Kassi Go Test Data",
)
