package io.github.vadimtoptunov.kassitestdata.playground

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "checksum-playground",
    pluginName = "Checksum & Regex Playground",
    reviewUrl = "https://plugins.jetbrains.com/plugin/33227/reviews",
    notificationGroupId = "Kassi Checksum Playground",
)
