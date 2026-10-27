package io.github.alstarknight.slidex

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists the installed widgets. The chosen one is bound (asking the user when Android requires it), configured
 * if it has a setup screen, and added as a card to the page the panel was on; then the panel opens again.
 */
class WidgetPickerActivity : ComponentActivity() {
    private val host by lazy { WidgetHost.get(this) }
    private val manager by lazy { AppWidgetManager.getInstance(this) }
    private var pageIndex = 0

    /** Widget id allocated for the widget being added, or [NO_WIDGET]. Kept across recreation. */
    private var pendingId = NO_WIDGET

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) configure() else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pageIndex = intent.getIntExtra(EXTRA_PAGE, 0)
        pendingId = savedInstanceState?.getInt(STATE_PENDING_ID, NO_WIDGET) ?: NO_WIDGET
        setContent {
            AppTheme {
                WidgetPicker(onPick = ::pick, onClose = ::finish)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PENDING_ID, pendingId)
    }

    private fun pick(info: AppWidgetProviderInfo) {
        if (pendingId != NO_WIDGET) return
        pendingId = host.allocateAppWidgetId()
        if (manager.bindAppWidgetIdIfAllowed(pendingId, info.profile, info.provider, null)) {
            configure()
        } else {
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile),
            )
        }
    }

    /** Opens the widget's setup screen when it has a required one, otherwise adds it right away. */
    private fun configure() {
        val info = manager.getAppWidgetInfo(pendingId) ?: return cancelPending()
        val optional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure == null || optional) {
            add(info)
            return
        }
        try {
            // The setup screen is often not exported, so it has to be started through the widget host.
            host.startAppWidgetConfigureActivityForResult(this, pendingId, 0, REQUEST_CONFIGURE, null)
        } catch (e: ActivityNotFoundException) {
            cancelPending()
            Toast.makeText(this, R.string.widget_failed, Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("The widget setup screen can only be started with startActivityForResult.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE) return
        val info = manager.getAppWidgetInfo(pendingId)
        if (resultCode == RESULT_OK && info != null) add(info) else cancelPending()
    }

    private fun add(info: AppWidgetProviderInfo) {
        val card = WidgetCard(
            id = ConfigStore.newId(),
            title = info.loadLabel(packageManager),
            widgetId = pendingId,
            heightDp = WidgetHost.defaultHeightDp(this, info),
        )
        ConfigStore.get(this).update { it.addCard(pageIndex, card) }
        pendingId = NO_WIDGET
        PanelService.openPanel(this, pageIndex)
        finish()
    }

    private fun cancelPending() {
        if (pendingId != NO_WIDGET) host.deleteAppWidgetId(pendingId)
        pendingId = NO_WIDGET
    }

    companion object {
        private const val EXTRA_PAGE = "page"
        private const val STATE_PENDING_ID = "pending_id"
        private const val REQUEST_CONFIGURE = 1
        private const val NO_WIDGET = -1

        fun intent(context: Context, pageIndex: Int): Intent =
            Intent(context, WidgetPickerActivity::class.java).putExtra(EXTRA_PAGE, pageIndex)
    }
}

@Composable
private fun WidgetPicker(onPick: (AppWidgetProviderInfo) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val previewPx = with(LocalDensity.current) { PREVIEW_SIZE.roundToPx() }
    val widgets by produceState<List<WidgetOption>?>(null) {
        value = withContext(Dispatchers.IO) { loadWidgetOptions(context, previewPx) }
    }
    var query by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
                Text(stringResource(R.string.widget_picker_title), style = MaterialTheme.typography.titleLarge)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_widgets)) },
            )
            val list = widgets
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                list.isEmpty() -> Text(stringResource(R.string.widget_none), Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(searchApps(list, query) { it.search }) { option ->
                        ListItem(
                            headlineContent = { Text(option.label) },
                            supportingContent = {
                                Text(stringResource(R.string.widget_option_details, option.appLabel, option.columns, option.rows))
                            },
                            leadingContent = {
                                Box(Modifier.size(PREVIEW_SIZE), contentAlignment = Alignment.Center) {
                                    option.preview?.let { Image(it, contentDescription = null) }
                                }
                            },
                            modifier = Modifier.clickable { onPick(option.info) },
                        )
                    }
                }
            }
        }
    }
}

private val PREVIEW_SIZE = 72.dp
