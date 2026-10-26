package io.github.alstarknight.slideanywhere

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full-panel sheet for picking what to add to a grid card: an app, a shortcut action or a link. */
@Composable
fun AddItemSheet(onAdd: (Item) -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    var tab by remember { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.safeDrawingPadding()) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
                Text(stringResource(R.string.item_add), style = MaterialTheme.typography.titleLarge)
            }
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.add_tab_apps, R.string.add_tab_actions, R.string.add_tab_link).forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(stringResource(label)) })
                }
            }
            when (tab) {
                0 -> AppPicker(onAdd)
                1 -> ActionPicker(onAdd)
                else -> LinkForm(onAdd)
            }
        }
    }
}

@Composable
private fun AppPicker(onAdd: (Item) -> Unit) {
    val context = LocalContext.current
    val apps by produceState<List<AppEntry>?>(null) {
        value = withContext(Dispatchers.IO) { InstalledApps.load(context) }
    }
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.search_apps)) },
    )
    val list = apps
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val filtered = list.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
    LazyColumn {
        items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
            val item = AppItem(app.label, app.packageName, app.activityName)
            PickerRow(item, app.label) { onAdd(item) }
        }
    }
}

@Composable
private fun ActionPicker(onAdd: (Item) -> Unit) {
    val context = LocalContext.current
    LazyColumn {
        items(Action.entries) { action ->
            val item = ActionItem(context.getString(action.labelRes), action)
            PickerRow(item, item.label) { onAdd(item) }
        }
    }
}

@Composable
private fun PickerRow(item: Item, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { ItemIcon(item, 40.dp) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun LinkForm(onAdd: (Item) -> Unit) {
    var label by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    val valid = label.isNotBlank() && ItemLauncher.linkIntent(uri) != null
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.link_label)) },
        )
        OutlinedTextField(
            value = uri,
            onValueChange = { uri = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.link_uri)) },
            supportingText = { Text(stringResource(R.string.link_uri_hint)) },
        )
        Button(
            onClick = { onAdd(LinkItem(label.trim(), uri.trim())) },
            enabled = valid,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.item_add))
        }
    }
}
