package io.github.halilozel1903.swipecards.core

import io.github.halilozel1903.swipecards.core.SwipeDirection.Left
import io.github.halilozel1903.swipecards.core.SwipeDirection.Right
import io.github.halilozel1903.swipecards.core.SwipeDirection.Up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwipeHistoryTest {

    @Test
    fun `undo returns swipes newest first`() {
        val history = SwipeHistory()
        assertFalse(history.canUndo)
        assertNull(history.pop())
        history.push(SwipeRecord(0, Right))
        history.push(SwipeRecord(1, Left))
        assertEquals(SwipeRecord(1, Left), history.peek())
        assertEquals(SwipeRecord(1, Left), history.pop())
        assertEquals(SwipeRecord(0, Right), history.pop())
        assertFalse(history.canUndo)
    }

    @Test
    fun `capacity drops the oldest swipe`() {
        val history = SwipeHistory(capacity = 2)
        history.push(SwipeRecord(0, Right))
        history.push(SwipeRecord(1, Left))
        history.push(SwipeRecord(2, Up))
        assertEquals(listOf(SwipeRecord(1, Left), SwipeRecord(2, Up)), history.toList())
    }

    @Test
    fun `zero capacity disables undo`() {
        val history = SwipeHistory(capacity = 0)
        history.push(SwipeRecord(0, Right))
        assertFalse(history.canUndo)
    }

    @Test
    fun `rebuild and clear`() {
        val records = listOf(SwipeRecord(0, Right), SwipeRecord(1, Up))
        val history = SwipeHistory.of(records)
        assertEquals(records, history.toList())
        assertTrue(history.canUndo)
        history.clear()
        assertEquals(0, history.size)
    }

    @Test
    fun `threshold tracker fires once per crossing`() {
        val tracker = ThresholdTracker()
        assertFalse(tracker.update(null))
        assertTrue(tracker.update(Right))
        assertFalse(tracker.update(Right))
        assertFalse(tracker.update(null))
        assertTrue(tracker.update(Right))
        assertTrue(tracker.update(Up))
        tracker.reset()
        assertNull(tracker.armed)
        assertTrue(tracker.update(Up))
    }
}
