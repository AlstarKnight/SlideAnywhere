package io.github.alstarknight.slidex

import android.content.Context
import android.util.Log
import androidx.core.util.AtomicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/** App-wide holder of the panel config; every change is written to `files/config.json` in the background. */
class ConfigStore private constructor(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "config.json"))
    private val _config = MutableStateFlow(load(context))
    val config: StateFlow<PanelConfig> = _config

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            _config.drop(1).collect { save(it) }
        }
    }

    fun update(transform: (PanelConfig) -> PanelConfig) {
        _config.update(transform)
    }

    private fun load(context: Context): PanelConfig {
        if (!file.baseFile.exists()) return defaultConfig(context)
        return try {
            parseConfig(file.readFully().decodeToString())
        } catch (e: IllegalArgumentException) {
            // Keep the unreadable file for inspection instead of overwriting it.
            Log.w(TAG, "Config is invalid, starting from defaults", e)
            file.baseFile.renameTo(File(context.filesDir, "config.json.bad"))
            defaultConfig(context)
        } catch (e: IOException) {
            Log.w(TAG, "Cannot read config, starting from defaults", e)
            defaultConfig(context)
        }
    }

    private fun save(config: PanelConfig) {
        var stream: FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(config.toJson().encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: IOException) {
            stream?.let(file::failWrite)
            Log.w(TAG, "Cannot save config", e)
        }
    }

    companion object {
        private const val TAG = "ConfigStore"

        @Volatile
        private var instance: ConfigStore? = null

        fun get(context: Context): ConfigStore =
            instance ?: synchronized(this) {
                instance ?: ConfigStore(context.applicationContext).also { instance = it }
            }

        fun newId(): String = UUID.randomUUID().toString()

        private fun defaultConfig(context: Context): PanelConfig {
            val shortcuts = listOf(Action.NOTIFICATIONS, Action.QUICK_SETTINGS, Action.SCREENSHOT, Action.LOCK_SCREEN)
                .map { ActionItem(context.getString(it.labelRes), it) }
            return PanelConfig(
                pages = listOf(
                    Page(
                        listOf(
                            GridCard(newId(), context.getString(R.string.default_card_apps)),
                            GridCard(newId(), context.getString(R.string.default_card_shortcuts), shortcuts),
                            NoteCard(newId(), context.getString(R.string.default_card_note)),
                        ),
                    ),
                ),
            )
        }
    }
}
