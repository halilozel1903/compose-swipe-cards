package io.github.halilozel1903.swipecards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.halilozel1903.swipecards.core.SwipeDirection
import io.github.halilozel1903.swipecards.core.SwipeHistory
import io.github.halilozel1903.swipecards.core.SwipeMath
import io.github.halilozel1903.swipecards.core.SwipeRecord
import io.github.halilozel1903.swipecards.core.SwipeThresholds
import io.github.halilozel1903.swipecards.core.SwipeVector
import kotlinx.coroutines.flow.first

/**
 * Creates and remembers a [SwipeCardState]. The current index and the undo history survive
 * configuration changes and process death.
 *
 * @param initialIndex Index of the item shown on top at first.
 * @param undoCapacity How many swipes [SwipeCardState.undo] can take back.
 */
@Composable
public fun rememberSwipeCardState(
    initialIndex: Int = 0,
    undoCapacity: Int = SwipeHistory.DefaultCapacity,
): SwipeCardState = rememberSaveable(undoCapacity, saver = SwipeCardState.saver(undoCapacity)) {
    SwipeCardState(initialIndex, undoCapacity)
}

/**
 * State of a [SwipeCardStack]: which item is on top, where the top card is, and the undo history.
 *
 * Use [swipe] and [undo] to drive the stack from buttons, and read [currentIndex], [canUndo],
 * [swipeDirection] and [swipeProgress] to update the UI around it. All properties are backed by
 * snapshot state, so reading them in composition recomposes when they change.
 *
 * @param initialIndex Index of the item shown on top at first.
 * @param undoCapacity How many swipes [undo] can take back.
 */
@Stable
public class SwipeCardState(
    initialIndex: Int = 0,
    undoCapacity: Int = SwipeHistory.DefaultCapacity,
) {
    init {
        require(initialIndex >= 0) { "initialIndex must not be negative, was $initialIndex" }
    }

    private val history = SwipeHistory(undoCapacity)

    // Mirrors history.size so reads of the history are observable.
    private var historyVersion by mutableIntStateOf(0)

    // Bumped by jumpTo() so an interrupted animation does not commit into the new position.
    private var generation = 0

    // Replaced (not snapped) after every swipe, so committing never needs to suspend.
    private var offsetAnimatable by mutableStateOf(newAnimatable(Offset.Zero))

    /** Index of the item on top of the stack. Equals the item count once every card is swiped. */
    public var currentIndex: Int by mutableIntStateOf(initialIndex)
        private set

    /** Offset of the top card from its resting position, in pixels. */
    public val offset: Offset get() = offsetAnimatable.value

    /** `true` while a card flies off or comes back after [undo]. Drags are ignored meanwhile. */
    public var isAnimating: Boolean by mutableStateOf(false)
        private set

    /** `true` while the user drags the top card. */
    public var isDragging: Boolean by mutableStateOf(false)
        internal set

    /** `true` when every item has been swiped. */
    public val isExhausted: Boolean get() = currentIndex >= itemCount

    /** `true` when [undo] would bring a card back. */
    public val canUndo: Boolean get() = historyVersion >= 0 && history.canUndo

    /** The swipe [undo] would take back, or `null`. */
    public val lastSwipe: SwipeRecord? get() = if (historyVersion >= 0) history.peek() else null

    /** Swipes that can be undone, oldest first. */
    public val swipes: List<SwipeRecord> get() = if (historyVersion >= 0) history.toList() else emptyList()

    /**
     * The direction the top card is leaning to (or flying to), limited to the stack's allowed
     * directions, or `null` while it rests.
     */
    public val swipeDirection: SwipeDirection?
        get() = flyingDirection ?: SwipeMath.leadingDirection(
            offset.x, offset.y, cardSize.width.toFloat(), cardSize.height.toFloat(), directions, thresholds,
        )

    /** Progress of the top card towards the threshold of [swipeDirection], `0..1`. */
    public val swipeProgress: Float
        get() {
            if (flyingDirection != null) return 1f
            val direction = swipeDirection ?: return 0f
            return progressTowards(direction)
        }

    // Configured by SwipeCardStack.
    internal var itemCount: Int by mutableIntStateOf(Int.MAX_VALUE)
    internal var cardSize: IntSize by mutableStateOf(IntSize.Zero)
    internal var directions: Set<SwipeDirection> by mutableStateOf(SwipeDirection.Default)
    internal var thresholds: SwipeThresholds by mutableStateOf(SwipeThresholds())
    internal var containerSize: () -> IntSize = { IntSize.Zero }
    internal var onSwipe: (SwipeRecord) -> Unit = {}
    internal var onUndo: (SwipeRecord) -> Unit = {}

    // Set while a swipe animation runs, so the overlay and the cards behind stay at full progress.
    private var flyingDirection: SwipeDirection? by mutableStateOf(null)

    internal val canDrag: Boolean get() = !isAnimating && !isExhausted

    /**
     * Swipes the top card in [direction] with the fly-off animation, as if the user had thrown it.
     * [direction] does not have to be one of the stack's allowed drag directions.
     *
     * Suspends until the card is off screen (and until the stack has been laid out). Returns
     * `false`, doing nothing, when the stack is empty, the user is dragging, or another swipe or
     * undo is running.
     */
    public suspend fun swipe(direction: SwipeDirection): Boolean {
        if (!canDrag || isDragging) return false
        awaitCardSize()
        if (!canDrag || isDragging) return false
        flyOff(direction, velocity = Offset.Zero)
        return true
    }

    /**
     * Brings the last swiped card back on top with an animation, from the side it left through.
     * Returns `false` when there is nothing to undo, the user is dragging, or another swipe or
     * undo is running.
     */
    public suspend fun undo(): Boolean {
        if (isAnimating || isDragging) return false
        val record = history.pop() ?: return false
        historyVersion++
        val token = generation
        isAnimating = true
        val start = SwipeMath.flyOffTarget(record.direction, 0f, 0f, flyOffDistance()).toOffset()
        val animatable = newAnimatable(start)
        currentIndex = record.index
        offsetAnimatable = animatable
        onUndo(record)
        try {
            animatable.animateTo(Offset.Zero, UndoSpec)
        } finally {
            if (token == generation && offsetAnimatable === animatable && animatable.value != Offset.Zero) {
                offsetAnimatable = newAnimatable(Offset.Zero)
            }
            isAnimating = false
        }
        return true
    }

    /**
     * Moves the top card [progress] of the way to its threshold in [direction] without swiping it,
     * for onboarding hints ("you can swipe this") or screenshots. `1` shows the overlay label
     * fully. Call [reset] to send it back.
     */
    public suspend fun peek(direction: SwipeDirection, progress: Float = 0.5f, animate: Boolean = true) {
        if (!canDrag || isDragging) return
        val size = awaitCardSize()
        val target = SwipeMath.peekOffset(
            direction, progress, size.width.toFloat(), size.height.toFloat(), thresholds,
        ).toOffset()
        if (animate) offsetAnimatable.animateTo(target, SettleSpec) else offsetAnimatable.snapTo(target)
    }

    /** Springs the top card back to its resting position. */
    public suspend fun reset() {
        if (isAnimating) return
        offsetAnimatable.animateTo(Offset.Zero, SettleSpec)
    }

    /**
     * Shows the item at [index] on top without animation and clears the undo history, for example
     * to start the deck over.
     */
    public fun jumpTo(index: Int) {
        require(index >= 0) { "index must not be negative, was $index" }
        generation++
        flyingDirection = null
        isAnimating = false
        isDragging = false
        history.clear()
        historyVersion++
        currentIndex = index
        offsetAnimatable = newAnimatable(Offset.Zero)
    }

    internal suspend fun dragTo(target: Offset) {
        offsetAnimatable.snapTo(target)
    }

    /**
     * Ends a drag at [finalOffset] with [velocity] in px/s; [density] converts it to the dp/s the
     * thresholds use.
     */
    internal suspend fun release(finalOffset: Offset, velocity: Offset, density: Float) {
        isDragging = false
        if (!canDrag) return
        offsetAnimatable.snapTo(finalOffset)
        val direction = SwipeMath.decide(
            dx = finalOffset.x,
            dy = finalOffset.y,
            vx = velocity.x / density,
            vy = velocity.y / density,
            width = cardSize.width.toFloat(),
            height = cardSize.height.toFloat(),
            directions = directions,
            thresholds = thresholds,
        )
        if (direction != null) {
            flyOff(direction, velocity)
        } else {
            offsetAnimatable.animateTo(Offset.Zero, SettleSpec, initialVelocity = velocity)
        }
    }

    internal fun progressTowards(direction: SwipeDirection): Float {
        if (flyingDirection == direction) return 1f
        return SwipeMath.progress(
            direction, offset.x, offset.y, cardSize.width.toFloat(), cardSize.height.toFloat(), thresholds,
        )
    }

    /** Progress used by the cards behind the top one; any direction counts. */
    internal val stackProgress: Float
        get() {
            if (flyingDirection != null) return 1f
            return SwipeMath.stackProgress(
                offset.x, offset.y, cardSize.width.toFloat(), cardSize.height.toFloat(),
                SwipeDirection.entries.toSet(), thresholds,
            )
        }

    private suspend fun flyOff(direction: SwipeDirection, velocity: Offset) {
        val token = generation
        val index = currentIndex
        val animatable = offsetAnimatable
        isAnimating = true
        flyingDirection = direction
        val start = animatable.value
        val target = SwipeMath.flyOffTarget(direction, start.x, start.y, flyOffDistance()).toOffset()
        try {
            animatable.animateTo(target, FlyOffSpec, initialVelocity = velocity)
        } finally {
            // Commit even when cancelled, so the stack never keeps a half-thrown card on top.
            if (token == generation) {
                history.push(SwipeRecord(index, direction))
                historyVersion++
                currentIndex = index + 1
                offsetAnimatable = newAnimatable(Offset.Zero)
                flyingDirection = null
                isAnimating = false
                onSwipe(SwipeRecord(index, direction))
            }
        }
    }

    private fun flyOffDistance(): Float {
        val container = containerSize()
        return SwipeMath.flyOffDistance(
            width = cardSize.width.toFloat(),
            height = cardSize.height.toFloat(),
            containerWidth = container.width.toFloat(),
            containerHeight = container.height.toFloat(),
        )
    }

    private suspend fun awaitCardSize(): IntSize =
        snapshotFlow { cardSize }.first { it.width > 0 && it.height > 0 }

    public companion object {
        private val FlyOffSpec = tween<Offset>(durationMillis = 300, easing = LinearEasing)
        private val SettleSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Offset.VisibilityThreshold,
        )
        private val UndoSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Offset.VisibilityThreshold,
        )

        private fun newAnimatable(initial: Offset): Animatable<Offset, AnimationVector2D> =
            Animatable(initial, Offset.VectorConverter, Offset.VisibilityThreshold)

        private fun SwipeVector.toOffset(): Offset = Offset(x, y)

        /**
         * [Saver] storing the current index and the undo history as a flat list of ints.
         */
        public fun saver(undoCapacity: Int = SwipeHistory.DefaultCapacity): Saver<SwipeCardState, Any> =
            listSaver(
                save = { state ->
                    buildList {
                        add(state.currentIndex)
                        state.history.toList().forEach { record ->
                            add(record.index)
                            add(record.direction.ordinal)
                        }
                    }
                },
                restore = { saved ->
                    SwipeCardState(saved.first(), undoCapacity).apply {
                        saved.drop(1).chunked(2).forEach { (index, ordinal) ->
                            history.push(SwipeRecord(index, SwipeDirection.entries[ordinal]))
                        }
                        historyVersion++
                    }
                },
            )
    }
}
