package io.github.vadimtoptunov.piianonymizer

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "pii-anonymizer",
    pluginName = "PII Anonymizer",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.pii-anonymizer",
    notificationGroupId = "Kassi PII Anonymizer",
)
