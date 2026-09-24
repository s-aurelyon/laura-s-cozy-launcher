package app.cozy.launcher.ui

import androidx.compose.runtime.mutableStateListOf

sealed class Screen {
    data object Home : Screen()
    data class Edit(val selectTile: String? = null) : Screen()
    data object AllApps : Screen()
    data object Settings : Screen()
    data object Themes : Screen()
    data object Notes : Screen()
    data object Templates : Screen()
    data class Editor(val noteId: String) : Screen()
    data object Reminders : Screen()
    data object Timer : Screen()
    data object Calendar : Screen()
}

/** A tiny back stack. The home screen is always at the bottom. */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()

    fun go(s: Screen) {
        if (current != s) stack.add(s)
    }

    /** Swap the current screen for another (used after picking a template). */
    fun replace(s: Screen) {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack.add(s)
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun home() {
        stack.clear()
        stack.add(Screen.Home)
    }

    fun openBuiltin(id: String) {
        when (id) {
            "notes" -> go(Screen.Notes)
            "reminders" -> go(Screen.Reminders)
            "timer" -> go(Screen.Timer)
            "calendar" -> go(Screen.Calendar)
        }
    }
}
