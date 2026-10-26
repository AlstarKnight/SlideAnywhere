package io.github.alstarknight.slideanywhere

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val COLUMNS = 4

/** The panel itself: swipeable pages of cards, with an edit mode for changing them. */
@Composable
fun PanelContent(store: ConfigStore, launcher: ItemLauncher, onOpenSettings: () -> Unit, onClose: () -> Unit) {
    val config by store.config.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var addingToCard by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState { config.pages.size }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = editing) { editing = false }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                if (config.pages.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / ${config.pages.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { editing = !editing }) {
                    Icon(
                        if (editing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = stringResource(if (editing) R.string.edit_done else R.string.edit_start),
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.panel_open_settings))
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                val page = config.pages.getOrNull(pageIndex) ?: return@HorizontalPager
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(page.cards, key = { it.id }) { card ->
                        PanelCard(
                            card = card,
                            editing = editing,
                            onStartEditing = { editing = true },
                            onLaunch = { item ->
                                launcher.launch(item)
                                onClose()
                            },
                            store = store,
                            onAddItem = { addingToCard = card.id },
                        )
                    }
                    if (page.cards.isEmpty() && !editing) {
                        item { EmptyPageHint() }
                    }
                    if (editing) {
                        item {
                            PageEditor(
                                canDelete = config.pages.size > 1,
                                canMoveLeft = pageIndex > 0,
                                canMoveRight = pageIndex < config.pages.size - 1,
                                onAddGrid = {
                                    val title = it
                                    store.update { c -> c.addCard(pageIndex, GridCard(ConfigStore.newId(), title)) }
                                },
                                onAddNote = {
                                    val title = it
                                    store.update { c -> c.addCard(pageIndex, NoteCard(ConfigStore.newId(), title)) }
                                },
                                onAddPage = {
                                    store.update { it.addPage() }
                                    scope.launch { pagerState.animateScrollToPage(config.pages.size) }
                                },
                                onDeletePage = { store.update { it.removePage(pageIndex) } },
                                onMovePage = { offset ->
                                    store.update { it.movePage(pageIndex, offset) }
                                    scope.launch { pagerState.scrollToPage(pageIndex + offset) }
                                },
                            )
                        }
                    }
                }
            }
        }
        addingToCard?.let { cardId ->
            AddItemSheet(
                onAdd = { item ->
                    store.update { it.addItem(cardId, item) }
                    addingToCard = null
                },
                onDismiss = { addingToCard = null },
            )
        }
    }
}

@Composable
private fun EmptyPageHint() {
    Text(
        stringResource(R.string.panel_empty_page),
        Modifier.padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PanelCard(
    card: Card,
    editing: Boolean,
    onStartEditing: () -> Unit,
    onLaunch: (Item) -> Unit,
    store: ConfigStore,
    onAddItem: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            CardHeader(card, editing, onStartEditing, store)
            when (card) {
                is GridCard -> ItemGrid(
                    items = card.items,
                    editing = editing,
                    onLaunch = onLaunch,
                    onRemove = { index -> store.update { it.removeItem(card.id, index) } },
                    onAdd = onAddItem,
                )
                is NoteCard -> NoteField(card, store)
            }
        }
    }
}

@Composable
private fun CardHeader(card: Card, editing: Boolean, onStartEditing: () -> Unit, store: ConfigStore) {
    var renaming by remember(card.id) { mutableStateOf(false) }
    if (editing && renaming) {
        var title by remember(card.id) { mutableStateOf(card.title) }
        val commit = {
            store.update { it.renameCard(card.id, title.trim()) }
            renaming = false
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailingIcon = {
                IconButton(onClick = commit) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.edit_done))
                }
            },
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            card.title,
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onStartEditing() }) },
            style = MaterialTheme.typography.titleMedium,
        )
        if (editing) {
            SmallIconButton(Icons.Default.KeyboardArrowUp, R.string.card_move_up) { store.update { it.moveCard(card.id, -1) } }
            SmallIconButton(Icons.Default.KeyboardArrowDown, R.string.card_move_down) { store.update { it.moveCard(card.id, 1) } }
            SmallIconButton(Icons.Default.Edit, R.string.card_rename) { renaming = true }
            SmallIconButton(Icons.Default.Delete, R.string.card_delete) { store.update { it.removeCard(card.id) } }
        }
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, description: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = stringResource(description), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ItemGrid(
    items: List<Item>,
    editing: Boolean,
    onLaunch: (Item) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    // Cells are indexes into items; -1 is the "add" cell shown in edit mode.
    val cells = items.indices.toList() + if (editing) listOf(-1) else emptyList()
    if (cells.isEmpty()) {
        Text(
            stringResource(R.string.card_empty),
            Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column {
        cells.chunked(COLUMNS).forEach { row ->
            Row {
                row.forEach { index ->
                    val cellModifier = Modifier.weight(1f)
                    if (index < 0) {
                        AddCell(onAdd, cellModifier)
                    } else {
                        ItemCell(items[index], editing, { onLaunch(items[index]) }, { onRemove(index) }, cellModifier)
                    }
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ItemCell(item: Item, editing: Boolean, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !editing, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ItemIcon(item, 44.dp)
            if (editing) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.item_remove),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
        Text(
            item.label,
            Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddCell(onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
        Text(
            stringResource(R.string.item_add),
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** App or link icon when one can be loaded, otherwise a round badge with a symbol or the label's first letter. */
@Composable
fun ItemIcon(item: Item, size: Dp) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val key = InstalledApps.iconKey(item)
    val bitmap by produceState(key?.let(InstalledApps::cachedIcon), key) {
        if (value == null && key != null) {
            value = withContext(Dispatchers.IO) {
                when (item) {
                    is AppItem -> InstalledApps.appIcon(context, item.packageName, item.activityName, sizePx)
                    is LinkItem -> InstalledApps.linkIcon(context, item.uri, sizePx)
                    is ActionItem -> null
                }
            }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded, contentDescription = null, modifier = Modifier.size(size))
    } else {
        BadgeIcon(item, size)
    }
}

@Composable
private fun BadgeIcon(item: Item, size: Dp) {
    val vector = (item as? ActionItem)?.action?.icon
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (vector != null) {
            Icon(vector, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            Text(
                item.label.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Material icon for actions that have a fitting one in the core icon set; the rest show their first letter. */
private val Action.icon: ImageVector?
    get() = when (this) {
        Action.NOTIFICATIONS -> Icons.Default.Notifications
        Action.QUICK_SETTINGS -> Icons.Default.Settings
        Action.RECENTS -> Icons.Default.Menu
        Action.HOME -> Icons.Default.Home
        Action.BACK -> Icons.AutoMirrored.Filled.ArrowBack
        Action.LOCK_SCREEN -> Icons.Default.Lock
        Action.PLAY_PAUSE -> Icons.Default.PlayArrow
        Action.NEXT_TRACK -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        Action.PREVIOUS_TRACK -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        Action.VOLUME_UP -> Icons.Default.KeyboardArrowUp
        Action.VOLUME_DOWN -> Icons.Default.KeyboardArrowDown
        Action.SCREENSHOT, Action.SPLIT_SCREEN, Action.FLASHLIGHT, Action.VOICE_ASSISTANT -> null
    }

@Composable
private fun NoteField(card: NoteCard, store: ConfigStore) {
    var text by remember(card.id) { mutableStateOf(card.text) }
    TextField(
        value = text,
        onValueChange = {
            text = it
            store.update { c -> c.setNoteText(card.id, it) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        placeholder = { Text(stringResource(R.string.note_placeholder)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun PageEditor(
    canDelete: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onAddGrid: (String) -> Unit,
    onAddNote: (String) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: () -> Unit,
    onMovePage: (Int) -> Unit,
) {
    val gridTitle = stringResource(R.string.new_grid_card_title)
    val noteTitle = stringResource(R.string.default_card_note)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ButtonRow(
            R.string.page_add_grid_card to { onAddGrid(gridTitle) },
            R.string.page_add_note_card to { onAddNote(noteTitle) },
        )
        ButtonRow(
            R.string.page_add to onAddPage,
            R.string.page_delete to onDeletePage.takeIf { canDelete },
        )
        ButtonRow(
            R.string.page_move_left to { onMovePage(-1) }.takeIf { canMoveLeft },
            R.string.page_move_right to { onMovePage(1) }.takeIf { canMoveRight },
        )
    }
}

/** Two equal-width buttons; a null action shows the button disabled. */
@Composable
private fun ButtonRow(first: Pair<Int, (() -> Unit)?>, second: Pair<Int, (() -> Unit)?>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(first, second).forEach { (label, action) ->
            OutlinedButton(
                onClick = { action?.invoke() },
                enabled = action != null,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
