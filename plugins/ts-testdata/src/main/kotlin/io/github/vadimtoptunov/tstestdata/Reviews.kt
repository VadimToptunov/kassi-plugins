package io.github.vadimtoptunov.tstestdata

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "ts-testdata",
    pluginName = "TypeScript Test Data",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.ts-testdata",
    notificationGroupId = "Kassi TypeScript Test Data",
)
