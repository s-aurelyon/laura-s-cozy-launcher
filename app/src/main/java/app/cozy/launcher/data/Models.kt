package app.cozy.launcher.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

// ---------- Home screen ----------

/**
 * A tile on the home screen. It opens a built-in app ([builtin]) or an installed
 * app ([pkg]). If both are set, the installed app wins.
 */
@Serializable
data class Tile(
    val id: String,
    val label: String,
    val icon: String,
    val builtin: String? = null,
    val pkg: String? = null,
)

object Builtins {
    const val NOTES = "notes"
    const val REMINDERS = "reminders"
    const val TIMER = "timer"
    const val CALENDAR = "calendar"
    val all = listOf(NOTES, REMINDERS, TIMER, CALENDAR)
    fun label(id: String) = when (id) {
        NOTES -> "Notes"
        REMINDERS -> "Reminders"
        TIMER -> "Focus timer"
        CALENDAR -> "Calendar"
        else -> id
    }
    fun icon(id: String) = when (id) {
        NOTES -> "notes"
        REMINDERS -> "bell"
        TIMER -> "timer"
        CALENDAR -> "calendar"
        else -> "star"
    }
}

fun defaultTiles() = listOf(
    Tile("notes", "Notes", "notes", builtin = Builtins.NOTES),
    Tile("books", "Books", "book"),
    Tile("music", "Music", "music"),
    Tile("reminders", "Reminders", "bell", builtin = Builtins.REMINDERS),
    Tile("timer", "Timer", "timer", builtin = Builtins.TIMER),
    Tile("calendar", "Calendar", "calendar", builtin = Builtins.CALENDAR),
)

@Serializable
data class Settings(
    val name: String = "Laura",
    val accent: Long = 0xFFF4C2CB,
    val animations: Boolean = true,
    val eink: Boolean = false,
    val weekStartsMonday: Boolean = true,
    val tiles: List<Tile> = defaultTiles(),
    val folders: List<String> = listOf("Lists", "Journal", "Recipes"),
    val reminderLists: List<String> = listOf("Home", "Wedding", "Shopping"),
    val penHoldMs: Long = 1200,
    val timerSound: Boolean = true,
    val autoStartBreaks: Boolean = true,
    val keepAwake: Boolean = false,
    val focusMinutes: Int = 25,
    val shortMinutes: Int = 5,
    val longMinutes: Int = 15,
    val askedNotifications: Boolean = false,
    val setupDone: Boolean = false,
)

// ---------- Notes ----------

@Serializable
enum class TextKind { TITLE, HEADING, SUBHEADING, BODY, MONO }

@Serializable
sealed class Block {
    abstract val id: String
}

@Serializable
@SerialName("text")
data class TextBlock(override val id: String = newId(), val text: String = "", val kind: TextKind = TextKind.BODY) : Block()

@Serializable
@SerialName("check")
data class CheckBlock(override val id: String = newId(), val text: String = "", val done: Boolean = false) : Block()

@Serializable
@SerialName("bullet")
data class BulletBlock(override val id: String = newId(), val text: String = "") : Block()

@Serializable
@SerialName("table")
data class TableBlock(override val id: String = newId(), val rows: List<List<String>>) : Block()

/** A pen stroke. Points are stored flat (x0, y0, x1, y1, ...) in dp. */
@Serializable
data class Stroke(
    val color: Long,
    val width: Float,
    val highlighter: Boolean = false,
    val pts: List<Float>,
)

/** A floating text box placed with a long pen hold. Position in dp. */
@Serializable
data class TextBox(
    val id: String = newId(),
    val x: Float,
    val y: Float,
    val text: String = "",
    val width: Float = 280f,
)

@Serializable
data class Note(
    val id: String = newId(),
    val title: String = "",
    val folder: String? = null,
    val paper: String = "blank",
    val paperColor: Long = 0xFFFFFDF8,
    val template: String = "blank",
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val blocks: List<Block> = listOf(TextBlock()),
    val strokes: List<Stroke> = emptyList(),
    val boxes: List<TextBox> = emptyList(),
    val color: Long = 0xFFF4C2CB,
    /** For daily pages: the epoch day this page belongs to. */
    val day: Long? = null,
) {
    fun previewText(): String {
        val parts = blocks.mapNotNull {
            when (it) {
                is TextBlock -> it.text
                is CheckBlock -> it.text
                is BulletBlock -> it.text
                is TableBlock -> it.rows.flatten().joinToString(" ")
            }
        }.map { it.trim() }.filter { it.isNotEmpty() }
        val text = parts.joinToString(" · ").replace("\n", " ")
        return when {
            text.isNotBlank() -> text
            boxes.any { it.text.isNotBlank() } -> boxes.first { it.text.isNotBlank() }.text
            strokes.isNotEmpty() -> "Handwritten note"
            else -> "Empty note"
        }
    }

    fun isEmpty(): Boolean = title.isBlank() && strokes.isEmpty() &&
        boxes.all { it.text.isBlank() } && blocks.all {
            when (it) {
                is TextBlock -> it.text.isBlank()
                is CheckBlock -> it.text.isBlank()
                is BulletBlock -> it.text.isBlank()
                is TableBlock -> it.rows.flatten().all { c -> c.isBlank() }
            }
        }

    fun checkCounts(): Pair<Int, Int> {
        val checks = blocks.filterIsInstance<CheckBlock>()
        return checks.count { it.done } to checks.size
    }
}

// ---------- Reminders ----------

@Serializable
enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY }

@Serializable
data class Reminder(
    val id: String = newId(),
    val title: String,
    val list: String? = null,
    /** Epoch millis. */
    val due: Long? = null,
    val hasTime: Boolean = false,
    val repeat: Repeat = Repeat.NONE,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val noteId: String? = null,
) {
    fun dueDate(): LocalDate? = due?.let { toLocalDateTime(it).toLocalDate() }
    fun dueDateTime(): LocalDateTime? = due?.let { toLocalDateTime(it) }
}

// ---------- Calendar ----------

@Serializable
data class CalEvent(
    val id: String = newId(),
    val title: String,
    val epochDay: Long,
    /** Minutes after midnight, or null for all day. */
    val minutes: Int? = null,
)

// ---------- Focus timer ----------

@Serializable
data class TimerState(
    val mode: String = "focus",
    val totalSec: Int = 25 * 60,
    val remainingSec: Int = 25 * 60,
    /** When running: the moment it ends (epoch millis). Null when paused or stopped. */
    val endAt: Long? = null,
    /** Completed focus sessions in the current round of four. */
    val session: Int = 0,
    val focusingOn: String = "",
    /** Set right after a session ends, until the next action. */
    val finishedMode: String? = null,
)

// ---------- Time helpers ----------

fun toLocalDateTime(millis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDate.atStartMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
