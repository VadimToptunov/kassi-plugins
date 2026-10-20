package io.github.vadimtoptunov.uuidtoolkit

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "uuid-toolkit",
    pluginName = "UUID, ULID & NanoID Toolkit",
    reviewUrl = "https://plugins.jetbrains.com/plugin/33488/reviews",
    notificationGroupId = "Kassi UUID Toolkit",
)
