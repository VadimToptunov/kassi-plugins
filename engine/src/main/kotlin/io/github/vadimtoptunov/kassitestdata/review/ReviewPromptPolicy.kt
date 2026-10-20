package io.github.vadimtoptunov.kassitestdata.review

/** Counters behind the Marketplace review nudge; the IDE layer persists them per plugin. */
data class ReviewPromptState(
    val firstUseAt: Long = 0L,
    val usesSinceAsk: Int = 0,
    val asks: Int = 0,
    val done: Boolean = false,
)

/**
 * Pure rules for when a plugin may ask for a Marketplace review, kept IDE-free so they are unit-tested.
 *
 * First ask: after [USES_BEFORE_FIRST_ASK] uses spread over at least [DAYS_BEFORE_FIRST_ASK] days —
 * never on a fresh install. "Not now" allows exactly one more ask, after [USES_BEFORE_SECOND_ASK]
 * further uses. "Leave a review" and "Don't ask again" end it for good.
 */
object ReviewPromptPolicy {
    const val USES_BEFORE_FIRST_ASK = 20
    const val DAYS_BEFORE_FIRST_ASK = 7
    const val USES_BEFORE_SECOND_ASK = 100
    const val MAX_ASKS = 2
    private const val DAY_MS = 24L * 60 * 60 * 1000

    data class Decision(val state: ReviewPromptState, val ask: Boolean)

    fun onUse(state: ReviewPromptState, now: Long): Decision {
        if (state.done) return Decision(state, ask = false)
        val firstUseAt = if (state.firstUseAt == 0L) now else state.firstUseAt
        val uses = state.usesSinceAsk + 1
        val due = when (state.asks) {
            0 -> uses >= USES_BEFORE_FIRST_ASK && now - firstUseAt >= DAYS_BEFORE_FIRST_ASK * DAY_MS
            1 -> uses >= USES_BEFORE_SECOND_ASK
            else -> false
        }
        if (!due) return Decision(state.copy(firstUseAt = firstUseAt, usesSinceAsk = uses), ask = false)
        val asks = state.asks + 1
        return Decision(ReviewPromptState(firstUseAt, 0, asks, done = asks >= MAX_ASKS), ask = true)
    }

    fun finished(state: ReviewPromptState): ReviewPromptState = state.copy(done = true)
}
