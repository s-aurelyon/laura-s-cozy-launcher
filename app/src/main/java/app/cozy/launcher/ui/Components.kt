package app.cozy.launcher.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

// ---------- Layout ----------

/** The page every screen sits on: cream background, content centred on wide screens. */
@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(p.bg), contentAlignment = Alignment.TopCenter) {
        Column(modifier.widthIn(max = 860.dp).fillMaxSize(), content = content)
    }
}

fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 2.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f * density / 2, 7f * density / 2))),
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.card,
    border: Color? = LocalPalette.current.border,
    radius: Dp = 28.dp,
    padding: PaddingValues = PaddingValues(24.dp),
    spacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(radius)
    var m = modifier.clip(shape).background(color, shape)
    if (border != null) m = m.border(p.line, border, shape)
    Column(m.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Txt(text, T.body(16, 700), modifier, color = LocalPalette.current.muted)
}

@Composable
fun Tag(text: String, color: Color = LocalPalette.current.soft) {
    val p = LocalPalette.current
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(color).padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Txt(text, T.body(14, 700), color = p.muted, maxLines = 1) }
}

// ---------- Buttons ----------

@Composable
fun RoundButton(
    icon: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    filled: Boolean = false,
    tint: Color? = null,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(size / 2)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (filled) p.ink else p.card)
            .border(p.line, if (filled) p.ink else p.border, shape)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CozyIcon(icon, size = size * 0.44f, tint = tint ?: if (filled) p.onInk else p.ink)
    }
}

enum class PillStyle { DARK, LIGHT, SOFT, DASHED, ACCENT }

@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.LIGHT,
    icon: String? = null,
    textSize: Int = 17,
    padding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    val bg = when (style) {
        PillStyle.DARK -> p.ink
        PillStyle.LIGHT -> p.card
        PillStyle.SOFT -> p.soft
        PillStyle.DASHED -> Color.Transparent
        PillStyle.ACCENT -> p.accent
    }
    val fg = when (style) {
        PillStyle.DARK -> p.onInk
        PillStyle.DASHED -> p.muted
        else -> p.ink
    }
    var m = modifier.clip(shape).background(bg)
    m = when (style) {
        PillStyle.LIGHT -> m.border(p.line, p.border, shape)
        PillStyle.DASHED -> m.dashedBorder(p.dashed, 24.dp, p.line)
        PillStyle.ACCENT -> if (p.eink) m.border(p.line, p.ink, shape) else m
        else -> m
    }
    Row(
        m.clickable(role = Role.Button, onClick = onClick).padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) CozyIcon(icon, size = 20.dp, tint = fg, weight = 2.4f)
        Txt(text, T.body(textSize, 700), color = fg, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Pill(
        text, onClick, modifier,
        style = if (selected) PillStyle.DARK else PillStyle.LIGHT,
        textSize = 16,
        padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, description: String) {
    val p = LocalPalette.current
    Box(
        Modifier
            .width(54.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) p.ink else p.dashed)
            .clickable(onClickLabel = description, role = Role.Switch) { onChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(Color.White))
    }
}

/** The round tick box used for reminders and checklists. */
@Composable
fun CheckMark(done: Boolean, onClick: () -> Unit, size: Dp = 30.dp, square: Boolean = false) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(if (square) size * 0.28f else size / 2)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (done) p.accent else p.card)
            .border(2.5.dp, p.ink, shape)
            .clickable(onClickLabel = if (done) "Mark as not done" else "Mark as done", role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) CozyIcon("check", size = size * 0.6f, weight = 3f)
    }
}

// ---------- Header ----------

@Composable
fun Header(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) RoundButton("back", "Back", onBack)
        Txt(title, T.display(40), Modifier.weight(1f).padding(start = 4.dp), maxLines = 1)
        actions()
    }
}

// ---------- Text fields ----------

@Composable
fun CozyField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = T.body(19),
    singleLine: Boolean = true,
    boxed: Boolean = true,
    leadingIcon: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    val outer = if (boxed) modifier.clip(shape).background(p.card).border(p.line, p.border, shape).padding(horizontal = 18.dp, vertical = 14.dp) else modifier
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = style.copy(color = p.ink),
        cursorBrush = SolidColor(p.ink),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = outer,
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (leadingIcon != null) CozyIcon(leadingIcon, size = 22.dp, tint = p.muted)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Txt(placeholder, style, color = p.muted, maxLines = 1)
                    inner()
                }
            }
        },
    )
}

// ---------- Dialog ----------

@Composable
fun CozyDialog(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.widthIn(max = 560.dp).fillMaxWidth(), radius = 30.dp, padding = PaddingValues(28.dp), spacing = 18.dp) {
            Txt(title, T.display(28, 500), color = p.ink)
            content()
        }
    }
}

@Composable
fun DialogButtons(confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit, danger: String? = null, onDanger: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (danger != null && onDanger != null) Pill(danger, onDanger, style = PillStyle.SOFT, icon = "trash")
        Spacer(Modifier.weight(1f))
        Pill("Cancel", onCancel, style = PillStyle.LIGHT)
        Pill(confirm, onConfirm, style = PillStyle.DARK)
    }
}

// ---------- Time ----------

/** The current time, refreshed every [intervalMs]. */
@Composable
fun rememberNow(intervalMs: Long = 1000L): Long {
    val now by produceState(System.currentTimeMillis(), intervalMs) {
        while (true) {
            delay(intervalMs)
            value = System.currentTimeMillis()
        }
    }
    return now
}

fun pickDate(ctx: Context, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(ctx, { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
}

fun pickTime(ctx: Context, initial: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(ctx, { _, h, min -> onPicked(LocalTime.of(h, min)) }, initial.hour, initial.minute, true).show()
}
