package io.github.halilozel1903.swipecards.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StackGeometryTest {

    private fun transform(depth: Int, progress: Float = 0f) =
        StackGeometry.transformFor(depth, progress, visibleCount = 3, scaleStep = 0.05f, offsetStep = 10f)

    @Test
    fun `top card is untransformed`() {
        assertEquals(CardTransform.Identity, transform(0))
        assertEquals(CardTransform.Identity, transform(0, progress = 0.7f))
    }

    @Test
    fun `cards behind shrink and peek out step by step`() {
        val first = transform(1)
        assertEquals(0.95f, first.scale, 1e-5f)
        assertEquals(10f, first.translationY, 1e-5f)
        assertEquals(1f, first.alpha)
        val second = transform(2)
        assertEquals(0.9f, second.scale, 1e-5f)
        assertEquals(20f, second.translationY, 1e-5f)
    }

    @Test
    fun `dragging the top card moves the others one step up`() {
        val halfway = transform(1, progress = 0.5f)
        assertEquals(0.975f, halfway.scale, 1e-5f)
        assertEquals(5f, halfway.translationY, 1e-5f)
        assertEquals(CardTransform.Identity, transform(1, progress = 1f))
        assertEquals(transform(1), transform(2, progress = 1f))
        // Progress beyond one is clamped.
        assertEquals(transform(1, progress = 1f), transform(1, progress = 3f))
    }

    @Test
    fun `the card after the visible ones fades in behind the last one`() {
        val hidden = transform(3)
        assertEquals(0f, hidden.alpha)
        assertEquals(transform(2).scale, hidden.scale, 1e-5f)
        val entering = transform(3, progress = 0.4f)
        assertEquals(0.4f, entering.alpha, 1e-5f)
        assertEquals(0f, transform(5).alpha)
    }

    @Test
    fun `indices to draw`() {
        assertEquals(0..3, StackGeometry.indicesToDraw(0, 10, 3))
        assertEquals(8..9, StackGeometry.indicesToDraw(8, 10, 3))
        assertEquals(9..9, StackGeometry.indicesToDraw(9, 10, 3))
        assertTrue(StackGeometry.indicesToDraw(10, 10, 3).isEmpty())
        assertTrue(StackGeometry.indicesToDraw(0, 0, 3).isEmpty())
    }

    @Test
    fun `invalid arguments are rejected`() {
        assertFailsWith<IllegalArgumentException> { StackGeometry.transformFor(-1, 0f, 3) }
        assertFailsWith<IllegalArgumentException> { StackGeometry.transformFor(1, 0f, 0) }
    }
}
