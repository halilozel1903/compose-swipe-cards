package io.github.halilozel1903.swipecards.core

import kotlin.math.abs

/**
 * Pure geometry behind a swipe: how far a card went in each direction, whether a release is a
 * swipe, how much it tilts, how visible its overlay label is and where it flies to.
 *
 * All offsets are relative to the card's resting position, with x growing to the right and y
 * growing downwards (screen coordinates).
 */
public object SwipeMath {

    /** How far ([dx], [dy]) moved the card in [direction]. Negative when it moved the other way. */
    public fun displacement(direction: SwipeDirection, dx: Float, dy: Float): Float = when (direction) {
        SwipeDirection.Left -> -dx
        SwipeDirection.Right -> dx
        SwipeDirection.Up -> -dy
        SwipeDirection.Down -> dy
    }

    /** The card size that thresholds in [direction] are measured against. */
    public fun axisSize(direction: SwipeDirection, width: Float, height: Float): Float =
        if (direction.isHorizontal) width else height

    /**
     * Progress towards the distance threshold in [direction], from `0` (resting or moved the other
     * way) to `1` (at or beyond the threshold). Returns `0` while the card has no size yet.
     */
    public fun progress(
        direction: SwipeDirection,
        dx: Float,
        dy: Float,
        width: Float,
        height: Float,
        thresholds: SwipeThresholds = SwipeThresholds(),
    ): Float {
        val size = axisSize(direction, width, height)
        if (size <= 0f) return 0f
        return (displacement(direction, dx, dy) / (size * thresholds.distanceFraction)).coerceIn(0f, 1f)
    }

    /**
     * The allowed direction the card leans towards the most, measured relative to each direction's
     * threshold, or `null` while it rests or only moved towards disallowed directions.
     */
    public fun leadingDirection(
        dx: Float,
        dy: Float,
        width: Float,
        height: Float,
        directions: Set<SwipeDirection> = SwipeDirection.Default,
        thresholds: SwipeThresholds = SwipeThresholds(),
    ): SwipeDirection? = directions
        .map { it to rawProgress(it, dx, dy, width, height, thresholds) }
        .filter { it.second > 0f }
        .maxByOrNull { it.second }
        ?.first

    /**
     * The progress of the [leadingDirection], `0..1`. The cards behind the top one use it to move
     * up one step as the top card leaves.
     */
    public fun stackProgress(
        dx: Float,
        dy: Float,
        width: Float,
        height: Float,
        directions: Set<SwipeDirection> = SwipeDirection.Default,
        thresholds: SwipeThresholds = SwipeThresholds(),
    ): Float {
        val leading = leadingDirection(dx, dy, width, height, directions, thresholds) ?: return 0f
        return progress(leading, dx, dy, width, height, thresholds)
    }

    /**
     * The direction the card would be swiped to if it were released now without any velocity, or
     * `null`. Used to fire the haptic tick when the finger crosses the threshold.
     */
    public fun armedDirection(
        dx: Float,
        dy: Float,
        width: Float,
        height: Float,
        directions: Set<SwipeDirection> = SwipeDirection.Default,
        thresholds: SwipeThresholds = SwipeThresholds(),
    ): SwipeDirection? = directions
        .map { it to rawProgress(it, dx, dy, width, height, thresholds) }
        .filter { it.second >= 1f - Epsilon }
        .maxByOrNull { it.second }
        ?.first

    /**
     * Decides what happens to a card released at ([dx], [dy]) with velocity ([vx], [vy]).
     *
     * 1. If the card passed the distance threshold of an allowed direction it is swiped there
     *    (the furthest one wins), unless it is being thrown back towards the center faster than
     *    the velocity threshold.
     * 2. Otherwise a fling faster than the velocity threshold in an allowed direction swipes the
     *    card, provided it moved at least [SwipeThresholds.minFlingFraction] that way.
     * 3. Otherwise `null`: the card springs back.
     */
    public fun decide(
        dx: Float,
        dy: Float,
        vx: Float,
        vy: Float,
        width: Float,
        height: Float,
        directions: Set<SwipeDirection> = SwipeDirection.Default,
        thresholds: SwipeThresholds = SwipeThresholds(),
    ): SwipeDirection? {
        if (width <= 0f || height <= 0f || directions.isEmpty()) return null

        val byDistance = armedDirection(dx, dy, width, height, directions, thresholds)
        if (byDistance != null) {
            val throwBack = -displacement(byDistance, vx, vy)
            return if (throwBack >= thresholds.velocity) null else byDistance
        }

        return directions
            .filter { direction ->
                val size = axisSize(direction, width, height)
                displacement(direction, vx, vy) >= thresholds.velocity &&
                    displacement(direction, dx, dy) >= size * thresholds.minFlingFraction
            }
            .maxByOrNull { displacement(it, vx, vy) }
    }

    /**
     * Tilt of the card in degrees, proportional to the horizontal offset: [maxDegrees] once it
     * moved a full card width, negative to the left.
     */
    public fun rotation(dx: Float, width: Float, maxDegrees: Float = 15f): Float {
        if (width <= 0f) return 0f
        return (dx / width).coerceIn(-1f, 1f) * maxDegrees
    }

    /**
     * Alpha of an overlay label for a direction whose [progress] is given. The label stays hidden
     * for the first [fadeStart] of the way so it does not flicker on small touches, then fades in
     * linearly and is fully opaque at the threshold.
     */
    public fun overlayAlpha(progress: Float, fadeStart: Float = 0.1f): Float {
        require(fadeStart >= 0f && fadeStart < 1f) { "fadeStart must be in [0, 1), was $fadeStart" }
        return ((progress - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
    }

    /**
     * How far a card must travel to be fully off screen, even when rotated: the container's
     * larger side plus the card's width and height. Pass `0` for the container when unknown.
     */
    public fun flyOffDistance(
        width: Float,
        height: Float,
        containerWidth: Float = 0f,
        containerHeight: Float = 0f,
    ): Float = maxOf(containerWidth, containerHeight, 0f) + width + height

    /**
     * Where a card swiped in [direction] animates to: [distance] (see [flyOffDistance]) along the
     * swiped axis, keeping the other axis where the finger left it.
     */
    public fun flyOffTarget(direction: SwipeDirection, dx: Float, dy: Float, distance: Float): SwipeVector =
        when (direction) {
            SwipeDirection.Left -> SwipeVector(-distance, dy)
            SwipeDirection.Right -> SwipeVector(distance, dy)
            SwipeDirection.Up -> SwipeVector(dx, -distance)
            SwipeDirection.Down -> SwipeVector(dx, distance)
        }

    /**
     * The offset that puts a card [progress] of the way to its threshold in [direction], for
     * example `1` to show the overlay label fully, `0.5` for a hint that it can be swiped.
     */
    public fun peekOffset(
        direction: SwipeDirection,
        progress: Float,
        width: Float,
        height: Float,
        thresholds: SwipeThresholds = SwipeThresholds(),
    ): SwipeVector {
        val along = axisSize(direction, width, height) * thresholds.distanceFraction * progress
        return when (direction) {
            SwipeDirection.Left -> SwipeVector(-along, 0f)
            SwipeDirection.Right -> SwipeVector(along, 0f)
            SwipeDirection.Up -> SwipeVector(0f, -along)
            SwipeDirection.Down -> SwipeVector(0f, along)
        }
    }

    // Absorbs float rounding, so a card dragged exactly to the threshold counts as armed.
    private const val Epsilon: Float = 1e-4f

    private fun rawProgress(
        direction: SwipeDirection,
        dx: Float,
        dy: Float,
        width: Float,
        height: Float,
        thresholds: SwipeThresholds,
    ): Float {
        val size = axisSize(direction, width, height)
        if (size <= 0f) return 0f
        val along = displacement(direction, dx, dy)
        // Ignore movement that is mostly along the other axis, so a diagonal drag up-right is
        // not armed for "up" just because it moved up a little.
        val across = if (direction.isHorizontal) abs(dy) else abs(dx)
        val acrossSize = if (direction.isHorizontal) height else width
        if (along <= 0f) return 0f
        if (acrossSize > 0f && across / acrossSize > along / size) return 0f
        return along / (size * thresholds.distanceFraction)
    }
}
