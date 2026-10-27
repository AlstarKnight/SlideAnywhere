package io.github.alstarknight.slidex

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

val SystemKind.labelRes: Int
    get() = when (this) {
        SystemKind.BATTERY -> R.string.system_battery
        SystemKind.BRIGHTNESS -> R.string.system_brightness
        SystemKind.VOLUME -> R.string.system_volume
        SystemKind.STORAGE -> R.string.system_storage
    }

/** Body of a system card; [onClose] closes the panel, e.g. before opening a permission page. */
@Composable
fun SystemCardContent(kind: SystemKind, onClose: () -> Unit) {
    Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (kind) {
            SystemKind.BATTERY -> BatteryInfo()
            SystemKind.BRIGHTNESS -> BrightnessSlider(onClose)
            SystemKind.VOLUME -> VolumeSliders()
            SystemKind.STORAGE -> StorageInfo()
        }
    }
}

@Composable
private fun BatteryInfo() {
    val context = LocalContext.current
    var battery by remember { mutableStateOf<Intent?>(null) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                battery = intent
            }
        }
        // The battery broadcast is sticky, so registering also returns the current state.
        battery = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    val intent = battery ?: return
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> R.string.battery_charging
        BatteryManager.BATTERY_STATUS_FULL -> R.string.battery_full
        BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.battery_discharging
        else -> R.string.battery_not_charging
    }
    val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> R.string.battery_health_good
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> R.string.battery_health_overheat
        BatteryManager.BATTERY_HEALTH_DEAD -> R.string.battery_health_dead
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> R.string.battery_health_over_voltage
        BatteryManager.BATTERY_HEALTH_COLD -> R.string.battery_health_cold
        else -> R.string.battery_health_unknown
    }
    val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f

    Row(verticalAlignment = Alignment.Bottom) {
        Text("$percent%", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(status),
            Modifier.padding(bottom = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    LinearProgressIndicator(
        progress = { percent / 100f },
        modifier = Modifier.fillMaxWidth(),
        color = if (percent <= LOW_BATTERY_PERCENT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Text(
        stringResource(R.string.battery_details, temperature, voltage, stringResource(health)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BrightnessSlider(onClose: () -> Unit) {
    val context = LocalContext.current
    if (!Settings.System.canWrite(context)) {
        PermissionHint(R.string.needs_write_settings) {
            context.startActivity(ItemLauncher.writeSettingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            onClose()
        }
        return
    }
    val resolver = context.contentResolver
    val max = remember { maxBrightness(context) }
    var value by remember {
        mutableFloatStateOf(Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, max / 2).toFloat())
    }
    // What this drag last wrote, or null before its first change; the slider moves many times per step.
    var written by remember { mutableStateOf<Int?>(null) }
    val seeThrough = LocalPanelSeeThrough.current
    DisposableEffect(seeThrough) { onDispose { seeThrough(false) } }
    Slider(
        value = value,
        onValueChange = {
            seeThrough(true)
            value = it
            val brightness = it.roundToInt()
            if (brightness != written) {
                try {
                    if (written == null) {
                        Settings.System.putInt(
                            resolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                        )
                    }
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                    written = brightness
                } catch (e: SecurityException) {
                    // Lost the permission while the panel was open; the slider just stops having an effect.
                }
            }
        },
        valueRange = 1f..max.toFloat(),
        onValueChangeFinished = {
            written = null
            seeThrough(false)
        },
    )
}

/** Highest brightness setting; some phones go above the usual 255. */
private fun maxBrightness(context: Context): Int {
    val system = Resources.getSystem()
    val id = system.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android")
    val configured = if (id != 0) system.getInteger(id) else DEFAULT_MAX_BRIGHTNESS
    val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
    return maxOf(configured, current, 2)
}

@Composable
private fun VolumeSliders() {
    VolumeRow(R.string.volume_media, AudioManager.STREAM_MUSIC)
    VolumeRow(R.string.volume_ring, AudioManager.STREAM_RING)
    VolumeRow(R.string.volume_alarm, AudioManager.STREAM_ALARM)
}

@Composable
private fun VolumeRow(label: Int, stream: Int) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val min = remember(stream) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0 }
    val max = remember(stream) { audio.getStreamMaxVolume(stream).coerceAtLeast(min + 1) }
    var value by remember(stream) { mutableFloatStateOf(audio.getStreamVolume(stream).toFloat()) }
    // What this drag last set, or null before its first change; the slider reports each step many times.
    var written by remember(stream) { mutableStateOf<Int?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = {
                value = it
                val volume = it.roundToInt()
                if (volume != written) {
                    try {
                        audio.setStreamVolume(stream, volume, 0)
                        written = volume
                    } catch (e: SecurityException) {
                        // Changing the ringer volume can need Do Not Disturb access; leave it as it is.
                    }
                }
            },
            onValueChangeFinished = { written = null },
            modifier = Modifier.weight(1f),
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
        )
    }
}

private class StorageStats(
    val ramAvailable: Long,
    val ramTotal: Long,
    val diskAvailable: Long,
    val diskTotal: Long,
    val uptimeMillis: Long,
)

private fun readStorageStats(context: Context): StorageStats {
    val memory = ActivityManager.MemoryInfo()
    context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
    val disk = StatFs(Environment.getDataDirectory().path)
    return StorageStats(
        ramAvailable = memory.availMem,
        ramTotal = memory.totalMem,
        diskAvailable = disk.availableBytes,
        diskTotal = disk.totalBytes,
        uptimeMillis = SystemClock.elapsedRealtime(),
    )
}

@Composable
private fun StorageInfo() {
    val context = LocalContext.current
    val stats by produceState<StorageStats?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) { readStorageStats(context) }
            delay(STORAGE_REFRESH_MS)
        }
    }
    val current = stats ?: return
    UsageRow(R.string.storage_ram, current.ramAvailable, current.ramTotal)
    UsageRow(R.string.storage_internal, current.diskAvailable, current.diskTotal)
    val minutes = current.uptimeMillis / 60_000
    Text(
        stringResource(R.string.uptime, minutes / (24 * 60), minutes / 60 % 24, minutes % 60),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UsageRow(label: Int, available: Long, total: Long) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    R.string.storage_available,
                    Formatter.formatShortFileSize(context, available),
                    Formatter.formatShortFileSize(context, total),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { if (total > 0) (total - available).toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Explains a missing permission, with a button that opens its settings page. */
@Composable
fun PermissionHint(message: Int, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(message),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
    }
}

private const val LOW_BATTERY_PERCENT = 20
private const val DEFAULT_MAX_BRIGHTNESS = 255
private const val STORAGE_REFRESH_MS = 2000L
