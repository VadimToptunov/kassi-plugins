package io.github.vadimtoptunov.leakguard.inspection

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "leak-guard",
    pluginName = "Secret & Card Leak Guard",
    reviewUrl = "https://plugins.jetbrains.com/plugin/34133/reviews",
    notificationGroupId = "Kassi Leak Guard",
)
