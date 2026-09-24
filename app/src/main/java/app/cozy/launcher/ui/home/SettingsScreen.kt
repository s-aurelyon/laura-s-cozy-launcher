package app.cozy.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.themeId
import app.cozy.launcher.system.Apps
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.Toggle
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import app.cozy.launcher.ui.theme.accentChoices

@Composable
fun SettingsScreen(nav: Navigator) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val s by Store.settings.collectAsState()

    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Header("Settings", { nav.back() })

            Card(spacing = 16.dp) {
                SectionLabel("Your name")
                CozyField(s.name, { v -> Store.updateSettings { it.copy(name = v) } }, "Name")

                SectionLabel("Colour")
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    accentChoices.forEach { c ->
                        val on = s.accent == c
                        val shape = RoundedCornerShape(26.dp)
                        Box(
                            Modifier.size(52.dp).clip(shape).background(Color(c))
                                .border(if (on) 3.dp else 1.5.dp, if (on) p.ink else p.border, shape)
                                .clickable(onClickLabel = "Choose colour", role = Role.RadioButton) { Store.updateSettings { it.copy(accent = c) } },
                            contentAlignment = Alignment.Center,
                        ) { if (on) CozyIcon("check", size = 22.dp, weight = 2.6f) }
                    }
                }
            }

            Card(spacing = 18.dp) {
                SettingRow("Cute animations", "The mascot bobs and blinks, sparkles twinkle") {
                    Toggle(s.animations, { v -> Store.updateSettings { it.copy(animations = v) } }, "Animations")
                }
                SettingRow("Theme", when (s.themeId()) {
                    "meadow" -> "Meadow: painted skies and strawberries"
                    "paper" -> "Paper: black and white for e-ink"
                    else -> "Cozy cream"
                }) {
                    Pill("Change", { nav.go(Screen.Themes) }, style = PillStyle.LIGHT, icon = "palette")
                }
                SettingRow("Week starts on Monday", "Otherwise the calendar starts on Sunday") {
                    Toggle(s.weekStartsMonday, { v -> Store.updateSettings { it.copy(weekStartsMonday = v) } }, "Week starts Monday")
                }
            }

            Card(spacing = 14.dp) {
                SectionLabel("Pen hold for a text box")
                Txt("How long to hold the pen still before a text box and keyboard appear.", T.body(16), color = p.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(800L to "Quick", 1200L to "Normal", 2000L to "Long").forEach { (ms, label) ->
                        Chip("$label · ${ms / 1000.0}s", s.penHoldMs == ms, { Store.updateSettings { it.copy(penHoldMs = ms) } })
                    }
                }
            }

            Card(spacing = 12.dp) {
                SectionLabel("Setup")
                Pill("Make Cozy the home screen", { Apps.openHomeSettings(ctx) }, Modifier.fillMaxWidth(), style = PillStyle.DARK, icon = "apps")
                Txt("Choose Cozy as the Home app. On Xiaomi this is under Settings › Apps › Default apps › Home app.", T.body(15), color = p.muted)
                Pill("Notification settings", { Apps.openNotificationSettings(ctx) }, Modifier.fillMaxWidth(), style = PillStyle.LIGHT, icon = "bell")
                Pill("Device settings", { Apps.openDeviceSettings(ctx) }, Modifier.fillMaxWidth(), style = PillStyle.LIGHT, icon = "settings")
            }

            Txt("Cozy 1.0 · made for ${s.name.ifBlank { "you" }}", T.body(15), color = p.muted, modifier = Modifier.padding(bottom = 20.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, T.body(19, 700))
            Txt(subtitle, T.body(15), color = p.muted)
        }
        control()
    }
}
