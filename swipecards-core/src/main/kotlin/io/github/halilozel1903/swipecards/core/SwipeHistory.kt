package io.github.halilozel1903.swipecards.core

/** One swipe: the item at [index] left the stack in [direction]. */
public data class SwipeRecord(val index: Int, val direction: SwipeDirection)

/**
 * Undo stack of swipes, newest last. When it holds [capacity] records the oldest is dropped.
 */
public class SwipeHistory(public val capacity: Int = DefaultCapacity) {

    init {
        require(capacity >= 0) { "capacity must not be negative, was $capacity" }
    }

    private val records = ArrayDeque<SwipeRecord>()

    /** Number of swipes that can be undone. */
    public val size: Int get() = records.size

    /** `true` when [pop] would return a record. */
    public val canUndo: Boolean get() = records.isNotEmpty()

    /** The swipe [pop] would undo, without removing it. */
    public fun peek(): SwipeRecord? = records.lastOrNull()

    /** Records a swipe. */
    public fun push(record: SwipeRecord) {
        if (capacity == 0) return
        if (records.size == capacity) records.removeFirst()
        records.addLast(record)
    }

    /** Removes and returns the newest swipe, or `null` when there is nothing to undo. */
    public fun pop(): SwipeRecord? = records.removeLastOrNull()

    /** Forgets every swipe. */
    public fun clear() {
        records.clear()
    }

    /** The records, oldest first. */
    public fun toList(): List<SwipeRecord> = records.toList()

    public companion object {
        /** Default number of swipes that can be undone. */
        public const val DefaultCapacity: Int = 50

        /** Rebuilds a history from [records] (oldest first), for example after process death. */
        public fun of(records: List<SwipeRecord>, capacity: Int = DefaultCapacity): SwipeHistory =
            SwipeHistory(capacity).also { history -> records.forEach(history::push) }
    }
}
