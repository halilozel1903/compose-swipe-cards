package io.github.halilozel1903.swipecards

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.swipecards.core.SwipeDirection
import io.github.halilozel1903.swipecards.core.SwipeThresholds

/** Default values and building blocks used by [SwipeCardStack]. */
public object SwipeCardDefaults {

    /** 30 % of the card's size, or a fling faster than 1000 dp/s. */
    public val Thresholds: SwipeThresholds = SwipeThresholds(distanceFraction = 0.3f, velocity = 1_000f)

    /** Cards drawn at rest, including the top one. */
    public const val VisibleCards: Int = 3

    /** How far each card peeks out below the one in front of it. */
    public val StackOffset: Dp = 12.dp

    /** How much smaller each card is than the one in front of it. */
    public const val ScaleStep: Float = 0.05f

    /** Tilt of the top card, in degrees, once it moved a full card width sideways. */
    public const val MaxRotation: Float = 15f

    /** Accessibility action label for undo. */
    public const val UndoAccessibilityLabel: String = "Undo last swipe"

    /** Accessibility action label for swiping in [direction]. */
    public fun accessibilityLabel(direction: SwipeDirection): String = when (direction) {
        SwipeDirection.Left -> "Nope"
        SwipeDirection.Right -> "Like"
        SwipeDirection.Up -> "Super like"
        SwipeDirection.Down -> "Skip"
    }

    /** Text of the overlay label for [direction]. */
    public fun overlayText(direction: SwipeDirection): String = when (direction) {
        SwipeDirection.Left -> "NOPE"
        SwipeDirection.Right -> "LIKE"
        SwipeDirection.Up -> "SUPER"
        SwipeDirection.Down -> "SKIP"
    }

    /** Color of the overlay label for [direction]. */
    public fun overlayColor(direction: SwipeDirection): Color = when (direction) {
        SwipeDirection.Left -> Color(0xFFF43F5E)
        SwipeDirection.Right -> Color(0xFF22C55E)
        SwipeDirection.Up -> Color(0xFF3B82F6)
        SwipeDirection.Down -> Color(0xFF94A3B8)
    }

    /**
     * The stamp-like label drawn over the top card: LIKE top left, NOPE top right, SUPER at the
     * bottom and SKIP at the top.
     *
     * @param alpha Opacity, tracking the swipe progress.
     */
    @Composable
    public fun Overlay(
        direction: SwipeDirection,
        alpha: Float,
        modifier: Modifier = Modifier,
        text: String = overlayText(direction),
        color: Color = overlayColor(direction),
    ) {
        val alignment = when (direction) {
            SwipeDirection.Right -> Alignment.TopStart
            SwipeDirection.Left -> Alignment.TopEnd
            SwipeDirection.Up -> Alignment.BottomCenter
            SwipeDirection.Down -> Alignment.TopCenter
        }
        val rotation = when (direction) {
            SwipeDirection.Right -> -15f
            SwipeDirection.Left -> 15f
            SwipeDirection.Up -> -8f
            SwipeDirection.Down -> 0f
        }
        Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = alignment) {
            BasicText(
                text = text,
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        rotationZ = rotation
                    }
                    .border(4.dp, color, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                style = TextStyle(
                    color = color,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                ),
            )
        }
    }
}
