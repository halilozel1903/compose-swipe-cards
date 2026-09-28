package io.github.halilozel1903.swipecards.core

import io.github.halilozel1903.swipecards.core.SwipeDirection.Down
import io.github.halilozel1903.swipecards.core.SwipeDirection.Left
import io.github.halilozel1903.swipecards.core.SwipeDirection.Right
import io.github.halilozel1903.swipecards.core.SwipeDirection.Up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SwipeDecisionTest {

    // A 400 x 600 card: the horizontal threshold is 120, the vertical one 180.
    private val w = 400f
    private val h = 600f
    private val t = SwipeThresholds(distanceFraction = 0.3f, velocity = 1_000f, minFlingFraction = 0.05f)

    private fun decide(dx: Float, dy: Float, vx: Float = 0f, vy: Float = 0f, dirs: Set<SwipeDirection> = SwipeDirection.Default) =
        SwipeMath.decide(dx, dy, vx, vy, w, h, dirs, t)

    @Test
    fun `below the distance threshold without velocity springs back`() {
        assertNull(decide(dx = 119f, dy = 0f))
        assertNull(decide(dx = -119f, dy = 0f))
        assertNull(decide(dx = 0f, dy = -179f))
    }

    @Test
    fun `at the distance threshold swipes`() {
        assertEquals(Right, decide(dx = 120f, dy = 0f))
        assertEquals(Left, decide(dx = -150f, dy = 20f))
        assertEquals(Up, decide(dx = 10f, dy = -180f))
    }

    @Test
    fun `fast fling swipes before the distance threshold`() {
        assertEquals(Right, decide(dx = 30f, dy = 0f, vx = 1_500f))
        assertEquals(Left, decide(dx = -30f, dy = 0f, vx = -1_200f))
        assertEquals(Up, decide(dx = 0f, dy = -40f, vy = -2_000f))
    }

    @Test
    fun `slow release below threshold springs back`() {
        assertNull(decide(dx = 60f, dy = 0f, vx = 999f))
    }

    @Test
    fun `a flick that barely moved the card is ignored`() {
        // minFlingFraction * width = 20
        assertNull(decide(dx = 10f, dy = 0f, vx = 5_000f))
    }

    @Test
    fun `fling against the drag direction does not swipe the other way`() {
        // Dragged a bit right, then flung left: the card has not moved left, so it springs back.
        assertNull(decide(dx = 50f, dy = 0f, vx = -3_000f))
    }

    @Test
    fun `throwing the card back towards the center cancels a swipe past the threshold`() {
        assertNull(decide(dx = 200f, dy = 0f, vx = -1_500f))
        // A slow move back keeps the decision.
        assertEquals(Right, decide(dx = 200f, dy = 0f, vx = -500f))
    }

    @Test
    fun `disabled directions never win`() {
        assertNull(decide(dx = 0f, dy = 300f)) // Down is not in the default set
        assertNull(decide(dx = 0f, dy = -300f, dirs = SwipeDirection.Horizontal))
        assertEquals(Down, decide(dx = 0f, dy = 300f, dirs = setOf(Down)))
        assertNull(decide(dx = 0f, dy = 20f, vy = 3_000f))
    }

    @Test
    fun `diagonal drags go to the dominant axis`() {
        // 200/400 = 0.5 of the width vs 200/600 = 0.33 of the height.
        assertEquals(Right, decide(dx = 200f, dy = -200f))
        // 130/400 = 0.33 vs 400/600 = 0.67.
        assertEquals(Up, decide(dx = 130f, dy = -400f))
    }

    @Test
    fun `strongest fling wins when two directions qualify`() {
        assertEquals(Up, decide(dx = 30f, dy = -40f, vx = 1_200f, vy = -2_500f))
    }

    @Test
    fun `unmeasured card or no directions never swipes`() {
        assertNull(SwipeMath.decide(500f, 0f, 5_000f, 0f, 0f, 0f, SwipeDirection.Default, t))
        assertNull(SwipeMath.decide(500f, 0f, 5_000f, 0f, w, h, emptySet(), t))
    }

    @Test
    fun `armed direction follows the distance threshold only`() {
        assertNull(SwipeMath.armedDirection(100f, 0f, w, h, SwipeDirection.Default, t))
        assertEquals(Right, SwipeMath.armedDirection(121f, 0f, w, h, SwipeDirection.Default, t))
        assertEquals(Left, SwipeMath.armedDirection(-121f, 5f, w, h, SwipeDirection.Default, t))
    }

    @Test
    fun `invalid thresholds are rejected`() {
        assertFailsWith<IllegalArgumentException> { SwipeThresholds(distanceFraction = 0f) }
        assertFailsWith<IllegalArgumentException> { SwipeThresholds(velocity = 0f) }
        assertFailsWith<IllegalArgumentException> { SwipeThresholds(distanceFraction = 0.2f, minFlingFraction = 0.3f) }
    }
}
