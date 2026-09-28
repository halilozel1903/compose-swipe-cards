package io.github.halilozel1903.swipecards.core

/** A platform independent 2D offset in pixels. */
public data class SwipeVector(val x: Float, val y: Float) {
    public companion object {
        public val Zero: SwipeVector = SwipeVector(0f, 0f)
    }
}
