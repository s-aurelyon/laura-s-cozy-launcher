package app.cozy.launcher.ui.notes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Block
import app.cozy.launcher.data.BulletBlock
import app.cozy.launcher.data.CheckBlock
import app.cozy.launcher.data.Note
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Stroke
import app.cozy.launcher.data.TableBlock
import app.cozy.launcher.data.Templates
import app.cozy.launcher.data.TextBlock
import app.cozy.launcher.data.TextBox
import app.cozy.launcher.data.TextKind
import app.cozy.launcher.data.toLocalDateTime
import app.cozy.launcher.system.Apps
import app.cozy.launcher.ui.CheckMark
import app.cozy.launcher.ui.Chip
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.DialogButtons
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.RoundButton
import app.cozy.launcher.ui.SectionLabel
import app.cozy.launcher.ui.dashedBorder
import app.cozy.launcher.ui.reminders.AddReminderDialog
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke

private enum class Tool { TYPE, PEN, HIGHLIGHT, ERASER }

private val penColors = listOf(0xFF3A2E2A, 0xFFE0708A, 0xFF5B7DB1, 0xFF5E9C76)
private const val HIGHLIGHT_COLOR = 0xFFF6D860

private fun textStyleFor(kind: TextKind): TextStyle = when (kind) {
    TextKind.TITLE -> T.display(34)
    TextKind.HEADING -> T.display(26, 500)
    TextKind.SUBHEADING -> T.body(20, 800)
    TextKind.BODY -> T.body(19)
    TextKind.MONO -> T.mono(17)
}

private fun textOf(b: Block): String = when (b) {
    is TextBlock -> b.text
    is CheckBlock -> b.text
    is BulletBlock -> b.text
    is TableBlock -> ""
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditorScreen(nav: Navigator, noteId: String) {
    val initial = remember(noteId) { Store.note(noteId) }
    if (initial == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val density = LocalDensity.current.density
    val keyboard = LocalSoftwareKeyboardController.current
    val settings by Store.settings.collectAsState()
    val holdMs = settings.penHoldMs

    var meta by remember { mutableStateOf(initial) }
    var title by remember { mutableStateOf(initial.title) }
    val blocks = remember { mutableStateListOf<Block>().apply { addAll(initial.blocks.ifEmpty { listOf(TextBlock()) }) } }
    val strokes = remember { mutableStateListOf<Stroke>().apply { addAll(initial.strokes) } }
    val boxes = remember { mutableStateListOf<TextBox>().apply { addAll(initial.boxes) } }
    val undo = remember { mutableStateListOf<String>() }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val boxBounds = remember { mutableMapOf<String, Rect>() }
    val pathCache = remember { IdentityHashMap<Stroke, Path>() }

    var tool by remember { mutableStateOf(Tool.TYPE) }
    var penColor by remember { mutableStateOf(penColors[0]) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    var pendingFocus by remember { mutableStateOf<String?>(null) }
    var focusBox by remember { mutableStateOf<String?>(null) }
    var livePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var holdAt by remember { mutableStateOf<Offset?>(null) }
    val holdProgress = remember { Animatable(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showRemind by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    fun req(id: String) = requesters.getOrPut(id) { FocusRequester() }

    fun save() {
        val n = meta.copy(
            title = title,
            blocks = blocks.toList(),
            strokes = strokes.toList(),
            boxes = boxes.toList(),
            updatedAt = System.currentTimeMillis(),
        )
        meta = n
        Store.upsertNote(n)
    }

    fun focusIndex(): Int = blocks.indexOfFirst { it.id == focusedId }.let { if (it < 0) blocks.lastIndex else it }

    fun insertAfter(i: Int, b: Block) {
        blocks.add((i + 1).coerceAtMost(blocks.size), b)
        pendingFocus = b.id
        save()
    }

    fun applyStyle(kind: TextKind) {
        val i = focusIndex()
        val b = blocks.getOrNull(i) ?: return
        if (b is TableBlock) return
        blocks[i] = TextBlock(b.id, textOf(b), kind)
        pendingFocus = b.id
        save()
    }

    fun toggleList(check: Boolean) {
        val i = focusIndex()
        val b = blocks.getOrNull(i)
        if (b == null || b is TableBlock) {
            insertAfter(i, if (check) CheckBlock() else BulletBlock())
            return
        }
        blocks[i] = when {
            check && b is CheckBlock -> TextBlock(b.id, b.text)
            !check && b is BulletBlock -> TextBlock(b.id, b.text)
            check -> CheckBlock(b.id, textOf(b))
            else -> BulletBlock(b.id, textOf(b))
        }
        pendingFocus = b.id
        save()
    }

    fun addTable() {
        val i = focusIndex()
        val t = TableBlock(rows = listOf(listOf("", "", ""), listOf("", "", ""), listOf("", "", "")))
        blocks.add((i + 1).coerceAtMost(blocks.size), t)
        val after = TextBlock()
        blocks.add((i + 2).coerceAtMost(blocks.size), after)
        save()
    }

    fun addBox(xDp: Float, yDp: Float) {
        val box = TextBox(x = xDp.coerceAtLeast(8f), y = yDp.coerceAtLeast(8f))
        boxes.add(box)
        undo.add("box:" + box.id)
        focusBox = box.id
        save()
    }

    fun eraseAt(pos: Offset) {
        val r = 16f * density
        val hit = strokes.filter { s ->
            var any = false
            var k = 0
            while (k + 1 < s.pts.size && !any) {
                val dx = s.pts[k] * density - pos.x
                val dy = s.pts[k + 1] * density - pos.y
                if (dx * dx + dy * dy < r * r) any = true
                k += 2
            }
            any
        }
        if (hit.isNotEmpty()) {
            strokes.removeAll(hit)
            save()
        }
    }

    fun doUndo() {
        val last = undo.removeLastOrNull() ?: return
        if (last == "stroke") strokes.removeLastOrNull()
        else if (last.startsWith("box:")) {
            val id = last.removePrefix("box:")
            boxes.removeAll { it.id == id }
        }
        save()
    }

    // Remove a note that was opened and left completely empty.
    DisposableEffect(noteId) {
        onDispose {
            val n = Store.note(noteId)
            if (n != null && n.isEmpty()) Store.deleteNote(noteId)
        }
    }

    LaunchedEffect(pendingFocus) {
        val id = pendingFocus ?: return@LaunchedEffect
        delay(60)
        try {
            requesters[id]?.requestFocus()
            keyboard?.show()
        } catch (_: IllegalStateException) {
        }
        pendingFocus = null
    }

    // The little ring that fills while the pen is held still.
    LaunchedEffect(holdAt) {
        if (holdAt != null) {
            holdProgress.snapTo(0f)
            holdProgress.animateTo(1f, tween(holdMs.toInt(), easing = LinearEasing))
        } else {
            holdProgress.snapTo(0f)
        }
    }

    val maxInkY = max(
        strokes.maxOfOrNull { s -> s.pts.filterIndexed { i, _ -> i % 2 == 1 }.maxOrNull() ?: 0f } ?: 0f,
        boxes.maxOfOrNull { it.y } ?: 0f,
    )
    val pageMinHeight = max(1500f, maxInkY + 600f).dp
    val startPad = if (meta.paper == "cornell") 116.dp else 36.dp
    val focusedKind = (blocks.firstOrNull { it.id == focusedId } as? TextBlock)?.kind

    Page {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Notes", { nav.back() }, style = PillStyle.LIGHT, icon = "back")
                Spacer(Modifier.weight(1f))
                RoundButton("pin", if (meta.pinned) "Unpin" else "Pin", {
                    meta = meta.copy(pinned = !meta.pinned)
                    save()
                }, filled = meta.pinned)
                RoundButton("bell", "Remind me about this note", { showRemind = true })
                RoundButton("share", "Share", {
                    Apps.share(ctx, title.ifBlank { "Note" }, exportText(title, blocks, boxes))
                })
                RoundButton("more", "More options", { showMenu = true })
            }

            // The page
            val pageShape = RoundedCornerShape(28.dp)
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(pageShape)
                    .background(if (p.eink) Color.White else Color(meta.paperColor))
                    .border(p.line, p.border, pageShape)
            ) {
                Box(Modifier.fillMaxSize().verticalScroll(scroll, enabled = tool == Tool.TYPE)) {
                    Box(
                        Modifier.fillMaxWidth()
                            .heightIn(min = pageMinHeight)
                            .drawBehind { drawPaper(meta.paper) }
                            .pointerInput(tool, penColor, holdMs) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    val stylus = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                                    // Fingers type and scroll, unless a drawing tool is picked.
                                    if (!stylus && tool == Tool.TYPE) return@awaitEachGesture
                                    down.consume()

                                    val erasing = down.type == PointerType.Eraser || tool == Tool.ERASER
                                    val highlight = tool == Tool.HIGHLIGHT
                                    val start = System.currentTimeMillis()
                                    val pts = mutableListOf(down.position)
                                    var moved = false
                                    var held = false
                                    val slop = viewConfiguration.touchSlop
                                    if (erasing) eraseAt(down.position) else holdAt = down.position

                                    while (true) {
                                        val canHold = !moved && !erasing
                                        val event: PointerEvent? = if (canHold) {
                                            val left = holdMs - (System.currentTimeMillis() - start)
                                            if (left <= 0) null else withTimeoutOrNull(left) { awaitPointerEvent(PointerEventPass.Initial) }
                                        } else {
                                            awaitPointerEvent(PointerEventPass.Initial)
                                        }
                                        if (event == null) {
                                            held = true
                                            break
                                        }
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        event.changes.forEach { it.consume() }
                                        if (change == null || !change.pressed) break
                                        val pos = change.position
                                        if (!moved && (pos - down.position).getDistance() > slop) {
                                            moved = true
                                            holdAt = null
                                        }
                                        if (erasing) {
                                            eraseAt(pos)
                                        } else if (moved) {
                                            pts.add(pos)
                                            livePoints = pts.toList()
                                        }
                                    }
                                    holdAt = null

                                    if (held) {
                                        // Held still: place a text box here and open the keyboard.
                                        addBox(down.position.x / density - 10f, down.position.y / density - 22f)
                                        while (true) {
                                            val e = awaitPointerEvent(PointerEventPass.Initial)
                                            e.changes.forEach { it.consume() }
                                            if (e.changes.none { it.pressed }) break
                                        }
                                    } else if (!erasing) {
                                        val quick = System.currentTimeMillis() - start < 250
                                        val tapped = if (!moved && quick) boxBounds.entries.firstOrNull { it.value.contains(down.position) }?.key else null
                                        if (tapped != null) {
                                            focusBox = tapped
                                        } else {
                                            if (pts.size == 1) pts.add(pts[0] + Offset(0.6f, 0.6f))
                                            strokes.add(
                                                Stroke(
                                                    color = if (highlight) HIGHLIGHT_COLOR else penColor,
                                                    width = if (highlight) 18f else 3f,
                                                    highlighter = highlight,
                                                    pts = pts.flatMap { listOf(it.x / density, it.y / density) },
                                                )
                                            )
                                            undo.add("stroke")
                                            save()
                                        }
                                    }
                                    livePoints = emptyList()
                                }
                            }
                    ) {
                        // Typed content
                        Column(
                            Modifier.fillMaxWidth().padding(start = startPad, end = 36.dp, top = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BasicTextField(
                                value = title,
                                onValueChange = { title = it; save() },
                                textStyle = T.display(36).copy(color = p.ink),
                                cursorBrush = SolidColor(p.ink),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                decorationBox = { inner ->
                                    Box {
                                        if (title.isEmpty()) Txt("Title", T.display(36), color = p.dashed)
                                        inner()
                                    }
                                },
                            )
                            val edited = toLocalDateTime(meta.updatedAt).format(DateTimeFormatter.ofPattern("HH:mm"))
                            Txt(
                                listOfNotNull("Edited $edited", Templates.paperNames[meta.paper], meta.folder).joinToString(" · "),
                                T.body(15, 600), color = p.muted,
                            )
                            Spacer(Modifier.height(4.dp))

                            blocks.forEachIndexed { i, b ->
                              key(b.id) {
                                val onFocus = { focusedId = b.id }
                                when (b) {
                                    is TextBlock -> TextBlockView(
                                        b, req(b.id),
                                        placeholder = if (blocks.size == 1 && b.text.isEmpty()) "Start typing, or write with your pen…" else null,
                                        onChange = { v -> blocks[i] = b.copy(text = v); save() },
                                        onFocus = onFocus,
                                    )
                                    is CheckBlock -> ListItemView(
                                        text = b.text,
                                        fr = req(b.id),
                                        leading = { CheckMark(b.done, { blocks[i] = b.copy(done = !b.done); save() }, size = 26.dp, square = true) },
                                        done = b.done,
                                        onChange = { v -> blocks[i] = b.copy(text = v); save() },
                                        onEnter = {
                                            if (b.text.isBlank()) { blocks[i] = TextBlock(b.id); pendingFocus = b.id; save() }
                                            else insertAfter(i, CheckBlock())
                                        },
                                        onFocus = onFocus,
                                    )
                                    is BulletBlock -> ListItemView(
                                        text = b.text,
                                        fr = req(b.id),
                                        leading = {
                                            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(p.ink))
                                            }
                                        },
                                        done = false,
                                        onChange = { v -> blocks[i] = b.copy(text = v); save() },
                                        onEnter = {
                                            if (b.text.isBlank()) { blocks[i] = TextBlock(b.id); pendingFocus = b.id; save() }
                                            else insertAfter(i, BulletBlock())
                                        },
                                        onFocus = onFocus,
                                    )
                                    is TableBlock -> TableView(
                                        b,
                                        onChange = { rows -> blocks[i] = b.copy(rows = rows); save() },
                                        onDelete = { blocks.removeAt(i); if (blocks.isEmpty()) blocks.add(TextBlock()); save() },
                                        onFocus = onFocus,
                                    )
                                }
                              }
                            }

                            // Tapping the empty space below carries on typing.
                            Box(
                                Modifier.fillMaxWidth().height(200.dp).clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    val last = blocks.lastOrNull()
                                    if (last is TextBlock && last.text.isEmpty()) pendingFocus = last.id
                                    else insertAfter(blocks.lastIndex, TextBlock())
                                }
                            )
                        }

                        // Ink
                        Canvas(Modifier.matchParentSize()) {
                            fun pathOf(s: Stroke): Path = pathCache.getOrPut(s) { buildPath(s.pts.chunked(2).map { Offset(it[0] * density, it[1] * density) }) }
                            strokes.filter { it.highlighter }.forEach { s ->
                                drawPath(pathOf(s), Color(s.color).copy(alpha = 0.45f), style = DrawStroke(s.width * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                            strokes.filter { !it.highlighter }.forEach { s ->
                                drawPath(pathOf(s), Color(s.color), style = DrawStroke(s.width * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                            if (livePoints.size > 1) {
                                val hl = tool == Tool.HIGHLIGHT
                                drawPath(
                                    buildPath(livePoints),
                                    if (hl) Color(HIGHLIGHT_COLOR).copy(alpha = 0.45f) else Color(penColor),
                                    style = DrawStroke((if (hl) 18f else 3f) * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                            val h = holdAt
                            if (h != null && holdProgress.value > 0.15f) {
                                val r = 24f * density
                                drawArc(
                                    color = p.ink,
                                    startAngle = -90f,
                                    sweepAngle = 360f * holdProgress.value,
                                    useCenter = false,
                                    topLeft = Offset(h.x - r, h.y - r),
                                    size = Size(r * 2, r * 2),
                                    style = DrawStroke(4f * density, cap = StrokeCap.Round),
                                )
                            }
                        }

                        // Floating text boxes
                        boxes.forEachIndexed { i, box ->
                          key(box.id) {
                            TextBoxView(
                                box = box,
                                density = density,
                                focusNow = focusBox == box.id,
                                onFocusHandled = { focusBox = null },
                                onChange = { v -> boxes[i] = box.copy(text = v); save() },
                                onMove = { dx, dy -> boxes[i] = box.copy(x = (box.x + dx).coerceAtLeast(0f), y = (box.y + dy).coerceAtLeast(0f)); save() },
                                onDelete = { boxes.removeAll { it.id == box.id }; boxBounds.remove(box.id); save() },
                                onBounds = { boxBounds[box.id] = it },
                            )
                          }
                        }
                    }
                }
            }

            // Text styles (typing) or pen colours (drawing)
            when (tool) {
                Tool.TYPE -> Row(
                    Modifier.clip(RoundedCornerShape(22.dp)).background(p.card).border(p.line, p.border, RoundedCornerShape(22.dp)).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(TextKind.TITLE to "Title", TextKind.HEADING to "Heading", TextKind.SUBHEADING to "Subheading", TextKind.BODY to "Body", TextKind.MONO to "Mono")
                        .forEach { (kind, label) ->
                            val on = focusedKind == kind
                            Box(
                                Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) p.ink else Color.Transparent)
                                    .clickable(onClickLabel = label, role = Role.Button) { applyStyle(kind) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                val style = when (kind) {
                                    TextKind.TITLE -> T.display(20)
                                    TextKind.HEADING -> T.display(18, 500)
                                    TextKind.SUBHEADING -> T.body(17, 800)
                                    TextKind.MONO -> T.mono(16)
                                    TextKind.BODY -> T.body(17)
                                }
                                Txt(label, style, color = if (on) p.onInk else p.ink)
                            }
                        }
                }
                Tool.PEN, Tool.HIGHLIGHT -> Row(
                    Modifier.clip(RoundedCornerShape(22.dp)).background(p.card).border(p.line, p.border, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (tool == Tool.PEN) {
                        penColors.forEach { c ->
                            val on = penColor == c
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color(c))
                                    .border(if (on) 3.dp else 0.dp, if (on) p.accent else Color.Transparent, RoundedCornerShape(17.dp))
                                    .clickable(onClickLabel = "Pen colour", role = Role.RadioButton) { penColor = c }
                            )
                        }
                        Txt("Your finger draws too while the pen tool is on.", T.body(15), color = p.muted)
                    } else {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(Color(HIGHLIGHT_COLOR)))
                        Txt("Highlighter", T.body(16, 700))
                    }
                }
                Tool.ERASER -> Txt("Rub over any ink to erase it. The eraser end of your pen works too.", T.body(16), color = p.muted)
            }

            // Toolbar
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(p.card)
                    .border(p.line, p.border, RoundedCornerShape(26.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolButton("keyboard", "Type", tool == Tool.TYPE) { tool = Tool.TYPE }
                ToolButton("pen", "Pen", tool == Tool.PEN) { tool = Tool.PEN }
                ToolButton("highlighter", "Highlighter", tool == Tool.HIGHLIGHT) { tool = Tool.HIGHLIGHT }
                ToolButton("eraser", "Eraser", tool == Tool.ERASER) { tool = Tool.ERASER }
                Box(Modifier.width(1.5.dp).height(32.dp).background(p.border))
                ToolButton("checklist", "Checklist", false) { tool = Tool.TYPE; toggleList(true) }
                ToolButton("bullets", "Bulleted list", false) { tool = Tool.TYPE; toggleList(false) }
                ToolButton("table", "Insert table", false) { tool = Tool.TYPE; addTable() }
                ToolButton("text", "Add a text box", false) { addBox(48f, scroll.value / density + 140f) }
                ToolButton("undo", "Undo ink", false) { doUndo() }
            }
        }
    }

    if (showRemind) {
        AddReminderDialog(
            onDismiss = { showRemind = false },
            initialTitle = title.ifBlank { "Look at my note" },
            initialDate = LocalDate.now(),
            noteId = noteId,
        )
    }

    if (showMenu) {
        var confirmDelete by remember { mutableStateOf(false) }
        CozyDialog({ showMenu = false }, "Note options") {
            if (!confirmDelete) {
                SectionLabel("Paper")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("blank" to "Blank", "lined" to "Lined", "dotted" to "Dotted", "grid" to "Grid", "cornell" to "Cornell").forEach { (k, label) ->
                        Chip(label, meta.paper == k, { meta = meta.copy(paper = k); save() })
                    }
                }
                SectionLabel("Paper colour")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Templates.paperColors.forEach { c ->
                        val on = meta.paperColor == c
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(Color(c))
                                .border(if (on) 3.dp else 1.5.dp, if (on) p.ink else p.border, RoundedCornerShape(20.dp))
                                .clickable(onClickLabel = "Paper colour", role = Role.RadioButton) { meta = meta.copy(paperColor = c); save() }
                        )
                    }
                }
                SectionLabel("Folder")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("None", meta.folder == null, { meta = meta.copy(folder = null); save() })
                    settings.folders.forEach { f -> Chip(f, meta.folder == f, { meta = meta.copy(folder = f); save() }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Delete note", { confirmDelete = true }, style = PillStyle.SOFT, icon = "trash")
                    Spacer(Modifier.weight(1f))
                    Pill("Done", { showMenu = false }, style = PillStyle.DARK)
                }
            } else {
                Txt("Delete this note? This can't be undone.", T.body(18))
                DialogButtons("Delete", {
                    showMenu = false
                    Store.deleteNote(noteId)
                    nav.back()
                }, { confirmDelete = false })
            }
        }
    }
}

private fun buildPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) {
        path.lineTo(points[0].x + 0.5f, points[0].y + 0.5f)
        return path
    }
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        path.quadraticBezierTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

private fun exportText(title: String, blocks: List<Block>, boxes: List<TextBox>): String {
    val sb = StringBuilder()
    if (title.isNotBlank()) sb.appendLine(title).appendLine()
    blocks.forEach { b ->
        when (b) {
            is TextBlock -> if (b.text.isNotBlank()) sb.appendLine(b.text)
            is CheckBlock -> sb.appendLine((if (b.done) "☑ " else "☐ ") + b.text)
            is BulletBlock -> sb.appendLine("• " + b.text)
            is TableBlock -> b.rows.forEach { r -> sb.appendLine(r.joinToString(" | ")) }
        }
    }
    boxes.filter { it.text.isNotBlank() }.forEach { sb.appendLine(it.text) }
    return sb.toString().trim()
}

@Composable
private fun ToolButton(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) p.blush else Color.Transparent)
            .then(if (selected) Modifier.border(2.dp, p.ink, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { CozyIcon(icon, size = 24.dp) }
}

@Composable
private fun TextBlockView(b: TextBlock, fr: FocusRequester, placeholder: String?, onChange: (String) -> Unit, onFocus: () -> Unit) {
    val p = LocalPalette.current
    val style = textStyleFor(b.kind)
    BasicTextField(
        value = b.text,
        onValueChange = onChange,
        textStyle = style.copy(color = p.ink),
        cursorBrush = SolidColor(p.ink),
        modifier = Modifier.fillMaxWidth().focusRequester(fr).onFocusChanged { if (it.isFocused) onFocus() }
            .padding(top = if (b.kind == TextKind.HEADING || b.kind == TextKind.TITLE) 8.dp else 0.dp),
        decorationBox = { inner ->
            Box {
                if (b.text.isEmpty() && placeholder != null) Txt(placeholder, style, color = p.muted)
                inner()
            }
        },
    )
}

@Composable
private fun ListItemView(
    text: String,
    fr: FocusRequester,
    leading: @Composable () -> Unit,
    done: Boolean,
    onChange: (String) -> Unit,
    onEnter: () -> Unit,
    onFocus: () -> Unit,
) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        leading()
        BasicTextField(
            value = text,
            onValueChange = { v ->
                // A typed or pasted new line starts the next item.
                if (v.contains('\n')) onEnter() else onChange(v)
            },
            singleLine = true,
            textStyle = T.body(19).copy(
                color = if (done) p.muted else p.ink,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(p.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onEnter() }),
            modifier = Modifier.weight(1f).focusRequester(fr).onFocusChanged { if (it.isFocused) onFocus() },
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Txt("List item", T.body(19), color = p.dashed)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun TableView(b: TableBlock, onChange: (List<List<String>>) -> Unit, onDelete: () -> Unit, onFocus: () -> Unit) {
    val p = LocalPalette.current
    val cols = b.rows.firstOrNull()?.size ?: 1
    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cellW = max(maxWidth.value / cols, 150f).dp
            val shape = RoundedCornerShape(14.dp)
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Column(Modifier.clip(shape).background(p.card).border(2.dp, p.dashed, shape)) {
                    b.rows.forEachIndexed { r, row ->
                        Row {
                            row.forEachIndexed { c, cell ->
                                BasicTextField(
                                    value = cell,
                                    onValueChange = { v ->
                                        onChange(b.rows.mapIndexed { ri, rr -> if (ri == r) rr.mapIndexed { ci, cc -> if (ci == c) v else cc } else rr })
                                    },
                                    textStyle = T.body(17, if (r == 0) 700 else 400).copy(color = p.ink),
                                    cursorBrush = SolidColor(p.ink),
                                    modifier = Modifier.width(cellW)
                                        .background(if (r == 0) p.mint else Color.Transparent)
                                        .border(0.75.dp, p.dashed)
                                        .onFocusChanged { if (it.isFocused) onFocus() }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val small = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
            Pill("+ Row", { onChange(b.rows + listOf(List(cols) { "" })) }, style = PillStyle.SOFT, textSize = 14, padding = small)
            Pill("+ Column", { onChange(b.rows.map { it + "" }) }, style = PillStyle.SOFT, textSize = 14, padding = small)
            if (b.rows.size > 1) Pill("− Row", { onChange(b.rows.dropLast(1)) }, style = PillStyle.SOFT, textSize = 14, padding = small)
            if (cols > 1) Pill("− Column", { onChange(b.rows.map { it.dropLast(1) }) }, style = PillStyle.SOFT, textSize = 14, padding = small)
            Pill("Remove table", onDelete, style = PillStyle.SOFT, textSize = 14, padding = small)
        }
    }
}

@Composable
private fun TextBoxView(
    box: TextBox,
    density: Float,
    focusNow: Boolean,
    onFocusHandled: () -> Unit,
    onChange: (String) -> Unit,
    onMove: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    onBounds: (Rect) -> Unit,
) {
    val p = LocalPalette.current
    val keyboard = LocalSoftwareKeyboardController.current
    val fr = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(focusNow) {
        if (focusNow) {
            delay(40)
            try {
                fr.requestFocus()
                keyboard?.show()
            } catch (_: IllegalStateException) {
            }
            onFocusHandled()
        }
    }

    Box(
        Modifier
            .offset { IntOffset((box.x * density).roundToInt(), (box.y * density).roundToInt()) }
            .width(box.width.dp)
            .onGloballyPositioned { onBounds(it.boundsInParent()) }
    ) {
        BasicTextField(
            value = box.text,
            onValueChange = onChange,
            textStyle = T.body(19).copy(color = p.ink),
            cursorBrush = SolidColor(p.ink),
            modifier = Modifier.fillMaxWidth()
                .then(if (focused) Modifier.dashedBorder(p.dashed, 12.dp) else Modifier)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .focusRequester(fr)
                .onFocusChanged { f ->
                    if (focused && !f.isFocused && box.text.isBlank()) onDelete()
                    focused = f.isFocused
                },
            decorationBox = { inner ->
                Box {
                    if (box.text.isEmpty()) Txt("Type here…", T.body(19), color = p.muted)
                    inner()
                }
            },
        )
        if (focused) {
            Row(
                Modifier.align(Alignment.TopEnd).offset(y = (-46).dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(p.card).border(1.5.dp, p.border, RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                onMove(drag.x / density, drag.y / density)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { CozyIcon("drag", size = 20.dp) }
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(p.card).border(1.5.dp, p.border, RoundedCornerShape(20.dp))
                        .clickable(onClickLabel = "Delete text box", role = Role.Button, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) { CozyIcon("trash", size = 20.dp) }
            }
        }
    }
}

