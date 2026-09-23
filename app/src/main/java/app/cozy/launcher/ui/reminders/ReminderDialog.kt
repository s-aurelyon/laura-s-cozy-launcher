package app.cozy.launcher.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Reminder
import app.cozy.launcher.data.Repeat
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.toMillis
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.pickDate
import app.cozy.launcher.ui.pickTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun repeatLabel(r: Repeat) = when (r) {
    Repeat.NONE -> "Never"
    Repeat.DAILY -> "Daily"
    Repeat.WEEKLY -> "Weekly"
    Repeat.MONTHLY -> "Monthly"
}

fun dateChipLabel(d: LocalDate?): String {
    val today = LocalDate.now()
    return when (d) {
        null -> "No date"
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> d.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}

fun dueMillis(date: LocalDate?, time: LocalTime?): Long? {
    val d = date ?: if (time != null) LocalDate.now() else return null
    return (if (time != null) d.atTime(time) else d.atStartOfDay()).toMillis()
}

/** Add or edit a reminder. Used by Reminders, Calendar and the note editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialDate: LocalDate? = null,
    noteId: String? = null,
    existing: Reminder? = null,
) {
    val ctx = LocalContext.current
    val settings by Store.settings.collectAsState()
    var title by remember { mutableStateOf(existing?.title ?: initialTitle) }
    var date by remember { mutableStateOf(existing?.dueDate() ?: initialDate) }
    var time by remember { mutableStateOf(existing?.takeIf { it.hasTime }?.dueDateTime()?.toLocalTime()) }
    var repeat by remember { mutableStateOf(existing?.repeat ?: Repeat.NONE) }
    var list by remember { mutableStateOf(existing?.list) }

    CozyDialog(onDismiss, if (existing != null) "Edit reminder" else "New reminder") {
        CozyField(title, { title = it }, "What should I remind you about?", Modifier.fillMaxWidth())

        SectionLabel("When")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val today = LocalDate.now()
            Chip("Today", date == today, { date = today })
            Chip("Tomorrow", date == today.plusDays(1), { date = today.plusDays(1) })
            val other = date != null && date != today && date != today.plusDays(1)
            Chip(if (other) dateChipLabel(date) else "Pick date", other, { pickDate(ctx, date ?: today) { date = it } })
            Chip("No date", date == null && time == null, { date = null; time = null })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(time?.format(DateTimeFormatter.ofPattern("HH:mm"))?.let { "At $it" } ?: "Add a time", time != null, {
                pickTime(ctx, time ?: LocalTime.of(9, 0)) { time = it; if (date == null) date = LocalDate.now() }
            })
            if (time != null) Chip("No time", false, { time = null })
        }

        SectionLabel("Repeat")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Repeat.values().forEach { r -> Chip(repeatLabel(r), repeat == r, { repeat = r }) }
        }

        SectionLabel("List")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("None", list == null, { list = null })
            settings.reminderLists.forEach { l -> Chip(l, list == l, { list = l }) }
        }

        DialogButtons(
            confirm = "Save",
            onConfirm = {
                val clean = title.trim()
                if (clean.isNotEmpty()) {
                    val due = dueMillis(date, time)
                    if (existing != null) {
                        Store.updateReminder(existing.copy(title = clean, due = due, hasTime = time != null, repeat = repeat, list = list))
                    } else {
                        Store.addReminder(Reminder(title = clean, due = due, hasTime = time != null, repeat = repeat, list = list, noteId = noteId))
                    }
                }
                onDismiss()
            },
            onCancel = onDismiss,
            danger = if (existing != null) "Delete" else null,
            onDanger = if (existing != null) ({ Store.deleteReminder(existing.id); onDismiss() }) else null,
        )
    }
}
