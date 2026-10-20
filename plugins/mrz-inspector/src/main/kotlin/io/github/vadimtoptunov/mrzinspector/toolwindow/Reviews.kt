package io.github.vadimtoptunov.mrzinspector.toolwindow

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "mrz-inspector",
    pluginName = "MRZ & Barcode Inspector",
    reviewUrl = "https://plugins.jetbrains.com/plugin/33325/reviews",
    notificationGroupId = "Kassi MRZ Inspector",
)
