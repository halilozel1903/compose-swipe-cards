package io.github.halilozel1903.swipecards.core

/**
 * When a released card counts as swiped.
 *
 * A card is swiped when it was dragged at least [distanceFraction] of the card's size along a
 * direction (width for left and right, height for up and down), **or** when it was flung faster
 * than [velocity] in that direction after moving at least [minFlingFraction] of the card's size.
 *
 * @property distanceFraction Distance threshold as a fraction of the card's width or height.
 * @property velocity Fling threshold. It uses the same unit as the velocity passed to
 *   [SwipeMath.decide]; the Compose layer passes dp per second.
 * @property minFlingFraction A fling only counts once the card moved this fraction of its size in
 *   the fling's direction, so a tiny flick does not throw a card away.
 */
public data class SwipeThresholds(
    val distanceFraction: Float = 0.3f,
    val velocity: Float = 1_000f,
    val minFlingFraction: Float = 0.05f,
) {
    init {
        require(distanceFraction > 0f && distanceFraction <= 1f) {
            "distanceFraction must be in (0, 1], was $distanceFraction"
        }
        require(velocity > 0f) { "velocity must be positive, was $velocity" }
        require(minFlingFraction >= 0f && minFlingFraction <= distanceFraction) {
            "minFlingFraction must be in [0, distanceFraction], was $minFlingFraction"
        }
    }
}
