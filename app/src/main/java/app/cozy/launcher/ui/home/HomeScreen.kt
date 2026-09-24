package app.cozy.launcher.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Focus
import app.cozy.launcher.data.Holidays
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Tile
import app.cozy.launcher.system.Apps
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.CheckMark
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.Sparkle
import app.cozy.launcher.ui.rememberNow
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(nav: Navigator) {
    if (LocalPalette.current.meadow) MeadowHomeScreen(nav) else CozyHomeScreen(nav)
}

/** Opens what a tile points at: another app, a built-in app, or edit mode if it points at nothing. */
fun openTile(ctx: android.content.Context, nav: Navigator, tile: Tile) {
    when {
        tile.pkg != null -> if (!Apps.launch(ctx, tile.pkg)) {
            Toast.makeText(ctx, "That app isn't installed any more. Pick another one.", Toast.LENGTH_LONG).show()
            nav.go(Screen.Edit(tile.id))
        }
        tile.builtin != null -> nav.openBuiltin(tile.builtin)
        else -> nav.go(Screen.Edit(tile.id))
    }
}

@Composable
private fun CozyHomeScreen(nav: Navigator) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val settings by Store.settings.collectAsState()
    val reminders by Store.reminders.collectAsState()
    val events by Store.events.collectAsState()
    val timer by Store.timer.collectAsState()
    val now = rememberNow(1000)
    val today = LocalDate.now()

    val todays = reminders
        .filter { !it.done && it.dueDate() != null && !it.dueDate()!!.isAfter(today) }
        .sortedBy { it.due }
    val todaysEvents = events.filter { it.epochDay == today.toEpochDay() }.sortedBy { it.minutes ?: -1 }

    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 5 -> "Hello night owl"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }


    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Greeting
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Txt(today.format(DateTimeFormatter.ofPattern("EEEE · d MMMM")), T.body(18, 600), color = p.muted)
                    Txt("$greeting, ${settings.name}", T.display(46), maxLines = 2)
                }
                RoundButton("apps", "All apps", { nav.go(Screen.AllApps) }, size = 56.dp)
                RoundButton("pencil", "Edit home screen", { nav.go(Screen.Edit()) }, size = 56.dp)
            }

            // Mascot and bubble
            MascotCard(bubbleText(todays.size, todaysEvents.firstOrNull()?.title, today, Focus.isRunning(timer)))

            // Tiles
            TileGrid(settings.tiles) { openTile(ctx, nav, it) }

            // Today + focus timer
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(Modifier.weight(1f), spacing = 16.dp) {
                    Txt("Today", T.display(26, 500))
                    if (todays.isEmpty() && todaysEvents.isEmpty()) {
                        Txt("All clear for today.", T.body(18), color = p.muted)
                    }
                    todaysEvents.take(2).forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(p.mint).border(1.dp, p.ink, RoundedCornerShape(6.dp)))
                            Txt(e.title, T.body(19), Modifier.weight(1f), maxLines = 1)
                            Txt(e.minutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "All day", T.body(17, 600), color = p.muted)
                        }
                    }
                    todays.take(4).forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CheckMark(false, { Store.toggleReminder(r.id) }, size = 28.dp)
                            Txt(r.title, T.body(19), Modifier.weight(1f), maxLines = 1)
                            val d = r.dueDateTime()
                            val label = when {
                                d == null -> ""
                                d.toLocalDate().isBefore(today) -> "Overdue"
                                r.hasTime -> d.format(DateTimeFormatter.ofPattern("HH:mm"))
                                else -> ""
                            }
                            Txt(label, T.body(17, 600), color = if (label == "Overdue") p.holiday else p.muted)
                        }
                    }
                    if (todays.size > 4) Txt("and ${todays.size - 4} more", T.body(16, 600), color = p.muted)
                    Pill("+ Add reminder", { nav.go(Screen.Reminders) }, style = PillStyle.SOFT)
                }
                FocusMini(timer, now) { nav.go(Screen.Timer) }
            }
        }
    }
}

internal fun bubbleText(reminders: Int, eventTitle: String?, today: LocalDate, timerRunning: Boolean): String {
    val first = when (reminders) {
        0 -> "Nothing on your list today"
        1 -> "You have 1 reminder today"
        else -> "You have $reminders reminders today"
    }
    val holidayToday = Holidays.name(today)
    val holidayTomorrow = Holidays.name(today.plusDays(1))
    val second = when {
        holidayToday != null -> ", and it's $holidayToday!"
        eventTitle != null -> " and $eventTitle on the calendar."
        holidayTomorrow != null -> " and tomorrow is $holidayTomorrow."
        else -> "."
    }
    val third = if (timerRunning) " Your focus timer is running." else " Your focus timer is ready when you are."
    return first + second + third
}

@Composable
private fun MascotCard(text: String) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(36.dp)
    var m = Modifier.fillMaxWidth().clip(shape).background(p.accent)
    if (p.eink) m = m.border(p.line, p.ink, shape)
    Box(m) {
        Sparkle(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 40.dp), size = 22.dp, color = if (p.eink) p.ink else androidx.compose.ui.graphics.Color.White)
        Sparkle(Modifier.align(Alignment.BottomEnd).padding(bottom = 26.dp, end = 120.dp), size = 14.dp, delayMs = 700, color = if (p.eink) p.ink else androidx.compose.ui.graphics.Color.White)
        Row(
            Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Mascot(size = 140.dp)
            Column(
                Modifier.weight(1f, fill = false)
                    .clip(RoundedCornerShape(26.dp))
                    .background(p.card)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Txt("Hi hi!", T.display(24, 500))
                Txt(text, T.body(19), color = p.ink)
            }
        }
    }
}

@Composable
fun TileGrid(tiles: List<Tile>, onClick: (Tile) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        tiles.chunked(3).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                chunk.forEachIndexed { col, tile ->
                    val shape = RoundedCornerShape(28.dp)
                    val colour = if ((row * 3 + col) % 2 == 0) p.accent else p.mint
                    Column(
                        Modifier.weight(1f)
                            .clip(shape)
                            .background(p.card)
                            .border(p.line, p.border, shape)
                            .clickable(onClickLabel = "Open ${tile.label}", role = Role.Button) { onClick(tile) }
                            .padding(vertical = 24.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TileIcon(tile, colour, 76)
                        Txt(tile.label, T.display(22, 500), maxLines = 1, align = TextAlign.Center)
                    }
                }
                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A tile's icon: its line icon on a soft square, or the app's own icon if chosen. */
@Composable
fun TileIcon(tile: Tile, colour: androidx.compose.ui.graphics.Color, sizeDp: Int) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape((sizeDp * 0.31f).dp)
    var m = Modifier.size(sizeDp.dp).clip(shape).background(colour)
    if (p.eink) m = m.border(1.5.dp, p.ink, shape)
    Box(m, contentAlignment = Alignment.Center) {
        CozyIcon(tile.icon, size = (sizeDp * 0.45f).dp)
    }
}

@Composable
private fun FocusMini(timer: app.cozy.launcher.data.TimerState, now: Long, openTimer: () -> Unit) {
    val p = LocalPalette.current
    val ms = Focus.remainingMs(timer, now)
    val sec = ((ms + 999) / 1000).toInt()
    val running = Focus.isRunning(timer)
    Card(
        Modifier.width(250.dp),
        color = p.mint,
        border = if (p.eink) p.ink else null,
        padding = PaddingValues(vertical = 26.dp, horizontal = 18.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt(if (timer.mode == Focus.FOCUS) "Focus timer" else Focus.label(timer.mode), T.display(22, 500))
            Txt("%02d:%02d".format(sec / 60, sec % 60), T.display(58), maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 25).forEach { m ->
                    val selected = timer.totalSec == m * 60
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (selected) p.ink else p.card)
                            .clickable(onClickLabel = "$m minutes", role = Role.Button) { Focus.setMinutes(m) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Txt("${m}m", T.body(15, 700), color = if (selected) p.onInk else p.ink) }
                }
            }
            Pill(
                if (running) "Open" else "Start",
                {
                    if (!running) Focus.start()
                    openTimer()
                },
                Modifier.fillMaxWidth(),
                style = PillStyle.DARK,
            )
        }
    }
}

@Composable
fun AppIcon(pkg: String, sizeDp: Int) {
    val ctx = LocalContext.current
    val bmp = Apps.icon(ctx, pkg)
    if (bmp != null) {
        Image(bmp, contentDescription = null, modifier = Modifier.size(sizeDp.dp))
    } else {
        CozyIcon("apps", size = sizeDp.dp)
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.5.dp).background(LocalPalette.current.border))
}
