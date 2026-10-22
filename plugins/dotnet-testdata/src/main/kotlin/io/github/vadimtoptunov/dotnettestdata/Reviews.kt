package io.github.vadimtoptunov.dotnettestdata

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "dotnet-testdata",
    pluginName = ".NET Test Data",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.dotnet-testdata",
    notificationGroupId = "Kassi .NET Test Data",
)
