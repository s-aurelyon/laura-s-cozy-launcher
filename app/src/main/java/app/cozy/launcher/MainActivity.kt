package app.cozy.launcher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import app.cozy.launcher.data.Focus
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.themeId
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.calendar.CalendarScreen
import app.cozy.launcher.ui.home.AllAppsScreen
import app.cozy.launcher.ui.home.EditScreen
import app.cozy.launcher.ui.home.HomeScreen
import app.cozy.launcher.ui.home.SettingsScreen
import app.cozy.launcher.ui.home.ThemesScreen
import app.cozy.launcher.ui.notes.NoteEditorScreen
import app.cozy.launcher.ui.notes.NotesScreen
import app.cozy.launcher.ui.notes.TemplatesScreen
import app.cozy.launcher.ui.reminders.RemindersScreen
import app.cozy.launcher.ui.theme.CozyTheme
import app.cozy.launcher.ui.theme.Fonts
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.timer.TimerScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val nav = Navigator()

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        Fonts.init(assets)
        handleOpen(intent)

        if (Build.VERSION.SDK_INT >= 33 && !Store.settings.value.askedNotifications) {
            Store.updateSettings { it.copy(askedNotifications = true) }
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by Store.settings.collectAsState()
            CozyTheme(theme = settings.themeId(), accent = settings.accent, animations = settings.animations) {
                val palette = LocalPalette.current
                LaunchedEffect(palette.bg) {
                    window.statusBarColor = palette.bg.toArgb()
                    window.navigationBarColor = palette.bg.toArgb()
                }
                // Keeps the focus timer moving on to breaks while the app is open.
                LaunchedEffect(Unit) {
                    while (true) {
                        Focus.tick()
                        delay(1000)
                    }
                }
                // Back goes back; on the home screen it does nothing, like any launcher.
                BackHandler(enabled = true) { nav.back() }

                Crossfade(
                    targetState = nav.current,
                    animationSpec = tween(if (settings.animations && !settings.eink) 180 else 0),
                    label = "screens",
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeScreen(nav)
                        is Screen.Edit -> EditScreen(nav, screen.selectTile)
                        Screen.AllApps -> AllAppsScreen(nav)
                        Screen.Settings -> SettingsScreen(nav)
                        Screen.Themes -> ThemesScreen(nav)
                        Screen.Notes -> NotesScreen(nav)
                        Screen.Templates -> TemplatesScreen(nav)
                        is Screen.Editor -> NoteEditorScreen(nav, screen.noteId)
                        Screen.Reminders -> RemindersScreen(nav)
                        Screen.Timer -> TimerScreen(nav)
                        Screen.Calendar -> CalendarScreen(nav)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing the Home button while Cozy is open goes back to the home screen.
        if (!handleOpen(intent)) nav.home()
    }

    private fun handleOpen(intent: Intent?): Boolean {
        val open = intent?.getStringExtra("open") ?: return false
        intent.removeExtra("open")
        nav.home()
        nav.openBuiltin(open)
        return true
    }
}
