package io.github.alstarknight.slidex

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        val stylePage = intent.getIntExtra(EXTRA_STYLE_PAGE, -1).takeIf { it >= 0 }
        setContent {
            AppTheme {
                // Match the status bar icons to the app's light or dark setting, which can differ from the system's.
                val dark = LocalThemeExtras.current.dark
                LaunchedEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                SettingsScreen(prefs, stylePage)
            }
        }
    }

    companion object {
        private const val EXTRA_STYLE_PAGE = "style_page"

        /** Opens the settings straight on the look of the panel's [page], in a fresh task. */
        fun pageStyleIntent(context: Context, page: Int): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_STYLE_PAGE, page)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
