package io.github.halilozel1903.swipecards.core

/**
 * How a card behind the top one is drawn.
 *
 * @property scale Uniform scale, `1` for the top card.
 * @property translationY Downward shift in pixels, `0` for the top card.
 * @property alpha `1` for visible cards; the card entering the visible stack fades in.
 */
public data class CardTransform(
    val scale: Float,
    val translationY: Float,
    val alpha: Float,
) {
    public companion object {
        public val Identity: CardTransform = CardTransform(scale = 1f, translationY = 0f, alpha = 1f)
    }
}

/** Depth effect for the cards under the top one. */
public object StackGeometry {

    /**
     * Transform of the card at [depth] (`0` is the top card) while the top card is [dragProgress]
     * (`0..1`) of the way to its threshold. Every card behind moves up by that fraction of one step,
     * so when the top card leaves, the next one is already in place.
     *
     * @param visibleCount How many cards are drawn at rest. The card at depth [visibleCount] fades
     *   in while the top card is dragged, deeper cards are invisible.
     * @param scaleStep How much smaller each card is than the one in front of it.
     * @param offsetStep How far, in pixels, each card peeks out below the one in front of it.
     */
    public fun transformFor(
        depth: Int,
        dragProgress: Float,
        visibleCount: Int,
        scaleStep: Float = 0.05f,
        offsetStep: Float = 0f,
    ): CardTransform {
        require(depth >= 0) { "depth must not be negative, was $depth" }
        require(visibleCount >= 1) { "visibleCount must be at least 1, was $visibleCount" }
        if (depth == 0) return CardTransform.Identity

        val progress = dragProgress.coerceIn(0f, 1f)
        val effectiveDepth = depth - progress
        // Cards deeper than the last visible one stack exactly behind it.
        val shownDepth = effectiveDepth.coerceAtMost((visibleCount - 1).toFloat())
        val scale = (1f - scaleStep * shownDepth).coerceAtLeast(0f)
        val alpha = (visibleCount - effectiveDepth).coerceIn(0f, 1f)
        return CardTransform(scale = scale, translationY = offsetStep * shownDepth, alpha = alpha)
    }

    /**
     * Indices of the items to compose, deepest first so the top card is drawn last: the current
     * card plus up to [visibleCount] behind it (one more than visible, so it can fade in).
     */
    public fun indicesToDraw(currentIndex: Int, itemCount: Int, visibleCount: Int): IntRange {
        require(visibleCount >= 1) { "visibleCount must be at least 1, was $visibleCount" }
        if (currentIndex >= itemCount) return IntRange.EMPTY
        val start = currentIndex.coerceAtLeast(0)
        val end = (start + visibleCount).coerceAtMost(itemCount - 1)
        return start..end
    }
}
