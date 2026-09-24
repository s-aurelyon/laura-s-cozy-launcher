package app.cozy.launcher.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Focus
import app.cozy.launcher.data.Store
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Mood
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.Toggle
import app.cozy.launcher.ui.home.Divider
import app.cozy.launcher.ui.meadow.CupView
import app.cozy.launcher.ui.meadow.MeadowScene
import app.cozy.launcher.ui.meadow.PaperTag
import app.cozy.launcher.ui.meadow.SceneBunny
import app.cozy.launcher.ui.meadow.SceneCloud
import app.cozy.launcher.ui.meadow.StrawberryCheck
import app.cozy.launcher.ui.meadow.gingham
import app.cozy.launcher.ui.meadow.paperShadow
import app.cozy.launcher.ui.meadow.skyVariant
import app.cozy.launcher.ui.rememberNow
import app.cozy.launcher.ui.theme.LocalAnimate
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

private fun meadowLabel(mode: String) = when (mode) {
    Focus.SHORT -> "Matcha break"
    Focus.LONG -> "Honey nap"
    else -> "Strawberry focus"
}

/** The Meadow focus timer: a drink that fills up while she focuses. */
@Composable
fun MeadowTimerScreen(nav: Navigator) {
    val p = LocalPalette.current
    val timer by Store.timer.collectAsState()
    val settings by Store.settings.collectAsState()
    val now = rememberNow(200)
    val running = Focus.isRunning(timer)
    val ms = Focus.remainingMs(timer, now)
    val sec = ((ms + 999) / 1000).toInt()
    val frac = if (timer.totalSec > 0) (1f - ms / 1000f / timer.totalSec).coerceIn(0f, 1f) else 0f
    val finished = timer.finishedMode != null && !running
    var customOpen by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(running, settings.keepAwake) {
        view.keepScreenOn = running && settings.keepAwake
        onDispose { view.keepScreenOn = false }
    }

    val mood = when {
        finished -> Mood.JOY
        running -> Mood.HAPPY
        else -> Mood.SLEEPY
    }
    val status = when {
        timer.finishedMode == Focus.FOCUS && !running -> "all done! time for a little stretch"
        finished -> "break's over, ready when you are"
        running && timer.mode == Focus.FOCUS -> "sip, sip… you're doing great"
        running -> "enjoy your little break"
        sec < timer.totalSec -> "paused, take your time"
        else -> "ready when you are"
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
        val sceneH = maxHeight.coerceAtLeast(1100.dp)
        MeadowScene(
            Modifier.fillMaxWidth().height(sceneH),
            variant = skyVariant(settings),
            hy = 560.dp, dm = 50.dp, df = 96.dp, seed = 17,
            tall = SceneCloud(0.59f, 0f, 1.35f),
            clouds = listOf(SceneCloud(0.11f, 150f, 0.7f), SceneCloud(0.86f, 110f, 0.55f)),
            bunnies = if (settings.bunnies) listOf(SceneBunny(0.78f, 690f, 1.05f, true), SceneBunny(0.9f, 710f, 0.8f)) else emptyList(),
            flowers = 170,
            picnic = false,
            animate = LocalAnimate.current && settings.driftClouds,
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 860.dp).fillMaxWidth().padding(horizontal = 40.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    RoundButton("back", "Back", { nav.back() })
                    PaperTag(padding = PaddingValues(horizontal = 22.dp, vertical = 8.dp)) { Txt("Focus", T.display(38)) }
                }

                // Drinks
                val tabShape = RoundedCornerShape(28.dp)
                Row(
                    Modifier.paperShadow(28.dp).clip(tabShape).background(p.card).border(2.dp, p.border, tabShape).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(Focus.FOCUS, Focus.SHORT, Focus.LONG).forEach { m ->
                        val on = timer.mode == m
                        Box(
                            Modifier.clip(RoundedCornerShape(22.dp)).background(if (on) p.ink else Color.Transparent)
                                .clickable(onClickLabel = meadowLabel(m), role = Role.Tab) { Focus.setMode(m) }
                                .padding(horizontal = 22.dp, vertical = 12.dp)
                        ) { Txt(meadowLabel(m), T.body(17, 700), color = if (on) p.onInk else p.ink) }
                    }
                }

                // The cup on its little picnic cloth, with the mascot keeping company
                Box(Modifier.width(560.dp).height(480.dp)) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).offset(y = (-10).dp).size(360.dp, 110.dp)
                            .paperShadow(55.dp, Color(0x264A3A2E), 6.dp).clip(RoundedCornerShape(55.dp)).gingham()
                    )
                    Mascot(Modifier.align(Alignment.BottomStart).offset(x = 10.dp, y = (-40).dp), size = 138.dp, mood = mood, bob = running)
                    CupView(frac, timer.mode, 300.dp, bubbling = running, modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-40).dp))
                }

                PaperTag(padding = PaddingValues(horizontal = 36.dp, vertical = 10.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt("%02d:%02d".format(sec / 60, sec % 60), T.display(66), maxLines = 1)
                        Txt(status, T.hand(26), color = p.muted)
                    }
                }

                // Controls
                val panel = RoundedCornerShape(30.dp)
                Column(
                    Modifier.fillMaxWidth().paperShadow(30.dp, Color(0x244A3A2E), 6.dp).clip(panel).background(p.card)
                        .border(2.dp, p.border, panel).padding(horizontal = 28.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val done = timer.session % 4
                        repeat(4) { i -> StrawberryCheck(i < done, 30.dp) }
                        Txt(
                            "Session ${done + 1} of 4, then a honey nap",
                            T.body(16, 700), Modifier.padding(start = 8.dp), color = p.muted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(5, 15, 25, 45).forEach { m ->
                            val on = timer.totalSec == m * 60
                            val sh = RoundedCornerShape(18.dp)
                            Box(
                                Modifier.clip(sh).background(if (on) p.accent else p.card)
                                    .border(2.dp, if (on) p.ink else p.border, sh)
                                    .clickable(onClickLabel = "$m minutes", role = Role.Button) { Focus.setMinutes(m) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) { Txt("$m min", T.body(17, 700)) }
                        }
                        Pill("Custom", { customOpen = true }, style = PillStyle.DASHED, padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Pill("Reset", { Focus.reset() }, Modifier.width(160.dp), style = PillStyle.LIGHT, textSize = 20, padding = PaddingValues(18.dp))
                        val label = when {
                            running -> "Pause"
                            sec < timer.totalSec -> "Resume"
                            timer.finishedMode != null -> "Start ${meadowLabel(timer.mode).lowercase()}"
                            else -> "Start"
                        }
                        Pill(label, { if (running) Focus.pause() else Focus.start() }, Modifier.weight(1f), style = PillStyle.DARK, textSize = 20, padding = PaddingValues(18.dp))
                    }
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt("Focusing on", T.body(18, 700), Modifier.weight(1f))
                        CozyField(timer.focusingOn, { Focus.setFocusingOn(it) }, "something lovely", Modifier.width(300.dp), style = T.hand(26).copy(color = Color(0xFF5B7DB1)), boxed = false)
                    }
                    Divider()
                    TimerSetting("Sound when done") {
                        Toggle(settings.timerSound, { v -> Store.updateSettings { it.copy(timerSound = v) } }, "Sound when done")
                    }
                    TimerSetting("Start breaks automatically") {
                        Toggle(settings.autoStartBreaks, { v -> Store.updateSettings { it.copy(autoStartBreaks = v) } }, "Start breaks automatically")
                    }
                    TimerSetting("Keep screen awake") {
                        Toggle(settings.keepAwake, { v -> Store.updateSettings { it.copy(keepAwake = v) } }, "Keep screen awake")
                    }
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
private fun TimerSetting(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, T.body(18, 600), Modifier.weight(1f))
        control()
    }
}
