package io.github.alstarknight.slidex

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(prefs: Prefs) {
    val context = LocalContext.current
    var overlayGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(false) }
    var batteryUnrestricted by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var writeSettingsGranted by remember { mutableStateOf(false) }
    var dndAccessGranted by remember { mutableStateOf(false) }
    var usageAccessGranted by remember { mutableStateOf(false) }
    var serviceEnabled by remember { mutableStateOf(prefs.serviceEnabled) }

    LifecycleResumeEffect(Unit) {
        overlayGranted = Settings.canDrawOverlays(context)
        notificationsGranted = hasNotificationPermission(context)
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        accessibilityEnabled = ActionAccessibilityService.isEnabled(context)
        writeSettingsGranted = Settings.System.canWrite(context)
        dndAccessGranted = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        usageAccessGranted = UsageApps.hasAccess(context)
        // Covers returning from the overlay permission screen and the service having been killed.
        if (prefs.serviceEnabled && overlayGranted) PanelService.start(context)
        onPauseOrDispose {}
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }
    val store = remember { ConfigStore.get(context) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) toast(context, exportConfig(context, it, store.config.value))
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) toast(context, importConfig(context, it, store))
    }
    var confirmImport by remember { mutableStateOf(false) }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            text = { Text(stringResource(R.string.config_import_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text(stringResource(R.string.config_import)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val titleBrush = LocalThemeExtras.current.titleBrush
            Text(
                stringResource(R.string.app_name),
                style = titleBrush?.let { MaterialTheme.typography.headlineMedium.copy(brush = it) }
                    ?: MaterialTheme.typography.headlineMedium,
            )

            Section {
                SwitchRow(stringResource(R.string.setting_enable), serviceEnabled) { on ->
                    serviceEnabled = on
                    prefs.serviceEnabled = on
                    when {
                        !on -> PanelService.stop(context)
                        overlayGranted -> PanelService.start(context)
                        else -> context.startActivity(overlaySettingsIntent(context))
                    }
                }
                var showNotification by remember { mutableStateOf(prefs.showNotification) }
                SwitchRow(stringResource(R.string.setting_show_notification), showNotification) {
                    showNotification = it
                    prefs.showNotification = it
                }
                Hint(stringResource(R.string.show_notification_hint))
            }

            SectionTitle(stringResource(R.string.section_permissions))
            Section {
                PermissionRow(
                    title = stringResource(R.string.permission_overlay),
                    description = stringResource(R.string.permission_overlay_desc),
                    granted = overlayGranted,
                ) { context.startActivity(overlaySettingsIntent(context)) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRow(
                        title = stringResource(R.string.permission_notifications),
                        description = stringResource(R.string.permission_notifications_desc),
                        granted = notificationsGranted,
                    ) { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
                PermissionRow(
                    title = stringResource(R.string.permission_accessibility),
                    description = stringResource(R.string.permission_accessibility_desc),
                    granted = accessibilityEnabled,
                ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                PermissionRow(
                    title = stringResource(R.string.permission_write_settings),
                    description = stringResource(R.string.permission_write_settings_desc),
                    granted = writeSettingsGranted,
                ) { context.startActivity(ItemLauncher.writeSettingsIntent(context)) }
                PermissionRow(
                    title = stringResource(R.string.permission_dnd),
                    description = stringResource(R.string.permission_dnd_desc),
                    granted = dndAccessGranted,
                ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                PermissionRow(
                    title = stringResource(R.string.permission_usage),
                    description = stringResource(R.string.permission_usage_desc),
                    granted = usageAccessGranted,
                ) { context.startActivity(UsageApps.settingsIntent()) }
                PermissionRow(
                    title = stringResource(R.string.permission_battery),
                    description = stringResource(R.string.permission_battery_desc),
                    granted = batteryUnrestricted,
                ) { context.startActivity(batteryOptimizationIntent(context)) }
            }

            SectionTitle(stringResource(R.string.section_trigger))
            Section {
                var mode by remember { mutableStateOf(prefs.triggerMode) }
                ChoiceRow(
                    stringResource(R.string.setting_trigger_mode),
                    listOf(TriggerMode.EDGE to R.string.trigger_edge, TriggerMode.BALL to R.string.trigger_ball),
                    mode,
                ) {
                    mode = it
                    prefs.triggerMode = it
                }
                if (mode == TriggerMode.EDGE) EdgeHandleSettings(prefs) else FloatingBallSettings(prefs)
                HiddenAppsRow(prefs)
                if (!accessibilityEnabled) Hint(stringResource(R.string.hidden_apps_hint))
                var hideInLandscape by remember { mutableStateOf(prefs.hideInLandscape) }
                SwitchRow(stringResource(R.string.setting_hide_in_landscape), hideInLandscape) {
                    hideInLandscape = it
                    prefs.hideInLandscape = it
                }
            }

            SectionTitle(stringResource(R.string.section_theme))
            Section {
                var preset by remember { mutableStateOf(prefs.themePreset) }
                ThemePicker(preset) {
                    preset = it
                    prefs.themePreset = it
                }
                var darkMode by remember { mutableStateOf(prefs.darkMode) }
                ChoiceRow(
                    stringResource(R.string.setting_dark_mode),
                    listOf(
                        DarkMode.SYSTEM to R.string.dark_mode_system,
                        DarkMode.LIGHT to R.string.dark_mode_light,
                        DarkMode.DARK to R.string.dark_mode_dark,
                    ),
                    darkMode,
                ) {
                    darkMode = it
                    prefs.darkMode = it
                }
                IconPackChoice(prefs)
            }

            SectionTitle(stringResource(R.string.section_panel))
            Section {
                SliderRow(stringResource(R.string.setting_panel_width), "%", prefs.panelWidthPercent, 50..100) {
                    prefs.panelWidthPercent = it
                }
                // Blurring what is behind a window needs Android 12.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var blur by remember { mutableStateOf(prefs.panelBlur) }
                    SwitchRow(stringResource(R.string.setting_panel_blur), blur) {
                        blur = it
                        prefs.panelBlur = it
                    }
                    Hint(stringResource(R.string.panel_blur_hint))
                }
            }

            SectionTitle(stringResource(R.string.section_config))
            Section {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("slidex-config.json") }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.config_export))
                    }
                    OutlinedButton(onClick = { confirmImport = true }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.config_import))
                    }
                }
            }
        }
    }
}

@Composable
private fun EdgeHandleSettings(prefs: Prefs) {
    var side by remember { mutableStateOf(prefs.side) }
    ChoiceRow(stringResource(R.string.setting_side), listOf(Side.LEFT to R.string.side_left, Side.RIGHT to R.string.side_right), side) {
        side = it
        prefs.side = it
    }
    SliderRow(stringResource(R.string.setting_handle_position), "%", prefs.handlePositionPercent, 0..100) {
        prefs.handlePositionPercent = it
    }
    // The system grants at most 200dp of back-gesture exclusion per edge.
    SliderRow(stringResource(R.string.setting_handle_height), "dp", prefs.handleHeightDp, 60..200) {
        prefs.handleHeightDp = it
    }
    SliderRow(stringResource(R.string.setting_handle_width), "dp", prefs.handleWidthDp, 8..40) {
        prefs.handleWidthDp = it
    }
    var showHandle by remember { mutableStateOf(prefs.showHandle) }
    SwitchRow(stringResource(R.string.setting_show_handle), showHandle) {
        showHandle = it
        prefs.showHandle = it
    }
    Hint(stringResource(R.string.handle_drag_hint))
}

@Composable
private fun FloatingBallSettings(prefs: Prefs) {
    var shape by remember { mutableStateOf(prefs.ballShape) }
    ChoiceRow(
        stringResource(R.string.setting_ball_shape),
        listOf(BallShape.CIRCLE to R.string.ball_circle, BallShape.SQUARE to R.string.ball_square),
        shape,
    ) {
        shape = it
        prefs.ballShape = it
    }
    SliderRow(stringResource(R.string.setting_ball_size), "dp", prefs.ballSizeDp, 32..96) { prefs.ballSizeDp = it }
    SliderRow(stringResource(R.string.setting_ball_opacity), "%", prefs.ballOpacityPercent, 30..100) {
        prefs.ballOpacityPercent = it
    }
    var tapAction by remember { mutableStateOf(prefs.ballTapAction) }
    ActionChoice(stringResource(R.string.setting_ball_tap), tapAction) {
        tapAction = it
        prefs.ballTapAction = it
    }
    var longPressAction by remember { mutableStateOf(prefs.ballLongPressAction) }
    ActionChoice(stringResource(R.string.setting_ball_long_press), longPressAction) {
        longPressAction = it
        prefs.ballLongPressAction = it
    }
    Hint(stringResource(R.string.ball_hint))
}

/** A row showing what a gesture does; tapping it lists "open the panel" and every action to pick from. */
@Composable
private fun ActionChoice(title: String, selected: Action?, onSelect: (Action?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { picking = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f))
        Text(actionLabel(selected), color = MaterialTheme.colorScheme.primary)
    }
    if (!picking) return
    // The app drawer lives inside the panel, so it is not offered on its own.
    val options = listOf<Action?>(null) + Action.entries.filter { it != Action.APP_DRAWER }
    AlertDialog(
        onDismissRequest = { picking = false },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                picking = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(actionLabel(option), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** A row naming the icon pack in use; tapping it lists the installed packs. */
@Composable
private fun IconPackChoice(prefs: Prefs) {
    val context = LocalContext.current
    val packs by produceState<List<IconPackEntry>?>(null) {
        value = withContext(Dispatchers.IO) { IconPacks.installed(context) }
    }
    var selected by remember { mutableStateOf(prefs.iconPack) }
    var picking by remember { mutableStateOf(false) }
    val systemIcons = stringResource(R.string.icon_pack_none)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { picking = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.setting_icon_pack), Modifier.weight(1f))
        Text(
            packs?.firstOrNull { it.packageName == selected }?.label ?: systemIcons,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (!picking) return
    val options = listOf<IconPackEntry?>(null) + packs.orEmpty()
    AlertDialog(
        onDismissRequest = { picking = false },
        title = { Text(stringResource(R.string.setting_icon_pack)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = option?.packageName
                                prefs.iconPack = selected
                                InstalledApps.clearIconCache()
                                picking = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option?.packageName == selected, onClick = null)
                        Text(option?.label ?: systemIcons, Modifier.padding(start = 12.dp))
                    }
                }
                if (packs?.isEmpty() == true) Hint(stringResource(R.string.icon_pack_empty))
            }
        },
        confirmButton = {
            TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** A row showing how many apps hide the trigger; tapping it opens the app list. */
@Composable
private fun HiddenAppsRow(prefs: Prefs) {
    var hiddenApps by remember { mutableStateOf(prefs.listedApps) }
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { picking = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.setting_hidden_apps), Modifier.weight(1f))
        Text(
            if (hiddenApps.isEmpty()) {
                stringResource(R.string.hidden_apps_none)
            } else {
                stringResource(R.string.hidden_apps_count, hiddenApps.size)
            },
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (picking) {
        HiddenAppsDialog(
            selected = hiddenApps,
            onDone = {
                hiddenApps = it
                prefs.listedApps = it
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun HiddenAppsDialog(selected: Set<String>, onDone: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps by produceState<List<AppEntry>?>(null) {
        value = withContext(Dispatchers.IO) {
            // Apps already chosen come first, so they are easy to find and untick.
            InstalledApps.load(context).distinctBy { it.packageName }.sortedBy { it.packageName !in selected }
        }
    }
    var checked by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxHeight(0.85f), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text(
                    stringResource(R.string.setting_hidden_apps),
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                )
                val list = apps
                if (list == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filtered = searchApps(list, query) { it.search }
                    LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key = { it.packageName }) { app ->
                            val on = app.packageName in checked
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { checked = if (on) checked - app.packageName else checked + app.packageName }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = on, onCheckedChange = null)
                                Box(Modifier.padding(horizontal = 12.dp)) {
                                    ItemIcon(AppItem(app.label, app.packageName, app.activityName), 32.dp)
                                }
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.End).padding(horizontal = 16.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onDone(checked) }) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }
}

@Composable
private fun actionLabel(action: Action?): String = stringResource(action?.labelRes ?: R.string.ball_action_panel)

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One tile per preset, showing a swatch of its accent. */
@Composable
private fun ThemePicker(selected: ThemePreset, onSelect: (ThemePreset) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreset.entries.forEach { preset ->
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .border(
                        if (preset == selected) 2.dp else 1.dp,
                        if (preset == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape,
                    )
                    .clickable { onSelect(preset) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(presetSwatch(preset)),
                )
                Text(
                    stringResource(preset.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A title with one chip per option below it. */
@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    Column {
        Text(title)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(title: String, unit: String, initial: Int, range: IntRange, onChange: (Int) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    Column {
        Row {
            Text(title, Modifier.weight(1f))
            Text("$value$unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                value = it.roundToInt()
                onChange(value)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

@Composable
private fun PermissionRow(title: String, description: String, granted: Boolean, onRequest: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (granted) {
            Text(stringResource(R.string.permission_granted), color = MaterialTheme.colorScheme.primary)
        } else {
            OutlinedButton(onClick = onRequest) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun overlaySettingsIntent(context: Context) =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())

@SuppressLint("BatteryLife")
private fun batteryOptimizationIntent(context: Context) =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())

private fun toast(context: Context, message: Int) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/** Writes the config to a user-picked file; returns the message to show. */
private fun exportConfig(context: Context, uri: Uri, config: PanelConfig): Int {
    try {
        val stream = context.contentResolver.openOutputStream(uri) ?: return R.string.config_io_failed
        stream.use { it.write(config.toJson().encodeToByteArray()) }
        return R.string.config_exported
    } catch (e: IOException) {
        return R.string.config_io_failed
    }
}

/** Replaces the config with a user-picked file; returns the message to show. */
private fun importConfig(context: Context, uri: Uri, store: ConfigStore): Int {
    try {
        val stream = context.contentResolver.openInputStream(uri) ?: return R.string.config_io_failed
        val config = parseConfig(stream.use { it.readBytes().decodeToString() })
        store.update { config }
        WidgetHost.deleteUnused(context, config)
        return R.string.config_imported
    } catch (e: IOException) {
        return R.string.config_io_failed
    } catch (e: IllegalArgumentException) {
        return R.string.config_invalid
    }
}
