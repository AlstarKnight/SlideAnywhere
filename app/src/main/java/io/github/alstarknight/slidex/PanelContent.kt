package io.github.alstarknight.slidex

import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The panel itself: swipeable pages of cards, with an edit mode for changing them. */
@Composable
fun PanelContent(store: ConfigStore, launcher: ItemLauncher, onOpenSettings: () -> Unit, onClose: () -> Unit) {
    val config by store.config.collectAsState()
    val switches by launcher.switches.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var addingToCard by remember { mutableStateOf<String?>(null) }
    var showingDrawer by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState { config.pages.size }
    val scope = rememberCoroutineScope()
    val extras = LocalThemeExtras.current

    LaunchedEffect(Unit) { launcher.refreshSwitches() }
    BackHandler(enabled = editing) { editing = false }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Row(
                Modifier
                    .drawBehind { if (extras.stars) drawStars() }
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = extras.titleBrush?.let { MaterialTheme.typography.titleLarge.copy(brush = it) }
                        ?: MaterialTheme.typography.titleLarge,
                )
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
                            switches = switches,
                            onStartEditing = { editing = true },
                            onLaunch = { item ->
                                when {
                                    item is ActionItem && item.action == Action.APP_DRAWER -> showingDrawer = true
                                    !launcher.launch(item) -> onClose()
                                }
                            },
                            store = store,
                            onAddItem = { addingToCard = card.id },
                            onClose = onClose,
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
                                onAddSystem = { kind, title ->
                                    store.update { c -> c.addCard(pageIndex, SystemCard(ConfigStore.newId(), title, kind)) }
                                },
                                onAddUsage = { kind, title ->
                                    store.update { c -> c.addCard(pageIndex, UsageCard(ConfigStore.newId(), title, kind)) }
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
        if (showingDrawer) {
            AppDrawer(
                onLaunch = { app ->
                    launcher.launch(app)
                    onClose()
                },
                onDismiss = { showingDrawer = false },
            )
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
    switches: Set<Action>,
    onStartEditing: () -> Unit,
    onLaunch: (Item) -> Unit,
    store: ConfigStore,
    onAddItem: () -> Unit,
    onClose: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), border = LocalThemeExtras.current.cardBorder) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            CardHeader(card, editing, onStartEditing, store)
            when (card) {
                is GridCard -> {
                    if (editing) ColumnsStepper(card.columns) { n -> store.update { it.setColumns(card.id, n) } }
                    ItemGrid(
                        items = card.items,
                        columns = card.columns,
                        editing = editing,
                        switches = switches,
                        onLaunch = onLaunch,
                        onRemove = { index -> store.update { it.removeItem(card.id, index) } },
                        onAdd = onAddItem,
                    )
                }
                is NoteCard -> NoteField(card, store)
                is SystemCard -> SystemCardContent(card.kind, onClose)
                is UsageCard -> UsageCardContent(card, editing, store, onLaunch, onClose)
            }
        }
    }
}

/** Recently or frequently used apps; asks for usage access first. */
@Composable
private fun UsageCardContent(
    card: UsageCard,
    editing: Boolean,
    store: ConfigStore,
    onLaunch: (Item) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    if (editing) ColumnsStepper(card.columns) { n -> store.update { it.setColumns(card.id, n) } }
    if (!UsageApps.hasAccess(context)) {
        Box(Modifier.padding(bottom = 12.dp)) {
            PermissionHint(R.string.needs_usage_access) {
                context.startActivity(UsageApps.settingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                onClose()
            }
        }
        return
    }
    val count = card.columns * USAGE_ROWS
    val apps by produceState<List<AppItem>?>(null, card.kind, count) {
        value = withContext(Dispatchers.IO) { UsageApps.load(context, card.kind, count) }
    }
    val list = apps ?: return
    if (list.isEmpty()) {
        Text(
            stringResource(R.string.usage_empty),
            Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    ItemGrid(
        items = list,
        columns = card.columns,
        editing = false,
        switches = emptySet(),
        onLaunch = onLaunch,
        onRemove = {},
        onAdd = {},
    )
}

private const val USAGE_ROWS = 2

@Composable
private fun ColumnsStepper(columns: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.card_columns, columns),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { onChange(columns - 1) }, enabled = columns > COLUMN_RANGE.first) {
            Text(stringResource(R.string.card_columns_fewer))
        }
        TextButton(onClick = { onChange(columns + 1) }, enabled = columns < COLUMN_RANGE.last) {
            Text(stringResource(R.string.card_columns_more))
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
    columns: Int,
    editing: Boolean,
    switches: Set<Action>,
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
    val iconSize = when {
        columns >= 6 -> 36.dp
        columns == 5 -> 40.dp
        else -> 44.dp
    }
    Column {
        cells.chunked(columns).forEach { row ->
            Row {
                row.forEach { index ->
                    val cellModifier = Modifier.weight(1f)
                    if (index < 0) {
                        AddCell(iconSize, onAdd, cellModifier)
                    } else {
                        val item = items[index]
                        val action = (item as? ActionItem)?.action
                        ItemCell(
                            item = item,
                            iconSize = iconSize,
                            switchedOn = action?.takeIf { it in SWITCH_ACTIONS }?.let { it in switches },
                            editing = editing,
                            onClick = { onLaunch(item) },
                            onRemove = { onRemove(index) },
                            modifier = cellModifier,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ItemCell(
    item: Item,
    iconSize: Dp,
    switchedOn: Boolean?,
    editing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !editing, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ItemIcon(item, iconSize, switchedOn)
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
private fun AddCell(iconSize: Dp, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(iconSize)
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

/**
 * App or link icon when one can be loaded, otherwise a round badge with a symbol or the label's first letter.
 * [switchedOn] colors a switch's badge by its state; null shows the plain badge.
 */
@Composable
fun ItemIcon(item: Item, size: Dp, switchedOn: Boolean? = null) {
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
        BadgeIcon(item, size, switchedOn)
    }
}

@Composable
private fun BadgeIcon(item: Item, size: Dp, switchedOn: Boolean?) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (switchedOn) {
        null -> colors.primaryContainer to colors.onPrimaryContainer
        true -> colors.primary to colors.onPrimary
        false -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (item is ActionItem) {
            Icon(painterResource(item.action.iconRes), contentDescription = null, tint = content)
        } else {
            Text(item.label.take(1), style = MaterialTheme.typography.titleMedium, color = content)
        }
    }
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
    onAddSystem: (SystemKind, String) -> Unit,
    onAddUsage: (UsageKind, String) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: () -> Unit,
    onMovePage: (Int) -> Unit,
) {
    val gridTitle = stringResource(R.string.new_grid_card_title)
    val noteTitle = stringResource(R.string.default_card_note)
    val systemTitles = SystemKind.entries.associateWith { stringResource(it.labelRes) }
    val recentTitle = stringResource(R.string.usage_recent)
    val frequentTitle = stringResource(R.string.usage_frequent)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ButtonRow(
            R.string.page_add_grid_card to { onAddGrid(gridTitle) },
            R.string.page_add_note_card to { onAddNote(noteTitle) },
        )
        SystemKind.entries.chunked(2).forEach { (first, second) ->
            ButtonRow(
                first.addLabelRes to { onAddSystem(first, systemTitles.getValue(first)) },
                second.addLabelRes to { onAddSystem(second, systemTitles.getValue(second)) },
            )
        }
        ButtonRow(
            R.string.page_add_recent_card to { onAddUsage(UsageKind.RECENT, recentTitle) },
            R.string.page_add_frequent_card to { onAddUsage(UsageKind.FREQUENT, frequentTitle) },
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

private val SystemKind.addLabelRes: Int
    get() = when (this) {
        SystemKind.BATTERY -> R.string.page_add_battery_card
        SystemKind.BRIGHTNESS -> R.string.page_add_brightness_card
        SystemKind.VOLUME -> R.string.page_add_volume_card
        SystemKind.STORAGE -> R.string.page_add_storage_card
    }

/** Equal-width buttons; a null action shows the button disabled. */
@Composable
private fun ButtonRow(vararg buttons: Pair<Int, (() -> Unit)?>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { (label, action) ->
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
