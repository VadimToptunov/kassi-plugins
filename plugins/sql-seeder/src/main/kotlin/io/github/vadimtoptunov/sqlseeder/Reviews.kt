package io.github.vadimtoptunov.sqlseeder

import io.github.vadimtoptunov.kassi.review.ReviewPrompt

/** The one-time Marketplace review nudge for this plugin (rules: ReviewPromptPolicy). */
internal val reviewPrompt = ReviewPrompt(
    pluginKey = "sql-seeder",
    pluginName = "SQL Synthetic Seeder",
    reviewUrl = "https://plugins.jetbrains.com/plugin/index?xmlId=io.github.vadimtoptunov.sql-seeder",
    notificationGroupId = "Kassi SQL Seeder",
)
