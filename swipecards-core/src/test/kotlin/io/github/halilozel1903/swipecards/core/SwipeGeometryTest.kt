package io.github.halilozel1903.swipecards.core

import io.github.halilozel1903.swipecards.core.SwipeDirection.Left
import io.github.halilozel1903.swipecards.core.SwipeDirection.Right
import io.github.halilozel1903.swipecards.core.SwipeDirection.Up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwipeGeometryTest {

    private val w = 400f
    private val h = 600f
    private val t = SwipeThresholds(distanceFraction = 0.3f)

    @Test
    fun `rotation is proportional to the horizontal offset and clamped`() {
        assertEquals(0f, SwipeMath.rotation(0f, w, 15f))
        assertEquals(7.5f, SwipeMath.rotation(200f, w, 15f))
        assertEquals(-7.5f, SwipeMath.rotation(-200f, w, 15f))
        assertEquals(15f, SwipeMath.rotation(2_000f, w, 15f))
        assertEquals(0f, SwipeMath.rotation(100f, 0f, 15f))
    }

    @Test
    fun `progress reaches one at the threshold and ignores the other way`() {
        assertEquals(0.5f, SwipeMath.progress(Right, 60f, 0f, w, h, t), 1e-5f)
        assertEquals(1f, SwipeMath.progress(Right, 500f, 0f, w, h, t))
        assertEquals(0f, SwipeMath.progress(Left, 60f, 0f, w, h, t))
        assertEquals(0.5f, SwipeMath.progress(Up, 0f, -90f, w, h, t), 1e-5f)
        assertEquals(0f, SwipeMath.progress(Right, 60f, 0f, 0f, 0f, t))
    }

    @Test
    fun `overlay alpha tracks progress after a small dead zone`() {
        assertEquals(0f, SwipeMath.overlayAlpha(0f))
        assertEquals(0f, SwipeMath.overlayAlpha(0.1f))
        assertEquals(0.5f, SwipeMath.overlayAlpha(0.55f), 1e-4f)
        assertEquals(1f, SwipeMath.overlayAlpha(1f))
        assertEquals(0.3f, SwipeMath.overlayAlpha(0.3f, fadeStart = 0f))
    }

    @Test
    fun `leading direction and stack progress`() {
        assertNull(SwipeMath.leadingDirection(0f, 0f, w, h, SwipeDirection.Default, t))
        assertEquals(Right, SwipeMath.leadingDirection(60f, 10f, w, h, SwipeDirection.Default, t))
        assertEquals(Up, SwipeMath.leadingDirection(10f, -60f, w, h, SwipeDirection.Default, t))
        assertNull(SwipeMath.leadingDirection(0f, 60f, w, h, SwipeDirection.Default, t))
        assertEquals(0.5f, SwipeMath.stackProgress(60f, 0f, w, h, SwipeDirection.Default, t), 1e-5f)
        assertEquals(0f, SwipeMath.stackProgress(0f, 90f, w, h, SwipeDirection.Default, t))
    }

    @Test
    fun `fly off target leaves the screen on the swiped axis and keeps the other`() {
        val distance = SwipeMath.flyOffDistance(w, h, containerWidth = 1_080f, containerHeight = 2_400f)
        assertEquals(2_400f + w + h, distance)
        assertEquals(w + h, SwipeMath.flyOffDistance(w, h))
        val right = SwipeMath.flyOffTarget(Right, 150f, 40f, distance)
        assertEquals(SwipeVector(distance, 40f), right)
        val left = SwipeMath.flyOffTarget(Left, -150f, 40f, distance)
        assertEquals(SwipeVector(-distance, 40f), left)
        val up = SwipeMath.flyOffTarget(Up, 25f, -200f, distance)
        assertEquals(SwipeVector(25f, -distance), up)
        assertTrue(SwipeMath.flyOffTarget(SwipeDirection.Down, 0f, 0f, distance).y > 0f)
    }

    @Test
    fun `peek offset lands at the requested progress`() {
        val half = SwipeMath.peekOffset(Right, 0.5f, w, h, t)
        assertEquals(60f, half.x, 1e-3f)
        assertEquals(0f, half.y)
        assertEquals(0.5f, SwipeMath.progress(Right, half.x, half.y, w, h, t), 1e-5f)
        val up = SwipeMath.peekOffset(Up, 1f, w, h, t)
        assertEquals(-180f, up.y, 1e-3f)
        assertEquals(Up, SwipeMath.armedDirection(up.x, up.y, w, h, SwipeDirection.Default, t))
        assertEquals(-30f, SwipeMath.peekOffset(Left, 0.25f, w, h, t).x, 1e-3f)
    }

    @Test
    fun `direction helpers`() {
        assertEquals(Left, Right.opposite)
        assertEquals(SwipeDirection.Down, Up.opposite)
        assertTrue(Left.isHorizontal)
        assertEquals(-50f, SwipeMath.displacement(Right, -50f, 0f))
        assertEquals(50f, SwipeMath.displacement(Up, 0f, -50f))
    }
}
