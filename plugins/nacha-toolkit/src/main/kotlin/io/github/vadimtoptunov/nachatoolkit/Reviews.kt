package io.github.vadimtoptunov.nachatoolkit

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "nacha-toolkit",
    pluginName = "ACH / NACHA Toolkit",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.nacha-toolkit",
    notificationGroupId = "Kassi NACHA Toolkit",
)
