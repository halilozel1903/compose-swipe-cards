package io.github.halilozel1903.swipecards.core

/**
 * The four ways a card can leave the stack.
 *
 * The library gives them no meaning; by convention [Left] is "nope", [Right] is "like" and
 * [Up] is "super like". Which directions are allowed is configured per stack.
 */
public enum class SwipeDirection {
    Left,
    Right,
    Up,
    Down,
    ;

    /** `true` for [Left] and [Right]. */
    public val isHorizontal: Boolean get() = this == Left || this == Right

    /** The direction pointing the other way. */
    public val opposite: SwipeDirection
        get() = when (this) {
            Left -> Right
            Right -> Left
            Up -> Down
            Down -> Up
        }

    public companion object {
        /** Left (nope), right (like) and up (super like): the classic dating app set. */
        public val Default: Set<SwipeDirection> = setOf(Left, Right, Up)

        /** Only left and right. */
        public val Horizontal: Set<SwipeDirection> = setOf(Left, Right)
    }
}
