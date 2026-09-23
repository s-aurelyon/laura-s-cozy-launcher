package app.cozy.launcher.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Holidays
import app.cozy.launcher.data.Reminder
import app.cozy.launcher.data.Repeat
import app.cozy.launcher.data.Store
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.CheckMark
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.home.Divider
import app.cozy.launcher.ui.pickDate
import app.cozy.launcher.ui.pickTime
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(nav: Navigator) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val all by Store.reminders.collectAsState()
    val settings by Store.settings.collectAsState()
    var filter by remember { mutableStateOf("today") }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var addingList by remember { mutableStateOf(false) }

    // Quick-add bar
    var newTitle by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf<LocalDate?>(null) }
    var newTime by remember { mutableStateOf<LocalTime?>(null) }
    var newRepeat by remember { mutableStateOf(Repeat.NONE) }
    var newList by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val open = all.filter { !it.done }
    val todayItems = open.filter { it.dueDate() != null && !it.dueDate()!!.isAfter(today) }.sortedBy { it.due }
    val scheduled = open.filter { it.dueDate()?.isAfter(today) == true }.sortedBy { it.due }
    val done = all.filter { it.done }.sortedByDescending { it.doneAt ?: 0L }

    fun addNew() {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        Store.addReminder(
            Reminder(
                title = clean,
                due = dueMillis(newDate, newTime),
                hasTime = newTime != null,
                repeat = newRepeat,
                list = newList ?: settings.reminderLists.firstOrNull { it == filter },
            )
        )
        newTitle = ""
        newTime = null
        newRepeat = Repeat.NONE
    }

    Page {
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Header("Reminders", { nav.back() })

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SmartCard("sun", "Today", todayItems.size, filter == "today", Modifier.weight(1f)) { filter = "today" }
                SmartCard("calendar", "Scheduled", scheduled.size, filter == "scheduled", Modifier.weight(1f)) { filter = "scheduled" }
                SmartCard("list", "All", open.size, filter == "all", Modifier.weight(1f)) { filter = "all" }
                SmartCard("circlecheck", "Done", done.size, filter == "done", Modifier.weight(1f)) { filter = "done" }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                val left = todayItems.size
                val cheer = when {
                    left == 0 && done.any { it.doneAt != null && app.cozy.launcher.data.toLocalDateTime(it.doneAt).toLocalDate() == today } -> "All done for today. Proud of you!"
                    left == 0 -> "Nothing due today. Enjoy the calm!"
                    left == 1 -> "Just 1 left today. Almost there!"
                    else -> "$left left today. Little steps, you've got this."
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(p.mint)
                        .then(if (p.eink) Modifier.border(p.line, p.ink, RoundedCornerShape(26.dp)) else Modifier)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Mascot(size = 68.dp, bob = false)
                    Txt(cheer, T.body(19, 600), Modifier.weight(1f))
                }

                val onEdit = { r: Reminder -> editing = r }
                val onOpenNote = { id: String -> nav.go(Screen.Editor(id)) }
                when (filter) {
                    "today" -> {
                        val overdue = todayItems.filter { it.dueDate()!!.isBefore(today) }
                        val due = todayItems.filter { it.dueDate() == today }
                        if (overdue.isNotEmpty()) Section("Overdue", overdue, onEdit, onOpenNote)
                        Section("Today", due, onEdit, onOpenNote, empty = "Nothing else due today.")
                        val tomorrow = today.plusDays(1)
                        val tomorrowItems = scheduled.filter { it.dueDate() == tomorrow }
                        if (tomorrowItems.isNotEmpty()) {
                            val h = Holidays.name(tomorrow)
                            Section(if (h != null) "Tomorrow · $h" else "Tomorrow", tomorrowItems, onEdit, onOpenNote)
                        }
                    }
                    "scheduled" -> {
                        if (scheduled.isEmpty()) Txt("Nothing scheduled yet.", T.body(18), color = p.muted)
                        scheduled.groupBy { it.dueDate()!! }.toSortedMap().forEach { (d, items) ->
                            val h = Holidays.name(d)
                            val label = (if (d == today.plusDays(1)) "Tomorrow" else d.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))) + (h?.let { " · $it" } ?: "")
                            Section(label, items, onEdit, onOpenNote)
                        }
                    }
                    "all" -> {
                        if (open.isEmpty()) Txt("No reminders. Add one below.", T.body(18), color = p.muted)
                        open.groupBy { it.list ?: "Reminders" }.forEach { (l, items) ->
                            Section(l, items.sortedBy { it.due ?: Long.MAX_VALUE }, onEdit, onOpenNote)
                        }
                    }
                    "done" -> {
                        if (done.isEmpty()) Txt("Nothing ticked off yet.", T.body(18), color = p.muted)
                        else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SectionLabel("Done", Modifier.weight(1f))
                                Pill("Clear done", { Store.clearDone() }, style = PillStyle.SOFT, icon = "trash", textSize = 15)
                            }
                            Section(null, done, onEdit, onOpenNote)
                        }
                    }
                    else -> {
                        val items = open.filter { it.list == filter }.sortedBy { it.due ?: Long.MAX_VALUE }
                        Section(filter, items, onEdit, onOpenNote, empty = "Nothing in $filter yet.")
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("My lists", Modifier.padding(vertical = 10.dp))
                    settings.reminderLists.forEach { l ->
                        val count = open.count { it.list == l }
                        Chip("$l · $count", filter == l, { filter = if (filter == l) "today" else l })
                    }
                    Pill("+ List", { addingList = true }, style = PillStyle.DASHED, textSize = 16)
                }
            }

            // Add bar
            Card(border = p.ink, radius = 26.dp, padding = PaddingValues(20.dp), spacing = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CozyField(
                        newTitle, { newTitle = it }, "New reminder…", Modifier.weight(1f),
                        boxed = false, leadingIcon = "plus",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addNew() }),
                    )
                    Pill("Add", { addNew() }, style = PillStyle.DARK)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Today", newDate == today, { newDate = if (newDate == today) null else today })
                    Chip("Tomorrow", newDate == today.plusDays(1), { newDate = if (newDate == today.plusDays(1)) null else today.plusDays(1) })
                    val other = newDate != null && newDate != today && newDate != today.plusDays(1)
                    Chip(if (other) dateChipLabel(newDate) else "Pick date", other, { pickDate(ctx, newDate ?: today) { newDate = it } })
                    Chip(newTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Time", newTime != null, {
                        if (newTime != null) newTime = null
                        else pickTime(ctx, LocalTime.now().plusHours(1).withMinute(0)) { newTime = it; if (newDate == null) newDate = today }
                    })
                    Chip(if (newRepeat == Repeat.NONE) "Repeat" else repeatLabel(newRepeat), newRepeat != Repeat.NONE, {
                        newRepeat = Repeat.values()[(newRepeat.ordinal + 1) % Repeat.values().size]
                    })
                    Chip(newList ?: "List", newList != null, {
                        val lists = listOf<String?>(null) + settings.reminderLists
                        newList = lists[(lists.indexOf(newList) + 1) % lists.size]
                    })
                }
            }
        }
    }

    editing?.let { r -> AddReminderDialog(onDismiss = { editing = null }, existing = r) }

    if (addingList) {
        var name by remember { mutableStateOf("") }
        CozyDialog({ addingList = false }, "New list") {
            CozyField(name, { name = it }, "List name", Modifier.fillMaxWidth())
            DialogButtons("Add", {
                val clean = name.trim()
                if (clean.isNotEmpty() && clean !in settings.reminderLists) Store.updateSettings { it.copy(reminderLists = it.reminderLists + clean) }
                addingList = false
            }, { addingList = false })
        }
    }
}

@Composable
private fun SmartCard(icon: String, label: String, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) p.ink else p.card)
            .border(p.line, if (selected) p.ink else p.border, shape)
            .clickable(onClickLabel = label, role = Role.Tab, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val fg = if (selected) p.onInk else p.ink
        Row(verticalAlignment = Alignment.CenterVertically) {
            CozyIcon(icon, size = 26.dp, tint = fg)
            Box(Modifier.weight(1f))
            Txt("$count", T.display(28), color = fg)
        }
        Txt(label, T.body(17, 700), color = fg, maxLines = 1)
    }
}

@Composable
private fun Section(
    title: String?,
    items: List<Reminder>,
    onEdit: (Reminder) -> Unit,
    onOpenNote: (String) -> Unit,
    empty: String? = null,
) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != null) Txt(title, T.display(24, 500))
        if (items.isEmpty()) {
            if (empty != null) Txt(empty, T.body(17), color = p.muted)
        } else {
            Card(padding = PaddingValues(0.dp), spacing = 0.dp) {
                items.forEachIndexed { i, r ->
                    ReminderRow(r, onEdit, onOpenNote)
                    if (i < items.lastIndex) Divider()
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(r: Reminder, onEdit: (Reminder) -> Unit, onOpenNote: (String) -> Unit) {
    val p = LocalPalette.current
    val today = LocalDate.now()
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Edit reminder") { onEdit(r) }.padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CheckMark(r.done, { Store.toggleReminder(r.id) }, size = 32.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(r.title, T.body(20, 600), color = if (r.done) p.muted else p.ink, strike = r.done, maxLines = 2)
            val meta = listOfNotNull(
                r.list,
                if (r.repeat != Repeat.NONE) "repeats ${repeatLabel(r.repeat).lowercase()}" else null,
                if (r.noteId != null) "linked note" else null,
            ).joinToString(" · ")
            if (meta.isNotEmpty()) Txt(meta, T.body(15), color = p.muted, maxLines = 1)
        }
        if (r.noteId != null && Store.note(r.noteId) != null) {
            RoundButton("notes", "Open linked note", { onOpenNote(r.noteId) }, size = 42.dp)
        }
        val d = r.dueDateTime()
        val label = when {
            d == null -> ""
            d.toLocalDate() == today -> if (r.hasTime) d.format(DateTimeFormatter.ofPattern("HH:mm")) else "Today"
            d.toLocalDate().isBefore(today) -> d.format(DateTimeFormatter.ofPattern("d MMM"))
            else -> d.format(DateTimeFormatter.ofPattern(if (r.hasTime) "EEE HH:mm" else "EEE d MMM"))
        }
        Txt(label, T.body(18, 700), color = if (d != null && d.toLocalDate().isBefore(today) && !r.done) p.holiday else p.ink)
    }
}
