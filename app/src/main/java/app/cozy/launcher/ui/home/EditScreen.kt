package app.cozy.launcher.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Builtins
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Tile
import app.cozy.launcher.data.newId
import app.cozy.launcher.system.Apps
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.Icons
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.dashedBorder
import app.cozy.launcher.ui.theme.LocalAnimate
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(nav: Navigator, selectTile: String?) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val settings by Store.settings.collectAsState()
    var selected by remember { mutableStateOf(selectTile ?: settings.tiles.firstOrNull()?.id) }
    var search by remember { mutableStateOf("") }
    val apps = remember { Apps.list(ctx, refresh = true) }
    val tile = settings.tiles.firstOrNull { it.id == selected }

    val t = rememberInfiniteTransition(label = "wiggle")
    val wig by t.animateFloat(-1.2f, 1.2f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "wig")
    val wiggle = if (LocalAnimate.current) wig else 0f

    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 40.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Txt("Edit home", T.display(40))
                    Txt("Tap a tile, then choose what it opens", T.body(17), color = p.muted)
                }
                RoundButton("settings", "Settings", { nav.go(Screen.Settings) })
                Pill("+ Add tile", {
                    val newTile = Tile(newId(), "New tile", "heart")
                    Store.updateSettings { it.copy(tiles = it.tiles + newTile) }
                    selected = newTile.id
                }, style = PillStyle.DASHED)
                Pill("Done", { nav.back() }, style = PillStyle.DARK)
            }

            // The tiles, wiggling while in edit mode
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                settings.tiles.chunked(3).forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        chunk.forEachIndexed { i, tl ->
                            val isSel = tl.id == selected
                            val shape = RoundedCornerShape(24.dp)
                            var m = Modifier.weight(1f).rotate(if (i % 2 == 0) wiggle else -wiggle).clip(shape)
                                .background(if (isSel) p.blush else p.card)
                            m = if (isSel) m.border(3.dp, p.ink, shape) else m.dashedBorder(p.dashed, 24.dp)
                            Column(
                                m.clickable(onClickLabel = "Edit ${tl.label}", role = Role.Button) { selected = tl.id }
                                    .padding(vertical = 18.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CozyIcon(tl.icon, size = 30.dp)
                                Txt(tl.label, T.display(20, 500), maxLines = 1)
                                Txt(opensLabel(ctx, tl), T.body(14), color = p.muted, maxLines = 1, align = TextAlign.Center)
                            }
                        }
                        repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            if (tile != null) {
                Card(radius = 32.dp, spacing = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Txt("${tile.label} tile", T.display(28, 500), Modifier.weight(1f), maxLines = 1)
                        val index = settings.tiles.indexOfFirst { it.id == tile.id }
                        RoundButton("back", "Move earlier", { move(tile.id, -1) }, size = 44.dp)
                        RoundButton("forward", "Move later", { move(tile.id, 1) }, size = 44.dp)
                        if (settings.tiles.size > 1) RoundButton("trash", "Remove tile", {
                            Store.updateSettings { s -> s.copy(tiles = s.tiles.filterNot { it.id == tile.id }) }
                            selected = settings.tiles.getOrNull(if (index > 0) index - 1 else 1)?.id
                        }, size = 44.dp)
                    }

                    SectionLabel("Label")
                    CozyField(tile.label, { v -> Store.updateTile(tile.id) { it.copy(label = v) } }, "Tile name")

                    SectionLabel("Icon")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icons.pickable.forEach { key ->
                            val on = tile.icon == key
                            val shape = RoundedCornerShape(18.dp)
                            Box(
                                Modifier.size(56.dp).clip(shape).background(if (on) p.ink else p.soft)
                                    .clickable(onClickLabel = "$key icon", role = Role.Button) { Store.updateTile(tile.id) { it.copy(icon = key) } },
                                contentAlignment = Alignment.Center,
                            ) { CozyIcon(key, size = 26.dp, tint = if (on) p.onInk else p.ink) }
                        }
                    }

                    SectionLabel("Opens")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Builtins.all.forEach { b ->
                            OpensRow(
                                title = "${Builtins.label(b)} (built-in)",
                                selected = tile.pkg == null && tile.builtin == b,
                                leading = { CozyIcon(Builtins.icon(b), size = 24.dp) },
                            ) { Store.updateTile(tile.id) { it.copy(builtin = b, pkg = null) } }
                        }
                        CozyField(search, { search = it }, "Search installed apps", Modifier.fillMaxWidth().padding(vertical = 6.dp), leadingIcon = "search")
                        val shown = apps.filter { search.isBlank() || it.label.contains(search, ignoreCase = true) }
                        shown.forEach { app ->
                            OpensRow(
                                title = app.label,
                                selected = tile.pkg == app.pkg,
                                leading = { AppIcon(app.pkg, 32) },
                            ) { Store.updateTile(tile.id) { it.copy(pkg = app.pkg, builtin = null) } }
                        }
                        if (shown.isEmpty()) Txt("No apps match \"$search\"", T.body(17), color = p.muted)
                    }
                }
            }
        }
    }
}

private fun opensLabel(ctx: android.content.Context, t: Tile): String = when {
    t.pkg != null -> "opens " + Apps.label(ctx, t.pkg)
    t.builtin != null -> "built-in app"
    else -> "tap to choose"
}

private fun move(id: String, by: Int) = Store.updateSettings { s ->
    val list = s.tiles.toMutableList()
    val i = list.indexOfFirst { it.id == id }
    val j = (i + by).coerceIn(0, list.lastIndex)
    if (i < 0 || i == j) return@updateSettings s
    val t = list.removeAt(i)
    list.add(j, t)
    s.copy(tiles = list)
}

@Composable
private fun OpensRow(title: String, selected: Boolean, leading: @Composable () -> Unit, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
            .background(if (selected) p.blush else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(p.soft), contentAlignment = Alignment.Center) { leading() }
        Txt(title, T.body(18, 600), Modifier.weight(1f), maxLines = 1)
        if (selected) CozyIcon("check", size = 22.dp, weight = 2.6f)
    }
}
