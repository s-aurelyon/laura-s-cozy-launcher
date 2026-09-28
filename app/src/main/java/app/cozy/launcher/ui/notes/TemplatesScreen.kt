package app.cozy.launcher.ui.notes

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
import app.cozy.launcher.ui.compact
import app.cozy.launcher.ui.gutter
import app.cozy.launcher.ui.gutterTop
import app.cozy.launcher.ui.theme.LocalCompact
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplatesScreen(nav: Navigator) {
    val p = LocalPalette.current
    var selected by remember { mutableStateOf("dotted") }
    var category by remember { mutableStateOf("All") }
    var paperIndex by remember { mutableStateOf(0) }
    val paperColor = Templates.paperColors[paperIndex]
    val shown = Templates.all.filter { category == "All" || it.category == category }
    val phone = LocalCompact.current
    val perRow = if (phone) 2 else 3

    Page {
        Column(Modifier.padding(horizontal = gutter, vertical = gutterTop), verticalArrangement = Arrangement.spacedBy(compact(22.dp, 14.dp))) {
            Header(if (phone) "Templates" else "Choose a template", { nav.back() })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(compact(10.dp, 8.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Paper", "Lists", "Planners", "Meadow").forEach { c -> Chip(c, category == c, { category = c }) }
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(compact(18.dp, 12.dp)),
            ) {
                shown.chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(compact(18.dp, 12.dp))) {
                        row.forEach { t ->
                            val on = t.id == selected
                            val shape = RoundedCornerShape(24.dp)
                            Column(
                                Modifier.weight(1f).clip(shape)
                                    .background(if (on) p.blush else p.card)
                                    .border(if (on) 3.dp else p.line, if (on) p.ink else p.border, shape)
                                    .clickable(onClickLabel = "Choose ${t.name}", role = Role.RadioButton) { selected = t.id }
                                    .padding(compact(12.dp, 8.dp)),
                                verticalArrangement = Arrangement.spacedBy(compact(10.dp, 6.dp)),
                            ) {
                                TemplateThumb(
                                    t.id,
                                    Modifier.fillMaxWidth().height(compact(190.dp, 150.dp)),
                                    paper = Color(paperColor),
                                    ink = p.ink,
                                    pink = if (p.eink) p.soft else p.accent,
                                    mint = p.mint,
                                )
                                Column(Modifier.padding(horizontal = compact(6.dp, 4.dp))) {
                                    Txt(t.name, T.body(compact(18, 15), 700), maxLines = 1)
                                    Txt(t.desc, T.body(compact(14, 12)), color = p.muted, maxLines = 1)
                                }
                            }
                        }
                        repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            val paperButton: @Composable () -> Unit = {
                Row(
                    Modifier.clip(RoundedCornerShape(24.dp)).border(p.line, p.border, RoundedCornerShape(24.dp))
                        .clickable(onClickLabel = "Change paper colour", role = Role.Button) { paperIndex = (paperIndex + 1) % Templates.paperColors.size }
                        .padding(horizontal = compact(16.dp, 12.dp), vertical = compact(12.dp, 10.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(Color(paperColor)).border(1.5.dp, p.ink, RoundedCornerShape(11.dp)))
                    Txt(if (phone) "Paper" else "Paper colour", T.body(compact(17, 15), 700))
                }
            }
            val useTemplate = {
                val note = Templates.create(selected, paperColor)
                Store.upsertNote(note)
                nav.replace(Screen.Editor(note.id))
            }
            if (phone) {
                Card(radius = 24.dp, padding = PaddingValues(16.dp), spacing = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Txt("Selected", T.body(13, 700), color = p.muted)
                            Txt(Templates.info(selected).name, T.display(20, 500), maxLines = 1)
                        }
                        paperButton()
                    }
                    Pill("Use template", useTemplate, Modifier.fillMaxWidth(), style = PillStyle.DARK, padding = PaddingValues(14.dp))
                }
            } else {
                Card(radius = 28.dp, padding = PaddingValues(start = 26.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f)) {
                            Txt("Selected", T.body(15, 700), color = p.muted)
                            Txt(Templates.info(selected).name, T.display(24, 500))
                        }
                        paperButton()
                        Pill("Use template", useTemplate, style = PillStyle.DARK, padding = PaddingValues(horizontal = 26.dp, vertical = 16.dp))
                    }
                }
            }
        }
    }
}
