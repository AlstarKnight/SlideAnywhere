package io.github.alstarknight.slidex

fun PanelConfig.addPage(): PanelConfig = copy(pages = pages + Page())

/** Removes a page, but always keeps at least one. */
fun PanelConfig.removePage(index: Int): PanelConfig =
    if (pages.size <= 1) this else copy(pages = pages.filterIndexed { i, _ -> i != index })

fun PanelConfig.movePage(index: Int, offset: Int): PanelConfig = copy(pages = pages.move(index, index + offset))

fun PanelConfig.addCard(pageIndex: Int, card: Card): PanelConfig =
    copy(pages = pages.mapIndexed { i, page -> if (i == pageIndex) page.copy(cards = page.cards + card) else page })

fun PanelConfig.removeCard(cardId: String): PanelConfig =
    copy(pages = pages.map { page -> page.copy(cards = page.cards.filterNot { it.id == cardId }) })

/** Moves a card up (negative offset) or down within its page. */
fun PanelConfig.moveCard(cardId: String, offset: Int): PanelConfig =
    copy(pages = pages.map { page ->
        val index = page.cards.indexOfFirst { it.id == cardId }
        if (index < 0) page else page.copy(cards = page.cards.move(index, index + offset))
    })

fun PanelConfig.renameCard(cardId: String, title: String): PanelConfig = mapCard(cardId) {
    when (it) {
        is GridCard -> it.copy(title = title)
        is NoteCard -> it.copy(title = title)
        is SystemCard -> it.copy(title = title)
        is UsageCard -> it.copy(title = title)
        is WidgetCard -> it.copy(title = title)
    }
}

fun PanelConfig.setWidgetHeight(cardId: String, heightDp: Int): PanelConfig = mapCard(cardId) {
    if (it is WidgetCard) it.copy(heightDp = heightDp.coerceIn(WIDGET_HEIGHT_RANGE)) else it
}

/** Widget ids still used by a card; any other id the panel's widget host holds can be freed. */
fun PanelConfig.widgetIds(): Set<Int> =
    pages.flatMap { it.cards }.filterIsInstance<WidgetCard>().mapTo(mutableSetOf()) { it.widgetId }

fun PanelConfig.setColumns(cardId: String, columns: Int): PanelConfig = mapCard(cardId) {
    when (it) {
        is GridCard -> it.copy(columns = columns.coerceIn(COLUMN_RANGE))
        is UsageCard -> it.copy(columns = columns.coerceIn(COLUMN_RANGE))
        else -> it
    }
}

fun PanelConfig.addItems(cardId: String, items: List<Item>): PanelConfig = mapCard(cardId) {
    if (it is GridCard) it.copy(items = it.items + items) else it
}

/** Moves the icon at [from] to [to] within a grid card. */
fun PanelConfig.moveItem(cardId: String, from: Int, to: Int): PanelConfig = mapCard(cardId) {
    if (it is GridCard) it.copy(items = it.items.move(from, to)) else it
}

fun PanelConfig.removeItem(cardId: String, index: Int): PanelConfig = mapCard(cardId) {
    if (it is GridCard) it.copy(items = it.items.filterIndexed { i, _ -> i != index }) else it
}

fun PanelConfig.setNoteText(cardId: String, text: String): PanelConfig = mapCard(cardId) {
    if (it is NoteCard) it.copy(text = text) else it
}

private fun PanelConfig.mapCard(cardId: String, transform: (Card) -> Card): PanelConfig =
    copy(pages = pages.map { page -> page.copy(cards = page.cards.map { if (it.id == cardId) transform(it) else it }) })

/** Moves the element at [from] to [to]; out-of-range moves leave the list unchanged. */
internal fun <T> List<T>.move(from: Int, to: Int): List<T> =
    if (from !in indices || to !in indices) this else toMutableList().apply { add(to, removeAt(from)) }
