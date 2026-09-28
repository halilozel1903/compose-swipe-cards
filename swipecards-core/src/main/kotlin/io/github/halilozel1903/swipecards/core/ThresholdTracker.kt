package io.github.halilozel1903.swipecards.core

/**
 * Remembers which direction a drag is armed for and reports when it changes, so the UI can play
 * exactly one haptic tick per threshold crossing instead of one per drag event.
 */
public class ThresholdTracker {

    /** The direction the drag is currently armed for, or `null`. */
    public var armed: SwipeDirection? = null
        private set

    /**
     * Feeds the latest [SwipeMath.armedDirection]. Returns `true` when the drag has just crossed
     * into a direction's threshold (not when it drops back below it).
     */
    public fun update(direction: SwipeDirection?): Boolean {
        val crossed = direction != null && direction != armed
        armed = direction
        return crossed
    }

    /** Forgets the armed direction, for example when a new drag starts. */
    public fun reset() {
        armed = null
    }
}
