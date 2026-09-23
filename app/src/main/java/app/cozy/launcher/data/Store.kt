package app.cozy.launcher.data

import android.content.Context
import app.cozy.launcher.system.Alarms
import app.cozy.launcher.system.Apps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Everything the app keeps: settings, notes, reminders, events and the timer.
 * Held in memory as flows and saved as small JSON files in the app's private storage.
 */
object Store {
    private lateinit var dir: File
    lateinit var app: Context
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = MutableStateFlow(Settings())
    val notes = MutableStateFlow<List<Note>>(emptyList())
    val reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val events = MutableStateFlow<List<CalEvent>>(emptyList())
    val timer = MutableStateFlow(TimerState())

    private var ready = false

    @OptIn(FlowPreview::class)
    fun init(context: Context) {
        if (ready) return
        ready = true
        app = context.applicationContext
        dir = File(app.filesDir, "cozy").apply { mkdirs() }

        val firstRun = !File(dir, "settings.json").exists()
        settings.value = load("settings.json", Settings.serializer(), Settings())
        notes.value = load("notes.json", ListSerializer(Note.serializer()), emptyList())
        reminders.value = load("reminders.json", ListSerializer(Reminder.serializer()), emptyList())
        events.value = load("events.json", ListSerializer(CalEvent.serializer()), emptyList())
        timer.value = load("timer.json", TimerState.serializer(), TimerState())

        if (firstRun) {
            settings.value = settings.value.copy(tiles = guessAppsForTiles(settings.value.tiles))
            notes.value = listOf(Templates.welcomeNote())
        }

        scope.launch { settings.drop(1).debounce(250).collect { save("settings.json", Settings.serializer(), it) } }
        scope.launch { notes.drop(1).debounce(400).collect { save("notes.json", ListSerializer(Note.serializer()), it) } }
        scope.launch { reminders.drop(1).debounce(250).collect { save("reminders.json", ListSerializer(Reminder.serializer()), it) } }
        scope.launch { events.drop(1).debounce(250).collect { save("events.json", ListSerializer(CalEvent.serializer()), it) } }
        scope.launch { timer.drop(1).debounce(250).collect { save("timer.json", TimerState.serializer(), it) } }

        if (firstRun) {
            // Make sure the first-run defaults are written even if nothing else changes.
            scope.launch {
                save("settings.json", Settings.serializer(), settings.value)
                save("notes.json", ListSerializer(Note.serializer()), notes.value)
            }
        }
    }

    /** On first run, link Books and Music to apps that are already installed. */
    private fun guessAppsForTiles(tiles: List<Tile>): List<Tile> {
        val music = listOf("com.spotify.music", "com.google.android.apps.youtube.music", "deezer.android.app", "com.apple.android.music")
        val books = listOf("com.amazon.kindle", "com.kobobooks.android", "com.google.android.apps.books", "com.bigme.reader", "org.readera", "com.faultexception.reader")
        return tiles.map { t ->
            when (t.id) {
                "music" -> t.copy(pkg = music.firstOrNull { Apps.isInstalled(app, it) })
                "books" -> t.copy(pkg = books.firstOrNull { Apps.isInstalled(app, it) })
                else -> t
            }
        }
    }

    private fun <T> load(name: String, ser: KSerializer<T>, default: T): T = try {
        val f = File(dir, name)
        if (f.exists()) json.decodeFromString(ser, f.readText()) else default
    } catch (e: Exception) {
        // Keep a copy of anything we could not read, rather than losing it.
        try { File(dir, name).copyTo(File(dir, "$name.broken-${System.currentTimeMillis()}"), overwrite = true) } catch (_: Exception) {}
        default
    }

    private fun <T> save(name: String, ser: KSerializer<T>, value: T) {
        try {
            val tmp = File(dir, "$name.tmp")
            tmp.writeText(json.encodeToString(ser, value))
            tmp.renameTo(File(dir, name))
        } catch (_: Exception) {
        }
    }

    // ---------- Settings ----------

    fun updateSettings(change: (Settings) -> Settings) {
        settings.value = change(settings.value)
    }

    fun updateTile(id: String, change: (Tile) -> Tile) = updateSettings { s ->
        s.copy(tiles = s.tiles.map { if (it.id == id) change(it) else it })
    }

    // ---------- Notes ----------

    fun note(id: String): Note? = notes.value.firstOrNull { it.id == id }

    fun upsertNote(note: Note) {
        val list = notes.value
        val i = list.indexOfFirst { it.id == note.id }
        notes.value = if (i >= 0) list.toMutableList().also { it[i] = note } else listOf(note) + list
    }

    fun deleteNote(id: String) {
        notes.value = notes.value.filterNot { it.id == id }
    }

    fun dailyPage(date: LocalDate): Note {
        val existing = notes.value.firstOrNull { it.day == date.toEpochDay() }
        if (existing != null) return existing
        val n = Templates.create("daily", date = date)
        upsertNote(n)
        return n
    }

    // ---------- Reminders ----------

    fun addReminder(r: Reminder) {
        reminders.value = reminders.value + r
        Alarms.scheduleReminder(app, r)
    }

    fun updateReminder(r: Reminder) {
        reminders.value = reminders.value.map { if (it.id == r.id) r else it }
        Alarms.scheduleReminder(app, r)
    }

    fun deleteReminder(id: String) {
        reminders.value = reminders.value.filterNot { it.id == id }
        Alarms.cancelReminder(app, id)
    }

    /** Tick a reminder off. Repeating reminders move on to their next date instead. */
    fun toggleReminder(id: String) {
        val r = reminders.value.firstOrNull { it.id == id } ?: return
        val updated = when {
            r.done -> r.copy(done = false, doneAt = null)
            r.repeat != Repeat.NONE && r.due != null -> r.copy(due = nextOccurrence(r))
            else -> r.copy(done = true, doneAt = System.currentTimeMillis())
        }
        updateReminder(updated)
    }

    private fun nextOccurrence(r: Reminder): Long {
        var t = r.dueDateTime() ?: return r.due ?: 0L
        val now = LocalDateTime.now()
        do {
            t = when (r.repeat) {
                Repeat.DAILY -> t.plusDays(1)
                Repeat.WEEKLY -> t.plusWeeks(1)
                Repeat.MONTHLY -> t.plusMonths(1)
                Repeat.NONE -> t
            }
        } while (t.isBefore(now))
        return t.toMillis()
    }

    fun clearDone() {
        val gone = reminders.value.filter { it.done }
        reminders.value = reminders.value.filterNot { it.done }
        gone.forEach { Alarms.cancelReminder(app, it.id) }
    }

    // ---------- Events ----------

    fun upsertEvent(e: CalEvent) {
        val list = events.value
        events.value = if (list.any { it.id == e.id }) list.map { if (it.id == e.id) e else it } else list + e
    }

    fun deleteEvent(id: String) {
        events.value = events.value.filterNot { it.id == id }
    }
}
