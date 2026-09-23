package app.cozy.launcher.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Focus
import app.cozy.launcher.data.Store
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Mood
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.Toggle
import app.cozy.launcher.ui.home.Divider
import app.cozy.launcher.ui.rememberNow
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

@Composable
fun TimerScreen(nav: Navigator) {
    val p = LocalPalette.current
    val timer by Store.timer.collectAsState()
    val settings by Store.settings.collectAsState()
    val now = rememberNow(200)
    val running = Focus.isRunning(timer)
    val ms = Focus.remainingMs(timer, now)
    val sec = ((ms + 999) / 1000).toInt()
    val progress = if (timer.totalSec > 0) (ms / 1000f / timer.totalSec).coerceIn(0f, 1f) else 0f
    var customOpen by remember { mutableStateOf(false) }

    // Keep the screen on while focusing, if she wants that.
    val view = LocalView.current
    DisposableEffect(running, settings.keepAwake) {
        view.keepScreenOn = running && settings.keepAwake
        onDispose { view.keepScreenOn = false }
    }

    val mood = when {
        timer.finishedMode != null && !running -> Mood.JOY
        running -> Mood.HAPPY
        else -> Mood.SLEEPY
    }
    val status = when {
        timer.finishedMode == Focus.FOCUS && !running -> "All done, time for a stretch!"
        timer.finishedMode != null && !running -> "Break's over. Ready when you are."
        running && timer.mode == Focus.FOCUS -> "Focusing…"
        running -> "Enjoy your break"
        sec < timer.totalSec -> "Paused"
        else -> "Ready when you are"
    }

    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Header("Focus", { nav.back() })

            // Modes
            Row(
                Modifier.clip(RoundedCornerShape(26.dp)).background(p.card).border(p.line, p.border, RoundedCornerShape(26.dp)).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(Focus.FOCUS, Focus.SHORT, Focus.LONG).forEach { m ->
                    val on = timer.mode == m
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) p.ink else Color.Transparent)
                            .clickable(onClickLabel = Focus.label(m), role = Role.Tab) { Focus.setMode(m) }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) { Txt(Focus.label(m), T.body(17, 700), color = if (on) p.onInk else p.ink) }
                }
            }

            // Ring
            Box(Modifier.size(360.dp), contentAlignment = Alignment.Center) {
                val ringColor = if (p.eink) p.ink else if (timer.mode == Focus.FOCUS) p.blushInk else Color(0xFF8FC7A6)
                Canvas(Modifier.size(360.dp)) {
                    val w = 22.dp.toPx()
                    val inset = w / 2 + 4.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    drawCircle(Color.White, radius = size.width / 2 - inset)
                    drawArc(p.border, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
                    drawArc(ringColor, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Mascot(size = 120.dp, mood = mood, bob = running)
                    Txt("%02d:%02d".format(sec / 60, sec % 60), T.display(66), maxLines = 1)
                    Txt(status, T.body(16, 700), color = p.muted)
                }
            }

            // Session dots
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val doneCount = timer.session % 4
                repeat(4) { i ->
                    val shape = RoundedCornerShape(7.dp)
                    val isCurrent = i == doneCount && timer.mode == Focus.FOCUS
                    Box(
                        Modifier.size(14.dp).clip(shape)
                            .background(if (i < doneCount) p.ink else if (isCurrent) p.accent else Color.Transparent)
                            .border(2.dp, if (i < doneCount || isCurrent) p.ink else p.dashed, shape)
                    )
                }
                Txt("Session ${(timer.session % 4) + 1} of 4, then a long break", T.body(16, 700), color = p.muted)
            }

            // Presets
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(5, 15, 25, 45).forEach { m ->
                    val on = timer.totalSec == m * 60
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        Modifier.clip(shape).background(if (on) p.accent else p.card)
                            .border(2.dp, if (on) p.ink else p.border, shape)
                            .clickable(onClickLabel = "$m minutes", role = Role.Button) { Focus.setMinutes(m) }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) { Txt("$m min", T.body(17, 700)) }
                }
                Pill("Custom", { customOpen = true }, style = PillStyle.DASHED, padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp))
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Pill("Reset", { Focus.reset() }, Modifier.width(150.dp), style = PillStyle.LIGHT, textSize = 20, padding = PaddingValues(18.dp))
                val label = when {
                    running -> "Pause"
                    sec < timer.totalSec -> "Resume"
                    timer.finishedMode != null -> "Start ${Focus.label(timer.mode).lowercase()}"
                    else -> "Start"
                }
                Pill(label, { if (running) Focus.pause() else Focus.start() }, Modifier.width(250.dp), style = PillStyle.DARK, textSize = 20, padding = PaddingValues(18.dp))
            }

            // Settings
            Card(Modifier.fillMaxWidth(), padding = PaddingValues(0.dp), spacing = 0.dp) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt("Focusing on", T.body(18, 600), Modifier.weight(1f))
                    CozyField(timer.focusingOn, { Focus.setFocusingOn(it) }, "Something lovely", Modifier.width(300.dp), style = T.body(17))
                }
                Divider()
                SettingLine("Sound when done") {
                    Toggle(settings.timerSound, { v -> Store.updateSettings { it.copy(timerSound = v) } }, "Sound when done")
                }
                Divider()
                SettingLine("Start breaks automatically") {
                    Toggle(settings.autoStartBreaks, { v -> Store.updateSettings { it.copy(autoStartBreaks = v) } }, "Start breaks automatically")
                }
                Divider()
                SettingLine("Keep screen awake") {
                    Toggle(settings.keepAwake, { v -> Store.updateSettings { it.copy(keepAwake = v) } }, "Keep screen awake")
                }
            }
        }
    }

    if (customOpen) {
        var text by remember { mutableStateOf("") }
        CozyDialog({ customOpen = false }, "Custom time") {
            CozyField(
                text, { v -> text = v.filter { it.isDigit() }.take(3) }, "Minutes", Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            DialogButtons("Set", {
                text.toIntOrNull()?.let { if (it > 0) Focus.setMinutes(it) }
                customOpen = false
            }, { customOpen = false })
        }
    }
}

@Composable
private fun SettingLine(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, T.body(18, 600), Modifier.weight(1f))
        control()
    }
}
