package app.cozy.launcher.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Note
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Templates
import app.cozy.launcher.data.toLocalDateTime
import app.cozy.launcher.ui.Card
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Mascot
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.Screen
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.Tag
import app.cozy.launcher.ui.home.Divider
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.meadow.MeadowBanner
import app.cozy.launcher.ui.meadow.PaperTag
import app.cozy.launcher.ui.meadow.SceneBunny
import app.cozy.launcher.ui.meadow.Washi
import app.cozy.launcher.ui.meadow.gingham
import app.cozy.launcher.ui.meadow.notebookLines
import app.cozy.launcher.ui.meadow.paperShadow
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(nav: Navigator) {
    val p = LocalPalette.current
    val notes by Store.notes.collectAsState()
    val settings by Store.settings.collectAsState()
    var search by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf<String?>(null) }
    var menuFor by remember { mutableStateOf<Note?>(null) }
    var addingFolder by remember { mutableStateOf(false) }

    val shown = notes
        .filter { folder == null || it.folder == folder }
        .filter {
            search.isBlank() || it.title.contains(search, ignoreCase = true) || it.previewText().contains(search, ignoreCase = true)
        }
        .sortedByDescending { it.updatedAt }
    val pinned = shown.filter { it.pinned }
    val others = shown.filterNot { it.pinned }

    Page {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
        ) {
          if (p.meadow) {
            MeadowBanner(250.dp, seed = 7, hy = 170.dp, dm = 30.dp, df = 58.dp, bunnies = listOf(SceneBunny(0.9f, 246f, 0.7f, true))) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RoundButton("back", "Back", { nav.back() })
                    PaperTag(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 8.dp)) { Txt("Notes", T.display(38)) }
                    Spacer(Modifier.weight(1f))
                    Pill("New note", { nav.go(Screen.Templates) }, style = PillStyle.BERRY, icon = "plus")
                }
            }
          }
          Column(
            Modifier.padding(horizontal = 40.dp, vertical = if (p.meadow) 22.dp else 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
          ) {
            if (!p.meadow) Header("Notes", { nav.back() }) {
                Pill("New note", { nav.go(Screen.Templates) }, style = PillStyle.DARK, icon = "plus")
            }
            CozyField(search, { search = it }, "Search notes", Modifier.fillMaxWidth(), leadingIcon = "search")

            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("All · ${notes.size}", folder == null, { folder = null })
                settings.folders.forEach { f ->
                    Chip(f, folder == f, { folder = if (folder == f) null else f })
                }
                Pill("+ Folder", { addingFolder = true }, style = PillStyle.DASHED, textSize = 16)
            }

            if (pinned.isNotEmpty() && p.meadow) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).gingham().padding(start = 26.dp, end = 26.dp, top = 22.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(p.card).padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Txt("Pinned", T.body(16, 800))
                    }
                    pinned.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                            pair.forEachIndexed { i, n ->
                                MeadowPinnedCard(n, Modifier.weight(1f), if (i == 0) -1.5f else 1.2f, if (i == 0) Color(0xFFF4A6B8) else Color(0xFFB9D38F),
                                    { nav.go(Screen.Editor(n.id)) }, { menuFor = n })
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else if (pinned.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CozyIcon("pin", size = 18.dp, tint = p.muted)
                    SectionLabel("Pinned")
                }
                pinned.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEach { n -> PinnedCard(n, Modifier.weight(1f), { nav.go(Screen.Editor(n.id)) }, { menuFor = n }) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (others.isNotEmpty() && p.meadow) {
                val nb = RoundedCornerShape(22.dp)
                Column(Modifier.fillMaxWidth().clip(nb).background(p.card).border(2.dp, p.border, nb).notebookLines()) {
                    Txt(if (folder != null) folder!!.lowercase() else "recent notes", T.hand(28), Modifier.height(52.dp).padding(start = 92.dp, top = 8.dp))
                    others.forEach { n -> MeadowNoteRow(n, { nav.go(Screen.Editor(n.id)) }, { menuFor = n }) }
                    Spacer(Modifier.height(12.dp))
                }
            } else if (others.isNotEmpty()) {
                SectionLabel(if (folder != null) folder!! else "Recent")
                Card(padding = androidx.compose.foundation.layout.PaddingValues(0.dp), spacing = 0.dp) {
                    others.forEachIndexed { i, n ->
                        NoteRow(n, { nav.go(Screen.Editor(n.id)) }, { menuFor = n })
                        if (i < others.lastIndex) Divider()
                    }
                }
            }

            if (shown.isEmpty()) {
                Txt(if (search.isNotBlank()) "No notes match \"$search\"." else "No notes here yet.", T.body(18), color = p.muted)
            }

            val shape = RoundedCornerShape(28.dp)
            var tipMod = Modifier.fillMaxWidth().clip(shape).background(p.accent)
            if (p.eink) tipMod = tipMod.border(p.line, p.ink, shape)
            Row(
                tipMod.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Mascot(size = 72.dp, bob = false)
                Txt("Tip: hold your pen still on a page for a moment and a text box pops up with the keyboard.", T.body(18, 600), Modifier.weight(1f))
            }
          }
        }
    }

    menuFor?.let { n -> NoteMenu(n, settings.folders) { menuFor = null } }

    if (addingFolder) {
        var name by remember { mutableStateOf("") }
        CozyDialog({ addingFolder = false }, "New folder") {
            CozyField(name, { name = it }, "Folder name", Modifier.fillMaxWidth())
            DialogButtons("Add", {
                val clean = name.trim()
                if (clean.isNotEmpty() && clean !in settings.folders) Store.updateSettings { it.copy(folders = it.folders + clean) }
                addingFolder = false
            }, { addingFolder = false })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MeadowPinnedCard(n: Note, modifier: Modifier, rot: Float, tape: Color, onOpen: () -> Unit, onMenu: () -> Unit) {
    val p = LocalPalette.current
    Box(modifier.rotate(rot)) {
        Column(
            Modifier.fillMaxWidth().paperShadow(6.dp, Color(0x244A3A2E), 6.dp).clip(RoundedCornerShape(6.dp)).background(p.card)
                .combinedClickable(onClick = onOpen, onLongClick = onMenu)
                .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Txt(n.title.ifBlank { "Untitled" }, T.display(22, 500), maxLines = 1)
            Txt(n.previewText(), T.body(16), color = p.muted, maxLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(Templates.info(n.template).name)
                val (done, total) = n.checkCounts()
                if (total > 0) Tag("$done of $total ticked")
            }
        }
        Washi(Modifier.align(Alignment.TopCenter).offset(y = (-12).dp), tape, 96.dp, -rot * 2f)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MeadowNoteRow(n: Note, onOpen: () -> Unit, onMenu: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().height(52.dp).combinedClickable(onClick = onOpen, onLongClick = onMenu).padding(start = 92.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Txt(n.title.ifBlank { "Untitled" }, T.body(19, 700), maxLines = 1)
        Txt("${relativeDay(n.updatedAt)} · ${n.previewText()}", T.body(16), Modifier.weight(1f), color = p.muted, maxLines = 1)
        Tag(Templates.info(n.template).name)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedCard(n: Note, modifier: Modifier, onOpen: () -> Unit, onMenu: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.clip(shape).background(p.card).border(p.line, p.border, shape)
            .combinedClickable(onClick = onOpen, onLongClick = onMenu)
    ) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(if (p.eink) p.ink else Color(n.color)))
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt(n.title.ifBlank { "Untitled" }, T.display(22, 500), maxLines = 1)
            Txt(n.previewText(), T.body(16), color = p.muted, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(Templates.info(n.template).name)
                val (done, total) = n.checkCounts()
                if (total > 0) Tag("$done of $total ticked")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(n: Note, onOpen: () -> Unit, onMenu: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onMenu).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt(n.title.ifBlank { "Untitled" }, T.body(19, 700), maxLines = 1)
            Txt("${relativeDay(n.updatedAt)} · ${n.previewText()}", T.body(16), color = p.muted, maxLines = 1)
        }
        Tag(Templates.info(n.template).name)
    }
}

fun relativeDay(millis: Long): String {
    val d = toLocalDateTime(millis).toLocalDate()
    val today = LocalDate.now()
    val diff = ChronoUnit.DAYS.between(d, today)
    return when {
        diff == 0L -> "Today"
        diff == 1L -> "Yesterday"
        diff in 2..6 -> d.format(DateTimeFormatter.ofPattern("EEEE"))
        d.year == today.year -> d.format(DateTimeFormatter.ofPattern("d MMM"))
        else -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteMenu(n: Note, folders: List<String>, close: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    CozyDialog(close, n.title.ifBlank { "Untitled" }) {
        if (!confirmDelete) {
            Pill(if (n.pinned) "Unpin" else "Pin to top", {
                Store.upsertNote(n.copy(pinned = !n.pinned))
                close()
            }, Modifier.fillMaxWidth(), style = PillStyle.LIGHT, icon = "pin")
            SectionLabel("Folder")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("None", n.folder == null, { Store.upsertNote(n.copy(folder = null)); close() })
                folders.forEach { f -> Chip(f, n.folder == f, { Store.upsertNote(n.copy(folder = f)); close() }) }
            }
            Pill("Delete note", { confirmDelete = true }, Modifier.fillMaxWidth(), style = PillStyle.SOFT, icon = "trash")
        } else {
            Txt("Delete this note? This can't be undone.", T.body(18))
            DialogButtons("Delete", {
                Store.deleteNote(n.id)
                close()
            }, { confirmDelete = false })
        }
    }
}
