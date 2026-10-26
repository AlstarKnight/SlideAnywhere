package io.github.alstarknight.slideanywhere

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(prefs: Prefs) {
    val context = LocalContext.current
    var overlayGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(false) }
    var batteryUnrestricted by remember { mutableStateOf(false) }
    var serviceEnabled by remember { mutableStateOf(prefs.serviceEnabled) }

    LifecycleResumeEffect(Unit) {
        overlayGranted = Settings.canDrawOverlays(context)
        notificationsGranted = hasNotificationPermission(context)
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        // Covers returning from the overlay permission screen and the service having been killed.
        if (prefs.serviceEnabled && overlayGranted) PanelService.start(context)
        onPauseOrDispose {}
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

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
                    title = stringResource(R.string.permission_battery),
                    description = stringResource(R.string.permission_battery_desc),
                    granted = batteryUnrestricted,
                ) { context.startActivity(batteryOptimizationIntent(context)) }
            }

            SectionTitle(stringResource(R.string.section_trigger))
            Section {
                var side by remember { mutableStateOf(prefs.side) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.setting_side), Modifier.weight(1f))
                    FilterChip(
                        selected = side == Side.LEFT,
                        onClick = { side = Side.LEFT; prefs.side = Side.LEFT },
                        label = { Text(stringResource(R.string.side_left)) },
                    )
                    FilterChip(
                        selected = side == Side.RIGHT,
                        onClick = { side = Side.RIGHT; prefs.side = Side.RIGHT },
                        label = { Text(stringResource(R.string.side_right)) },
                    )
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
            }

            SectionTitle(stringResource(R.string.section_panel))
            Section {
                SliderRow(stringResource(R.string.setting_panel_width), "%", prefs.panelWidthPercent, 50..100) {
                    prefs.panelWidthPercent = it
                }
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
