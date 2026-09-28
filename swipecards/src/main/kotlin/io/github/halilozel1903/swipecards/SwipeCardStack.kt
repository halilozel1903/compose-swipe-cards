package io.github.halilozel1903.swipecards

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import io.github.halilozel1903.swipecards.core.StackGeometry
import io.github.halilozel1903.swipecards.core.SwipeDirection
import io.github.halilozel1903.swipecards.core.SwipeMath
import io.github.halilozel1903.swipecards.core.SwipeThresholds
import io.github.halilozel1903.swipecards.core.ThresholdTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * A Tinder-style stack of swipeable cards.
 *
 * The item at [SwipeCardState.currentIndex] is on top and can be dragged; the next
 * [visibleCards] - 1 items are drawn behind it, smaller and peeking out below. A released card is
 * swiped when it passed the distance threshold or was flung faster than the velocity threshold
 * (see [thresholds]); otherwise it springs back. Swiped cards fly off screen, then [onSwiped] is
 * called and the next card is on top.
 *
 * Every card fills the stack, so give the stack a size (for example `Modifier.fillMaxSize()` or
 * `Modifier.aspectRatio(0.7f)`).
 *
 * @param items All items of the deck. Items before [SwipeCardState.currentIndex] are already swiped.
 * @param state Current index, undo history and programmatic [SwipeCardState.swipe] / [SwipeCardState.undo].
 * @param onSwiped Called once a card has left the stack, after its fly-off animation.
 * @param modifier Modifier for the stack.
 * @param directions Directions the user can swipe in. [SwipeCardState.swipe] may use any direction.
 * @param thresholds Distance and velocity thresholds deciding when a release is a swipe.
 * @param visibleCards Number of cards drawn at rest, including the top one.
 * @param stackOffset How far each card peeks out below the one in front of it.
 * @param scaleStep How much smaller each card is than the one in front of it.
 * @param maxRotation Tilt of the top card in degrees once it moved a full card width sideways.
 * @param hapticFeedback Whether to play a haptic tick when a drag crosses a threshold.
 * @param onUndo Called when [SwipeCardState.undo] brings a card back, before its animation.
 * @param itemKey Stable key for an item, so card content keeps its state when the stack moves.
 * @param accessibilityLabel Label of the accessibility action that swipes in a direction.
 * @param undoAccessibilityLabel Label of the undo accessibility action, or `null` to omit it.
 * @param overlay Label drawn over the top card for the direction it leans to; `alpha` tracks the
 *   swipe progress and reaches `1` at the threshold.
 * @param emptyContent Shown when every card has been swiped.
 * @param content A card for an item.
 */
@Composable
public fun <T> SwipeCardStack(
    items: List<T>,
    state: SwipeCardState = rememberSwipeCardState(),
    onSwiped: (item: T, direction: SwipeDirection) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    directions: Set<SwipeDirection> = SwipeDirection.Default,
    thresholds: SwipeThresholds = SwipeCardDefaults.Thresholds,
    visibleCards: Int = SwipeCardDefaults.VisibleCards,
    stackOffset: Dp = SwipeCardDefaults.StackOffset,
    scaleStep: Float = SwipeCardDefaults.ScaleStep,
    maxRotation: Float = SwipeCardDefaults.MaxRotation,
    hapticFeedback: Boolean = true,
    onUndo: (item: T, direction: SwipeDirection) -> Unit = { _, _ -> },
    itemKey: ((item: T) -> Any)? = null,
    accessibilityLabel: (SwipeDirection) -> String = SwipeCardDefaults::accessibilityLabel,
    undoAccessibilityLabel: String? = SwipeCardDefaults.UndoAccessibilityLabel,
    overlay: @Composable BoxScope.(direction: SwipeDirection, alpha: Float) -> Unit = { direction, alpha ->
        SwipeCardDefaults.Overlay(direction, alpha)
    },
    emptyContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.(item: T) -> Unit,
) {
    require(visibleCards >= 1) { "visibleCards must be at least 1, was $visibleCards" }

    val currentItems by rememberUpdatedState(items)
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val view = LocalView.current
    // Owned by the stack, not the top card: undo changes the top card, which would cancel a scope
    // remembered inside it.
    val scope = rememberCoroutineScope()

    SideEffect {
        state.itemCount = items.size
        state.directions = directions
        state.thresholds = thresholds
        state.containerSize = { IntSize(view.rootView.width, view.rootView.height) }
        state.onSwipe = { record ->
            currentItems.getOrNull(record.index)?.let { currentOnSwiped(it, record.direction) }
        }
        state.onUndo = { record ->
            currentItems.getOrNull(record.index)?.let { currentOnUndo(it, record.direction) }
        }
    }

    val offsetStepPx = with(LocalDensity.current) { stackOffset.toPx() }

    Box(
        modifier = modifier.onSizeChanged { state.cardSize = it },
        contentAlignment = Alignment.Center,
    ) {
        val currentIndex = state.currentIndex
        val indices = StackGeometry.indicesToDraw(currentIndex, items.size, visibleCards)
        if (indices.isEmpty()) {
            emptyContent()
        }
        for (index in indices.reversed()) {
            val item = items[index]
            key(itemKey?.invoke(item) ?: index) {
                val depth = index - currentIndex
                val isTop = depth == 0
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isTop) {
                                Modifier
                                    .swipeGestures(state, hapticFeedback)
                                    .swipeSemantics(state, scope, directions, accessibilityLabel, undoAccessibilityLabel)
                            } else {
                                // Only the top card is reachable with TalkBack.
                                Modifier.clearAndSetSemantics { }
                            },
                        )
                        .graphicsLayer {
                            // Every property is set in both branches: a card keeps its layer when
                            // it moves from behind to the top.
                            if (isTop) {
                                val offset = state.offset
                                translationX = offset.x
                                translationY = offset.y
                                rotationZ = SwipeMath.rotation(offset.x, size.width, maxRotation)
                                transformOrigin = TransformOrigin.Center
                                scaleX = 1f
                                scaleY = 1f
                                alpha = 1f
                            } else {
                                val transform = StackGeometry.transformFor(
                                    depth = depth,
                                    dragProgress = state.stackProgress,
                                    visibleCount = visibleCards,
                                    scaleStep = scaleStep,
                                    offsetStep = offsetStepPx,
                                )
                                translationX = 0f
                                translationY = transform.translationY
                                rotationZ = 0f
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                scaleX = transform.scale
                                scaleY = transform.scale
                                alpha = transform.alpha
                            }
                        },
                ) {
                    content(item)
                    if (isTop) {
                        SwipeOverlayHost(state, overlay)
                    }
                }
            }
        }
    }
}

/** Reads the drag state in its own scope, so only the overlay recomposes while dragging. */
@Composable
private fun SwipeOverlayHost(
    state: SwipeCardState,
    overlay: @Composable BoxScope.(direction: SwipeDirection, alpha: Float) -> Unit,
) {
    val direction = state.swipeDirection ?: return
    val alpha = SwipeMath.overlayAlpha(state.progressTowards(direction))
    if (alpha <= 0f) return
    Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        overlay(direction, alpha)
    }
}

@Composable
private fun Modifier.swipeGestures(state: SwipeCardState, hapticFeedback: Boolean): Modifier {
    val haptics = LocalHapticFeedback.current
    val currentHaptics by rememberUpdatedState(haptics)
    val hapticsEnabled by rememberUpdatedState(hapticFeedback)
    return pointerInput(state) {
        val velocityTracker = VelocityTracker()
        val thresholdTracker = ThresholdTracker()
        try {
            coroutineScope {
                var dragOffset = Offset.Zero
                var active = false
                detectDragGestures(
                    onDragStart = {
                        active = state.canDrag
                        if (active) {
                            velocityTracker.resetTracking()
                            thresholdTracker.reset()
                            dragOffset = state.offset
                            state.isDragging = true
                        }
                    },
                    onDragEnd = {
                        if (active) {
                            active = false
                            val velocity = velocityTracker.calculateVelocity()
                            val finalOffset = dragOffset
                            launch { state.release(finalOffset, Offset(velocity.x, velocity.y), density) }
                        }
                    },
                    onDragCancel = {
                        if (active) {
                            active = false
                            val finalOffset = dragOffset
                            launch { state.release(finalOffset, Offset.Zero, density) }
                        }
                    },
                ) { change, dragAmount ->
                    if (!active) return@detectDragGestures
                    change.consume()
                    // The modifier sits before graphicsLayer, so positions are not affected by the
                    // card's own translation and give a correct velocity.
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    dragOffset += dragAmount
                    val target = dragOffset
                    launch { state.dragTo(target) }

                    val armed = SwipeMath.armedDirection(
                        dx = target.x,
                        dy = target.y,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        directions = state.directions,
                        thresholds = state.thresholds,
                    )
                    if (thresholdTracker.update(armed) && hapticsEnabled) {
                        currentHaptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    }
                }
            }
        } finally {
            // The card left the composition mid-drag.
            state.isDragging = false
        }
    }
}

private fun Modifier.swipeSemantics(
    state: SwipeCardState,
    scope: CoroutineScope,
    directions: Set<SwipeDirection>,
    accessibilityLabel: (SwipeDirection) -> String,
    undoAccessibilityLabel: String?,
): Modifier {
    val canUndo = state.canUndo
    return semantics {
        customActions = buildList {
            SwipeDirection.entries.filter { it in directions }.forEach { direction ->
                add(
                    CustomAccessibilityAction(accessibilityLabel(direction)) {
                        scope.launch { state.swipe(direction) }
                        true
                    },
                )
            }
            if (undoAccessibilityLabel != null && canUndo) {
                add(
                    CustomAccessibilityAction(undoAccessibilityLabel) {
                        scope.launch { state.undo() }
                        true
                    },
                )
            }
        }
    }
}
