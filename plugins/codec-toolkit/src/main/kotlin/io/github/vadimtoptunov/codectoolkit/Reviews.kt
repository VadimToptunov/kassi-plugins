package io.github.vadimtoptunov.codectoolkit

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "codec-toolkit",
    pluginName = "Crypto Codec Toolkit",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.codec-toolkit",
    notificationGroupId = "Kassi Codec Toolkit",
)
