package io.github.vadimtoptunov.httpdatainjector

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "http-data-injector",
    pluginName = "Kassi HTTP Data Injector",
    reviewUrl = "https://plugins.jetbrains.com/plugin/33967/reviews",
    notificationGroupId = "Kassi HTTP Data Injector",
)
