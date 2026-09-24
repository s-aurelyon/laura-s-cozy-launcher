package app.cozy.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Focus
import app.cozy.launcher.data.Holidays
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Tile
import app.cozy.launcher.ui.CheckMark
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.meadow.CupView
import app.cozy.launcher.ui.meadow.GinghamHeader
import app.cozy.launcher.ui.meadow.MeadowIcon
import app.cozy.launcher.ui.meadow.MeadowScene
import app.cozy.launcher.ui.meadow.PaperTag
import app.cozy.launcher.ui.meadow.SceneBunny
import app.cozy.launcher.ui.meadow.SceneCloud
import app.cozy.launcher.ui.meadow.paperShadow
import app.cozy.launcher.ui.meadow.skyVariant
import app.cozy.launcher.ui.rememberNow
import app.cozy.launcher.ui.theme.LocalAnimate
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun MeadowHomeScreen(nav: Navigator) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val settings by Store.settings.collectAsState()
    val reminders by Store.reminders.collectAsState()
    val events by Store.events.collectAsState()
    val timer by Store.timer.collectAsState()
    val now = rememberNow(1000)
    val today = LocalDate.now()
    val animate = LocalAnimate.current

    val todays = reminders.filter { !it.done && it.dueDate() != null && !it.dueDate()!!.isAfter(today) }.sortedBy { it.due }
    val todaysEvents = events.filter { it.epochDay == today.toEpochDay() }.sortedBy { it.minutes ?: -1 }
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 5 -> "Hello night owl"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val holiday = Holidays.name(today)
    val hello = if (holiday != null) "Hi hi, happy $holiday!" else "Hi hi!"

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            // The painted meadow
            Box(Modifier.fillMaxWidth().height(520.dp)) {
                MeadowScene(
                    Modifier.fillMaxSize(),
                    variant = skyVariant(settings, hour),
                    hy = 330.dp, seed = 3,
                    tall = SceneCloud(0.65f, 0f, 1.25f),
                    clouds = listOf(SceneCloud(0.11f, 118f, 0.7f), SceneCloud(0.86f, 84f, 0.55f), SceneCloud(0.41f, 60f, 0.45f)),
                    bunnies = if (settings.bunnies) listOf(
                        SceneBunny(0.70f, 492f, 1.15f, true), SceneBunny(0.86f, 478f, 0.85f), SceneBunny(0.31f, 500f, 0.75f),
                    ) else emptyList(),
                    flowers = 120,
                    picnic = settings.scene == "picnic",
                    wave = p.bg,
                    animate = animate && settings.driftClouds,
                )
                Box(Modifier.widthIn(max = 860.dp).fillMaxSize().align(Alignment.TopCenter)) {
                    Mascot(Modifier.offset(x = 38.dp, y = 312.dp), size = 162.dp, berry = true)

                    Row(
                        Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 36.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            PaperTag {
                                Txt(today.format(DateTimeFormatter.ofPattern("EEEE · d MMMM")), T.body(17, 700), color = p.muted)
                                Txt("$greeting, ${settings.name}", T.display(40), maxLines = 2)
                            }
                        }
                        RoundButton("apps", "All apps", { nav.go(Screen.AllApps) })
                        RoundButton("palette", "Theme", { nav.go(Screen.Themes) })
                        RoundButton("pencil", "Edit home screen", { nav.go(Screen.Edit()) })
                    }

                    // Speech bubble
                    Box(Modifier.offset(x = 228.dp, y = 246.dp).widthIn(max = 400.dp)) {
                        Canvas(Modifier.offset(x = 0.dp, y = 60.dp).size(16.dp, 28.dp)) {
                            val tri = Path().apply {
                                moveTo(size.width + 2f, 0f)
                                lineTo(0f, size.height / 2f)
                                lineTo(size.width + 2f, size.height)
                                close()
                            }
                            drawPath(tri, p.card)
                            drawLine(p.border, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height / 2f), 2f * density)
                            drawLine(p.border, androidx.compose.ui.geometry.Offset(0f, size.height / 2f), androidx.compose.ui.geometry.Offset(size.width, size.height), 2f * density)
                        }
                        PaperTag(Modifier.padding(start = 14.dp), padding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)) {
                            Txt(hello, T.display(23, 500))
                            Txt(
                                meadowBubble(todays.size, todaysEvents.firstOrNull()?.title, today, Focus.isRunning(timer), hour),
                                T.body(18), Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            Column(
                Modifier.widthIn(max = 860.dp).fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                MeadowTileGrid(settings.tiles) { openTile(ctx, nav, it) }

                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                    // Today, on a picnic cloth
                    val shape = RoundedCornerShape(28.dp)
                    Column(
                        Modifier.weight(1f).paperShadow(28.dp, p.border, 5.dp).clip(shape).background(p.card).border(2.dp, p.border, shape)
                    ) {
                        GinghamHeader(if (holiday != null) "Today · $holiday" else "Today")
                        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (todays.isEmpty() && todaysEvents.isEmpty()) {
                                Txt("A free day. Maybe a picnic?", T.hand(26), color = p.muted)
                            }
                            todaysEvents.take(2).forEach { e ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(p.mint).border(1.5.dp, p.ink, RoundedCornerShape(7.dp)))
                                    }
                                    Txt(e.title, T.body(19), Modifier.weight(1f), maxLines = 1)
                                    Txt(e.minutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "All day", T.body(17, 700), color = p.muted)
                                }
                            }
                            todays.take(4).forEach { r ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    CheckMark(false, { Store.toggleReminder(r.id) }, size = 30.dp)
                                    Txt(r.title, T.body(19), Modifier.weight(1f), maxLines = 1)
                                    val d = r.dueDateTime()
                                    val label = when {
                                        d == null -> ""
                                        d.toLocalDate().isBefore(today) -> "Overdue"
                                        r.hasTime -> d.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        else -> ""
                                    }
                                    Txt(label, T.body(17, 700), color = if (label == "Overdue") p.holiday else p.muted)
                                }
                            }
                            if (todays.size > 4) Txt("and ${todays.size - 4} more", T.body(16, 600), color = p.muted)
                            if (todays.isNotEmpty()) Txt("tick one off and a strawberry goes in the basket", T.hand(23), color = p.muted)
                            Pill("+ Add reminder", { nav.go(Screen.Reminders) }, style = PillStyle.SOFT)
                        }
                    }

                    // Focus drink
                    val running = Focus.isRunning(timer)
                    val ms = Focus.remainingMs(timer, now)
                    val sec = ((ms + 999) / 1000).toInt()
                    val frac = if (timer.totalSec > 0) 1f - (ms / 1000f / timer.totalSec) else 0f
                    val skyShape = RoundedCornerShape(28.dp)
                    Column(
                        Modifier.width(250.dp).paperShadow(28.dp, p.skyBorder, 5.dp).clip(skyShape).background(p.skyCard)
                            .border(2.dp, p.skyBorder, skyShape).padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CupView(if (running || timer.remainingSec < timer.totalSec) frac else 0.35f, timer.mode, 92.dp, bubbling = running)
                        Txt("%02d:%02d".format(sec / 60, sec % 60), T.display(52), maxLines = 1)
                        Txt(drinkName(timer.mode), T.hand(24), color = p.ink)
                        Pill(
                            if (running) "Open" else "Start",
                            {
                                if (!running) Focus.start()
                                nav.go(Screen.Timer)
                            },
                            Modifier.fillMaxWidth(),
                            style = PillStyle.DARK,
                        )
                    }
                }
            }
        }
    }
}

fun drinkName(mode: String) = when (mode) {
    Focus.SHORT -> "matcha break"
    Focus.LONG -> "honey nap"
    else -> "strawberry focus"
}

private fun meadowBubble(reminders: Int, eventTitle: String?, today: LocalDate, timerRunning: Boolean, hour: Int): String {
    val first = when (reminders) {
        0 -> "Nothing on your list today"
        1 -> "One little reminder today"
        2 -> "Two little reminders today"
        else -> "$reminders little reminders today"
    }
    val second = when {
        eventTitle != null -> ", and $eventTitle on the calendar."
        Holidays.name(today.plusDays(1)) != null -> ", and tomorrow is ${Holidays.name(today.plusDays(1))}."
        else -> "."
    }
    val third = when {
        timerRunning -> " Your focus drink is filling up."
        hour >= 19 || hour < 5 -> " The stars are out. Time to get cozy."
        hour >= 16 -> " Golden hour, the prettiest part of the day."
        else -> " The sun is out, perfect for a picnic."
    }
    return first + second + third
}

@Composable
fun MeadowTileGrid(tiles: List<Tile>, onClick: (Tile) -> Unit) {
    val p = LocalPalette.current
    val colours = listOf(p.accent, p.mint, p.skyCard, p.butter)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        tiles.chunked(3).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                chunk.forEachIndexed { col, tile ->
                    val shape = RoundedCornerShape(28.dp)
                    Column(
                        Modifier.weight(1f)
                            .paperShadow(28.dp, p.border, 5.dp)
                            .clip(shape)
                            .background(p.card)
                            .border(2.dp, p.border, shape)
                            .clickable(onClickLabel = "Open ${tile.label}", role = Role.Button) { onClick(tile) }
                            .padding(top = 22.dp, bottom = 20.dp, start = 10.dp, end = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(84.dp).clip(RoundedCornerShape(42.dp)).background(colours[(row * 3 + col) % colours.size]),
                            contentAlignment = Alignment.Center,
                        ) { MeadowIcon(tile.icon, 56.dp) }
                        Txt(tile.label, T.display(22, 500), maxLines = 1, align = TextAlign.Center)
                    }
                }
                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
