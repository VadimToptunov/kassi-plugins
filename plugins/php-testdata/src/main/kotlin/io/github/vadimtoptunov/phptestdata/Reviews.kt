package io.github.vadimtoptunov.phptestdata

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "php-testdata",
    pluginName = "PHP Test Data",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.php-testdata",
    notificationGroupId = "Kassi PHP Test Data",
)
