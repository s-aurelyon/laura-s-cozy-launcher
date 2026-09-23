package app.cozy.launcher.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.CalEvent
import app.cozy.launcher.data.Holidays
import app.cozy.launcher.data.Reminder
import app.cozy.launcher.data.Store
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.Tag
import app.cozy.launcher.ui.pickTime
import app.cozy.launcher.ui.reminders.AddReminderDialog
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class Entry(val title: String, val time: String, val kind: String, val event: CalEvent? = null, val reminder: Reminder? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(nav: Navigator) {
    val p = LocalPalette.current
    val events by Store.events.collectAsState()
    val reminders by Store.reminders.collectAsState()
    val settings by Store.settings.collectAsState()
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var addingEvent by remember { mutableStateOf<LocalDate?>(null) }
    var editingEvent by remember { mutableStateOf<CalEvent?>(null) }
    var addingReminder by remember { mutableStateOf<LocalDate?>(null) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    val firstDay = if (settings.weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val gridStart = first.minusDays(lead.toLong())
    val weeks = ((lead + month.lengthOfMonth()) + 6) / 7

    fun entriesFor(d: LocalDate): List<Entry> {
        val list = mutableListOf<Entry>()
        Holidays.name(d)?.let { list += Entry(it, "All day", "Holiday") }
        events.filter { it.epochDay == d.toEpochDay() }.sortedBy { it.minutes ?: -1 }.forEach { e ->
            list += Entry(e.title, e.minutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "All day", "Event", event = e)
        }
        reminders.filter { it.dueDate() == d }.sortedBy { it.due }.forEach { r ->
            list += Entry(r.title, if (r.hasTime) r.dueDateTime()!!.format(DateTimeFormatter.ofPattern("HH:mm")) else "", if (r.done) "Done" else "Reminder", reminder = r)
        }
        return list
    }

    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Header("Calendar", { nav.back() }) {
                Pill("Today", { month = YearMonth.from(today); selected = today }, style = PillStyle.LIGHT)
                RoundButton("back", "Previous month", { month = month.minusMonths(1) })
                RoundButton("forward", "Next month", { month = month.plusMonths(1) })
            }

            // The wall calendar sheet
            Box(Modifier.padding(top = 10.dp)) {
                val sheet = RoundedCornerShape(20.dp)
                Column(
                    Modifier.fillMaxWidth().clip(sheet).background(p.card).border(p.line, if (p.eink) p.ink else Color(0xFFE3D6C7), sheet)
                ) {
                    SeasonStrip(month)
                    // Weekday names
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
                        for (i in 0 until 7) {
                            val dow = firstDay.plus(i.toLong())
                            val weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
                            Txt(
                                dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                                T.body(14, 800), Modifier.weight(1f).padding(vertical = 8.dp),
                                color = if (weekend) p.holiday else p.ink, align = TextAlign.Center,
                            )
                        }
                    }
                    // Day grid
                    Column(
                        Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE3D6C7).let { if (p.eink) p.ink else it })
                            .padding(1.5.dp),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        for (w in 0 until weeks) {
                            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                                for (i in 0 until 7) {
                                    val d = gridStart.plusDays((w * 7 + i).toLong())
                                    val inMonth = d.month == month.month
                                    val entries = if (inMonth) entriesFor(d) else emptyList()
                                    DayCell(
                                        date = d,
                                        inMonth = inMonth,
                                        isToday = d == today,
                                        isSelected = d == selected,
                                        entries = entries,
                                        modifier = Modifier.weight(1f)
                                            .combinedClickable(
                                                onClick = { selected = d; if (!inMonth) month = YearMonth.from(d) },
                                                onLongClick = { selected = d; addingEvent = d },
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
                // Binding rings
                Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp).offset(y = (-14).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(12) {
                        Box(Modifier.width(12.dp).height(28.dp).clip(RoundedCornerShape(6.dp)).background(p.muted))
                    }
                }
            }

            // The selected day
            val dayEntries = entriesFor(selected)
            Card(radius = 26.dp, spacing = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt(selected.format(DateTimeFormatter.ofPattern("EEEE d MMMM")), T.serif(28), Modifier.weight(1f))
                    if (selected == today) Tag("Today", p.accent)
                }
                if (dayEntries.isEmpty()) Txt("Nothing planned. A free day!", T.body(18), color = p.muted)
                dayEntries.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    when {
                                        e.event != null -> editingEvent = e.event
                                        e.reminder != null -> editingReminder = e.reminder
                                    }
                                },
                                onLongClick = {
                                    if (e.event != null) editingEvent = e.event
                                },
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val dot = when (e.kind) {
                            "Holiday" -> p.holiday
                            "Event" -> if (p.eink) p.ink else Color(0xFF8FC7A6)
                            else -> p.blushInk
                        }
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(dot))
                        Txt(e.title, T.body(19, 600), Modifier.weight(1f), maxLines = 1, strike = e.kind == "Done", color = if (e.kind == "Done") p.muted else p.ink)
                        Tag(e.kind)
                        Txt(e.time, T.body(17, 700), Modifier.width(80.dp), align = TextAlign.End)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("+ Event", { addingEvent = selected }, style = PillStyle.DARK)
                    Pill("+ Reminder", { addingReminder = selected }, style = PillStyle.SOFT)
                    Pill("Open daily page", {
                        val n = Store.dailyPage(selected)
                        nav.go(Screen.Editor(n.id))
                    }, style = PillStyle.SOFT)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    addingEvent?.let { d -> EventDialog(d, null) { addingEvent = null } }
    editingEvent?.let { e -> EventDialog(LocalDate.ofEpochDay(e.epochDay), e) { editingEvent = null } }
    addingReminder?.let { d -> AddReminderDialog(onDismiss = { addingReminder = null }, initialDate = d) }
    editingReminder?.let { r -> AddReminderDialog(onDismiss = { editingReminder = null }, existing = r) }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    entries: List<Entry>,
    modifier: Modifier,
) {
    val p = LocalPalette.current
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val holiday = entries.firstOrNull { it.kind == "Holiday" }
    val bg = when {
        isSelected -> p.blush
        !inMonth -> if (p.eink) Color(0xFFF4F4F4) else Color(0xFFF7F1EA)
        weekend -> if (p.eink) Color.White else Color(0xFFFDF7F2)
        else -> p.card
    }
    var m = modifier.height(96.dp).background(bg)
    if (isSelected) m = m.border(2.5.dp, p.ink)
    Column(m.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val numColor = when {
            isToday -> p.onInk
            !inMonth -> Color(0xFF9A8A7E)
            weekend || holiday != null -> p.holiday
            else -> p.ink
        }
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(if (isToday) p.ink else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Txt(date.dayOfMonth.toString(), T.serif(20), color = numColor) }

        if (inMonth) {
            val label: String?
            val kind: String?
            val events = entries.filter { it.kind == "Event" }
            val rems = entries.filter { it.kind == "Reminder" }
            when {
                holiday != null -> { label = holiday.title; kind = "Holiday" }
                events.size == 1 -> { label = events[0].title; kind = "Event" }
                events.size > 1 -> { label = "${events.size} events"; kind = "Event" }
                rems.size == 1 -> { label = rems[0].title; kind = "Reminder" }
                rems.size > 1 -> { label = "${rems.size} reminders"; kind = "Reminder" }
                else -> { label = null; kind = null }
            }
            if (label != null) {
                val (chipBg, chipFg) = when {
                    p.eink -> p.soft to p.ink
                    kind == "Holiday" -> Color(0xFFF6D6CF) to Color(0xFF7E3328)
                    kind == "Event" -> p.mint to Color(0xFF2F4A3A)
                    else -> p.accent to p.ink
                }
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(chipBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Txt(label, T.body(12, 800), color = chipFg, maxLines = 1)
                }
            }
        }
    }
}

/** The illustrated strip at the top of the sheet. Southern-hemisphere seasons. */
@Composable
private fun SeasonStrip(month: YearMonth) {
    val p = LocalPalette.current
    val (season, colour) = when (month.monthValue) {
        9, 10, 11 -> "SPRING" to Color(0xFFF4C2CB)
        12, 1, 2 -> "SUMMER" to Color(0xFFF6E2A8)
        3, 4, 5 -> "AUTUMN" to Color(0xFFF3C9A5)
        else -> "WINTER" to Color(0xFFBFE0F0)
    }
    val bg = if (p.eink) p.soft else colour
    Box(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 24.dp).height(150.dp)
            .clip(RoundedCornerShape(14.dp)).background(bg)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val d = density
            fun flower(cx: Float, cy: Float, r: Float) {
                val petal = Color.White
                drawCircle(petal, r, Offset(cx, cy - r * 1.1f))
                drawCircle(petal, r, Offset(cx + r * 1.1f, cy))
                drawCircle(petal, r, Offset(cx, cy + r * 1.1f))
                drawCircle(petal, r, Offset(cx - r * 1.1f, cy))
                drawCircle(if (p.eink) p.border else Color(0xFFF6E2A8), r * 0.8f, Offset(cx, cy))
            }
            when (season) {
                "SPRING" -> { flower(size.width * 0.48f, 34 * d, 7 * d); flower(size.width * 0.58f, 92 * d, 4.5f * d); flower(size.width * 0.66f, 40 * d, 3.5f * d) }
                "SUMMER" -> { drawCircle(Color.White, 22 * d, Offset(size.width * 0.52f, 52 * d)); drawCircle(Color(0xFFFFF3C8), 14 * d, Offset(size.width * 0.52f, 52 * d)) }
                "AUTUMN" -> { drawOval(Color.White, Offset(size.width * 0.47f, 30 * d), androidx.compose.ui.geometry.Size(22 * d, 12 * d)); drawOval(Color.White, Offset(size.width * 0.58f, 84 * d), androidx.compose.ui.geometry.Size(16 * d, 9 * d)) }
                else -> for (i in 0 until 6) drawCircle(Color.White, (3 + i % 3) * d, Offset(size.width * (0.42f + i * 0.05f), (30 + (i * 23) % 80) * d))
            }
        }
        Row(
            Modifier.matchParentSize().padding(start = 28.dp, end = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt("$season · ${month.year}", T.body(15, 800))
                Txt(month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), T.serif(58, 500, italic = true), maxLines = 1)
            }
            Mascot(size = 124.dp, bob = false)
        }
    }
}

@Composable
private fun EventDialog(date: LocalDate, existing: CalEvent?, close: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var time by remember { mutableStateOf(existing?.minutes?.let { LocalTime.of(it / 60, it % 60) }) }
    CozyDialog(close, if (existing != null) "Edit event" else "New event · " + date.format(DateTimeFormatter.ofPattern("d MMM"))) {
        CozyField(title, { title = it }, "What's happening?", Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("All day", time == null, { time = null })
            Chip(time?.format(DateTimeFormatter.ofPattern("HH:mm"))?.let { "At $it" } ?: "Pick a time", time != null, {
                pickTime(ctx, time ?: LocalTime.of(10, 0)) { time = it }
            })
        }
        DialogButtons(
            confirm = "Save",
            onConfirm = {
                val clean = title.trim()
                if (clean.isNotEmpty()) {
                    val minutes = time?.let { it.hour * 60 + it.minute }
                    Store.upsertEvent(existing?.copy(title = clean, minutes = minutes) ?: CalEvent(title = clean, epochDay = date.toEpochDay(), minutes = minutes))
                }
                close()
            },
            onCancel = close,
            danger = if (existing != null) "Delete" else null,
            onDanger = if (existing != null) ({ Store.deleteEvent(existing.id); close() }) else null,
        )
    }
}
