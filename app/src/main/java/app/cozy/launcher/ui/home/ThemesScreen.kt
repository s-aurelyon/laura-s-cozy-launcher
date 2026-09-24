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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.themeId
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.Toggle
import app.cozy.launcher.ui.meadow.MeadowScene
import app.cozy.launcher.ui.meadow.SceneBunny
import app.cozy.launcher.ui.meadow.SceneCloud
import app.cozy.launcher.ui.meadow.drawStrawberry
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

/** Settings › Theme: Cozy cream, Meadow or Paper, plus the Meadow extras. */
@Composable
fun ThemesScreen(nav: Navigator) {
    val p = LocalPalette.current
    val s by Store.settings.collectAsState()
    val current = s.themeId()

    fun choose(id: String) = Store.updateSettings { it.copy(theme = if (id == "paper") it.theme else id, eink = id == "paper") }

    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Header("Theme", { nav.back() })
            Txt("Same apps, same notes. Just a different mood.", T.body(17), color = p.muted)

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ThemeCard("Cozy cream", "Soft and simple", current == "cozy", Modifier.weight(1f), { choose("cozy") }) {
                    Canvas(Modifier.fillMaxSize()) { drawCozyMini() }
                }
                ThemeCard("Meadow", "Painted skies, strawberries", current == "meadow", Modifier.weight(1f), { choose("meadow") }) {
                    Column(Modifier.fillMaxSize()) {
                        MeadowScene(
                            Modifier.fillMaxWidth().height(150.dp), variant = "sunny", hy = 92.dp, dm = 18.dp, df = 36.dp, seed = 31,
                            tall = SceneCloud(0.6f, 0f, 0.33f), clouds = listOf(SceneCloud(0.15f, 30f, 0.2f)),
                            bunnies = listOf(SceneBunny(0.75f, 142f, 0.4f, true)), flowers = 50, animate = false,
                        )
                        Canvas(Modifier.fillMaxSize()) { drawMeadowMiniTiles() }
                    }
                }
                ThemeCard("Paper", "Black and white for e-ink", current == "paper", Modifier.weight(1f), { choose("paper") }) {
                    Canvas(Modifier.fillMaxSize()) { drawPaperMini() }
                }
            }

            SectionLabel("Meadow scene")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    Triple("sunny", "Sunny meadow", "sunny"),
                    Triple("picnic", "Strawberry picnic", "sunny"),
                    Triple("golden", "Golden hour", "golden"),
                    Triple("night", "Starry night", "night"),
                ).forEach { (key, name, variant) ->
                    val on = s.scene == key
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        Modifier.weight(1f).clip(shape).background(p.card)
                            .border(if (on) 3.dp else 2.dp, if (on) p.ink else p.border, shape)
                            .clickable(onClickLabel = name, role = Role.RadioButton) { Store.updateSettings { it.copy(scene = key) } }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MeadowScene(
                            Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)),
                            variant = variant, hy = 66.dp, dm = 16.dp, df = 30.dp, seed = 41,
                            tall = if (variant != "night") SceneCloud(0.58f, 0f, 0.28f) else null,
                            clouds = listOf(SceneCloud(0.15f, 26f, 0.18f)),
                            bunnies = if (key == "sunny") listOf(SceneBunny(0.78f, 104f, 0.34f, true)) else emptyList(),
                            flowers = 40, picnic = key == "picnic", animate = false, mountains = true,
                        )
                        Txt(name, T.body(16, 700), Modifier.padding(horizontal = 4.dp), maxLines = 1)
                    }
                }
            }
            if (s.skyFollowsTime) {
                Txt("The sky follows the time of day while that's on, so the scene only picks the extras.", T.body(15), color = p.muted)
            }

            Card(padding = PaddingValues(0.dp), spacing = 0.dp) {
                ThemeToggle("Sky follows the time of day", "Morning blue, golden evenings, starry nights", s.skyFollowsTime) { v ->
                    Store.updateSettings { it.copy(skyFollowsTime = v) }
                }
                Divider()
                ThemeToggle("Little bunnies hop around", "They hop now and then, never while you write", s.bunnies) { v ->
                    Store.updateSettings { it.copy(bunnies = v) }
                }
                Divider()
                ThemeToggle("Drifting clouds", "Slow and gentle. Off saves a little battery", s.driftClouds) { v ->
                    Store.updateSettings { it.copy(driftClouds = v) }
                }
                Divider()
                ThemeToggle("Stickers in Notes", "Strawberries, bunnies, clouds, daisies and more", s.stickers) { v ->
                    Store.updateSettings { it.copy(stickers = v) }
                }
            }

            Pill("Done", { nav.back() }, Modifier.fillMaxWidth(), style = PillStyle.DARK, textSize = 19, padding = PaddingValues(18.dp))
        }
    }
}

@Composable
private fun ThemeCard(name: String, desc: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit, preview: @Composable () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.clip(shape).background(p.card)
            .border(if (selected) 3.dp else 2.dp, if (selected) p.ink else p.border, shape)
            .clickable(onClickLabel = "Use $name", role = Role.RadioButton, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, p.border, RoundedCornerShape(16.dp))) {
            preview()
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(36.dp).clip(RoundedCornerShape(18.dp)).background(p.ink),
                    contentAlignment = Alignment.Center,
                ) { CozyIcon("check", size = 20.dp, tint = p.onInk, weight = 3f) }
            }
        }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Txt(name, T.display(22, 500), maxLines = 1)
            Txt(desc, T.body(15), color = p.muted, maxLines = 2)
        }
    }
}

@Composable
private fun ThemeToggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Txt(title, T.body(19, 800))
            Txt(sub, T.body(15), color = p.muted)
        }
        Toggle(value, onChange, title)
    }
}

// ---- Little previews, drawn to fit their box ----

private fun DrawScope.u() = size.width / 200f

private fun DrawScope.rr(c: Color, x: Float, y: Float, w: Float, h: Float, r: Float, stroke: Float? = null) {
    val k = u()
    if (stroke == null) drawRoundRect(c, Offset(x * k, y * k), Size(w * k, h * k), CornerRadius(r * k))
    else drawRoundRect(c, Offset(x * k, y * k), Size(w * k, h * k), CornerRadius(r * k), style = Stroke(stroke * k))
}

private fun DrawScope.drawCozyMini() {
    drawRect(Color(0xFFFBF6EF))
    rr(Color(0xFF3A2E2A), 14f, 16f, 110f, 12f, 6f)
    rr(Color(0xFFB8A898), 14f, 34f, 70f, 7f, 3.5f)
    rr(Color(0xFFF4C2CB), 14f, 54f, 172f, 62f, 16f)
    rr(Color.White, 84f, 70f, 90f, 30f, 10f)
    val k = u()
    drawCircle(Color(0xFFFFFDF8), 20f * k, Offset(46f * k, 88f * k))
    drawCircle(Color(0xFF3A2E2A), 20f * k, Offset(46f * k, 88f * k), style = Stroke(1.6f * k))
    for (r in 0 until 2) for (c in 0 until 3) {
        rr(Color.White, 14f + c * 59f, 128f + r * 62f, 54f, 54f, 12f)
        rr(Color(0xFFEFE6DA), 14f + c * 59f, 128f + r * 62f, 54f, 54f, 12f, 2f)
        rr(if ((r * 3 + c) % 2 == 0) Color(0xFFF4C2CB) else Color(0xFFCFE8D8), 28f + c * 59f, 138f + r * 62f, 26f, 26f, 8f)
    }
    rr(Color.White, 14f, 256f, 110f, 34f, 10f)
    rr(Color(0xFFCFE8D8), 130f, 256f, 56f, 34f, 10f)
}

private fun DrawScope.drawMeadowMiniTiles() {
    drawRect(Color(0xFFFFF8EC))
    val cols = listOf(0xFFF7C9CF, 0xFFCFE3B4, 0xFFCFE6F2, 0xFFF8E4A8, 0xFFF7C9CF, 0xFFCFE3B4)
    val k = u()
    for (r in 0 until 2) for (c in 0 until 3) {
        rr(Color(0xFFFFFDF7), 14f + c * 59f, 8f + r * 50f, 54f, 44f, 12f)
        rr(Color(0xFFEADFCB), 14f + c * 59f, 8f + r * 50f, 54f, 44f, 12f, 2f)
        drawCircle(Color(cols[r * 3 + c]), 12f * k, Offset((41f + c * 59f) * k, (27f + r * 50f) * k))
    }
    rr(Color(0x80E8665C), 14f, 112f, 110f, 30f, 10f)
    rr(Color(0xFFDCEEF7), 130f, 112f, 56f, 30f, 10f)
    drawStrawberry(158f * k, 126f * k, 0.55f * k, stroke = 1.2f * k)
}

private fun DrawScope.drawPaperMini() {
    drawRect(Color.White)
    val ink = Color(0xFF111111)
    rr(ink, 14f, 16f, 110f, 12f, 6f)
    rr(Color(0xFF555555), 14f, 34f, 70f, 7f, 3.5f)
    for (r in 0 until 2) for (c in 0 until 3) {
        if (r == 1 && c == 2) rr(ink, 14f + c * 59f, 56f + r * 62f, 54f, 54f, 12f)
        rr(ink, 14f + c * 59f, 56f + r * 62f, 54f, 54f, 12f, 2.5f)
    }
    rr(ink, 14f, 184f, 172f, 80f, 12f, 2.5f)
    for (i in 0 until 3) {
        rr(ink, 26f, 198f + i * 20f, 12f, 12f, 3f, 2f)
        rr(ink, 46f, 202f + i * 20f, 100f - i * 20f, 5f, 2.5f)
    }
}
