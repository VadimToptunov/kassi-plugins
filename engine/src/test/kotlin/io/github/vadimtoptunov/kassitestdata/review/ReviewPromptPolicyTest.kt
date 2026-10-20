package io.github.vadimtoptunov.kassitestdata.review

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReviewPromptPolicyTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_700_000_000_000L

    /** Applies [n] uses at time [at] and returns the final decision. */
    private fun use(start: ReviewPromptState, n: Int, at: Long): ReviewPromptPolicy.Decision {
        var state = start
        var last = ReviewPromptPolicy.Decision(state, false)
        repeat(n) {
            last = ReviewPromptPolicy.onUse(state, at)
            state = last.state
        }
        return last
    }

    @Test
    fun `never asks on a fresh install, however heavily it is used`() {
        val d = use(ReviewPromptState(), 500, t0)
        assertFalse(d.ask)
        assertEquals(0, d.state.asks)
    }

    @Test
    fun `never asks before the use threshold, however old the install`() {
        val d = use(ReviewPromptState(firstUseAt = t0), ReviewPromptPolicy.USES_BEFORE_FIRST_ASK - 1, t0 + 30 * day)
        assertFalse(d.ask)
    }

    @Test
    fun `asks exactly once the use and age thresholds are both met`() {
        val before = use(ReviewPromptState(firstUseAt = t0), ReviewPromptPolicy.USES_BEFORE_FIRST_ASK - 1, t0 + 8 * day)
        assertFalse(before.ask)
        val at = ReviewPromptPolicy.onUse(before.state, t0 + 8 * day)
        assertTrue(at.ask)
        assertEquals(1, at.state.asks)
        assertEquals(0, at.state.usesSinceAsk)
        assertFalse(at.state.done)
        // The very next use must not ask again.
        assertFalse(ReviewPromptPolicy.onUse(at.state, t0 + 8 * day).ask)
    }

    @Test
    fun `after Not now, asks a second and last time after many more uses, then never again`() {
        val afterFirst = ReviewPromptState(firstUseAt = t0, asks = 1)
        val notYet = use(afterFirst, ReviewPromptPolicy.USES_BEFORE_SECOND_ASK - 1, t0 + 60 * day)
        assertFalse(notYet.ask)
        val second = ReviewPromptPolicy.onUse(notYet.state, t0 + 60 * day)
        assertTrue(second.ask)
        assertTrue(second.state.done)
        assertFalse(use(second.state, 1000, t0 + 365 * day).ask)
    }

    @Test
    fun `finished state never asks and keeps its counters`() {
        val done = ReviewPromptPolicy.finished(ReviewPromptState(firstUseAt = t0, usesSinceAsk = 3))
        assertTrue(done.done)
        val d = use(done, 1000, t0 + 365 * day)
        assertFalse(d.ask)
        assertEquals(done, d.state)
    }
}
