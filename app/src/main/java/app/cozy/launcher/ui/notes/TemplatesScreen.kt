package app.cozy.launcher.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Templates
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

@Composable
fun TemplatesScreen(nav: Navigator) {
    val p = LocalPalette.current
    var selected by remember { mutableStateOf("dotted") }
    var category by remember { mutableStateOf("All") }
    var paperIndex by remember { mutableStateOf(0) }
    val paperColor = Templates.paperColors[paperIndex]
    val shown = Templates.all.filter { category == "All" || it.category == category }

    Page {
        Column(Modifier.padding(horizontal = 40.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Header("Choose a template", { nav.back() })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("All", "Paper", "Lists", "Planners", "Meadow").forEach { c -> Chip(c, category == c, { category = c }) }
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                shown.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEach { t ->
                            val on = t.id == selected
                            val shape = RoundedCornerShape(24.dp)
                            Column(
                                Modifier.weight(1f).clip(shape)
                                    .background(if (on) p.blush else p.card)
                                    .border(if (on) 3.dp else p.line, if (on) p.ink else p.border, shape)
                                    .clickable(onClickLabel = "Choose ${t.name}", role = Role.RadioButton) { selected = t.id }
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                TemplateThumb(
                                    t.id,
                                    Modifier.fillMaxWidth().height(190.dp),
                                    paper = Color(paperColor),
                                    ink = p.ink,
                                    pink = if (p.eink) p.soft else p.accent,
                                    mint = p.mint,
                                )
                                Column(Modifier.padding(horizontal = 6.dp)) {
                                    Txt(t.name, T.body(18, 700), maxLines = 1)
                                    Txt(t.desc, T.body(14), color = p.muted, maxLines = 1)
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Card(radius = 28.dp, padding = androidx.compose.foundation.layout.PaddingValues(start = 26.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Txt("Selected", T.body(15, 700), color = p.muted)
                        Txt(Templates.info(selected).name, T.display(24, 500))
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(24.dp)).border(p.line, p.border, RoundedCornerShape(24.dp))
                            .clickable(onClickLabel = "Change paper colour", role = Role.Button) { paperIndex = (paperIndex + 1) % Templates.paperColors.size }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(Color(paperColor)).border(1.5.dp, p.ink, RoundedCornerShape(11.dp)))
                        Txt("Paper colour", T.body(17, 700))
                    }
                    Pill("Use template", {
                        val note = Templates.create(selected, paperColor)
                        Store.upsertNote(note)
                        nav.replace(Screen.Editor(note.id))
                    }, style = PillStyle.DARK, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 26.dp, vertical = 16.dp))
                }
            }
        }
    }
}
