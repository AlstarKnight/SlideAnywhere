package io.github.alstarknight.slidex

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A card being dragged by its handle in a page's list. While the drag lasts the page shows its cards in [order],
 * swapping the dragged card with a neighbour once it is pulled past the neighbour's middle.
 */
@Stable
class CardDrag(private val list: LazyListState) {
    var cardId by mutableStateOf<String?>(null)
        private set

    /** Card ids in the order shown while dragging. */
    var order by mutableStateOf(emptyList<String>())
        private set

    private var startIndex = 0
    private var startOffset = 0
    private var moved by mutableFloatStateOf(0f)

    fun start(id: String, ids: List<String>) {
        startOffset = itemInfo(id)?.offset ?: return
        startIndex = ids.indexOf(id)
        cardId = id
        order = ids
        moved = 0f
    }

    fun drag(dy: Float) {
        val id = cardId ?: return
        val info = itemInfo(id) ?: return
        moved += dy
        val top = startOffset + moved
        val index = order.indexOf(id)
        val next = order.getOrNull(index + 1)?.let(::itemInfo)
        val previous = order.getOrNull(index - 1)?.let(::itemInfo)
        when {
            next != null && top + info.size > next.offset + next.size / 2 -> order = order.move(index, index + 1)
            previous != null && top < previous.offset + previous.size / 2 -> order = order.move(index, index - 1)
        }
    }

    /** How far [id] is drawn from its place in the list, so the dragged card stays under the finger. */
    fun offsetOf(id: String): Float =
        if (id != cardId) 0f else startOffset + moved - (itemInfo(id)?.offset ?: startOffset)

    /** Ends the drag; returns the dragged card's id and how many places it moved, or null when nothing was dragged. */
    fun end(): Pair<String, Int>? {
        val id = cardId ?: return null
        val result = id to order.indexOf(id) - startIndex
        cancel()
        return result
    }

    fun cancel() {
        cardId = null
        order = emptyList()
        moved = 0f
    }

    private fun itemInfo(id: String): LazyListItemInfo? = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
}
