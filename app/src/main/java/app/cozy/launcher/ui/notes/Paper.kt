package app.cozy.launcher.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private val lineColor = Color(0xFFD9CBBB)
private val dotColor = Color(0xFFC4B2A0)
private val gridColor = Color(0xFFE3D6C7)
private val marginColor = Color(0xFFF0A3B1)

/** Draws the paper pattern behind a note. [top] leaves room for the title. */
fun DrawScope.drawPaper(kind: String, top: Float = 0f) {
    val d = density
    when (kind) {
        "lined", "cornell" -> {
            val step = 36f * d
            var y = top + step
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f * d)
                y += step
            }
            if (kind == "cornell") {
                val x = 96f * d
                drawLine(marginColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.6f * d)
            }
        }
        "dotted" -> {
            val step = 24f * d
            var y = step
            while (y < size.height) {
                var x = step
                while (x < size.width) {
                    drawCircle(dotColor, radius = 1.4f * d, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        "grid" -> {
            val step = 24f * d
            var x = step
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f * d)
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f * d)
                y += step
            }
        }
    }
}

/** Miniature pictures of each template for the picker. */
@Composable
fun TemplateThumb(kind: String, modifier: Modifier = Modifier, paper: Color = Color(0xFFFFFDF8), ink: Color = Color(0xFF3A2E2A), pink: Color = Color(0xFFF4C2CB), mint: Color = Color(0xFFCFE8D8)) {
    Canvas(modifier) {
        val d = density
        drawRoundRect(paper, cornerRadius = CornerRadius(14f * d))
        drawRoundRect(Color(0xFFEFE6DA), cornerRadius = CornerRadius(14f * d), style = Stroke(1.5f * d))
        val pad = 16f * d
        val w = size.width - pad * 2
        fun bar(x: Float, y: Float, width: Float, h: Float, c: Color) =
            drawRoundRect(c, topLeft = Offset(x, y), size = Size(width, h), cornerRadius = CornerRadius(h / 2))

        when (kind) {
            "lined" -> {
                var y = 18f * d
                while (y < size.height - 6 * d) { drawLine(lineColor, Offset(4 * d, y), Offset(size.width - 4 * d, y), 1.2f * d); y += 18f * d }
            }
            "dotted" -> {
                var y = 14f * d
                while (y < size.height - 6 * d) {
                    var x = 14f * d
                    while (x < size.width - 6 * d) { drawCircle(dotColor, 1.3f * d, Offset(x, y)); x += 14f * d }
                    y += 14f * d
                }
            }
            "grid" -> {
                var x = 16f * d
                while (x < size.width) { drawLine(gridColor, Offset(x, 2 * d), Offset(x, size.height - 2 * d), 1f * d); x += 16f * d }
                var y = 16f * d
                while (y < size.height) { drawLine(gridColor, Offset(2 * d, y), Offset(size.width - 2 * d, y), 1f * d); y += 16f * d }
            }
            "checklist" -> {
                bar(pad, pad, w * 0.6f, 8 * d, ink)
                for (i in 0 until 4) {
                    val y = pad + (24 + i * 28) * d
                    val box = 14f * d
                    if (i == 0) drawRoundRect(pink, Offset(pad, y), Size(box, box), CornerRadius(4 * d))
                    drawRoundRect(ink, Offset(pad, y), Size(box, box), CornerRadius(4 * d), style = Stroke(1.5f * d))
                    bar(pad + box + 8 * d, y + 5 * d, (w - box - 8 * d) * listOf(1f, 1f, 0.6f, 0.75f)[i], 5 * d, lineColor)
                }
            }
            "table" -> {
                bar(pad, pad, w * 0.5f, 8 * d, ink)
                val top = pad + 22 * d
                val rows = 4
                val rh = 22f * d
                drawRect(mint, Offset(pad, top), Size(w, rh))
                for (r in 0..rows) drawLine(Color(0xFFCBB9A6), Offset(pad, top + r * rh), Offset(pad + w, top + r * rh), 1.5f * d)
                for (c in 0..3) drawLine(Color(0xFFCBB9A6), Offset(pad + c * w / 3, top), Offset(pad + c * w / 3, top + rows * rh), 1.5f * d)
            }
            "daily" -> {
                bar(pad, pad, w * 0.45f, 8 * d, ink)
                drawCircle(pink, 11 * d, Offset(size.width - pad - 11 * d, pad + 4 * d))
                val colW = w * 0.58f
                for (i in 0 until 8) {
                    val y = pad + (28 + i * 14) * d
                    drawLine(lineColor, Offset(pad, y), Offset(pad + colW, y), 1.5f * d)
                }
                val bx = pad + colW + 8 * d
                val bw = w - colW - 8 * d
                val h = (size.height - pad * 2 - 28 * d - 6 * d) / 2
                drawRoundRect(Color(0xFFF5EEE4), Offset(bx, pad + 26 * d), Size(bw, h), CornerRadius(6 * d))
                drawRoundRect(mint, Offset(bx, pad + 32 * d + h), Size(bw, h), CornerRadius(6 * d))
            }
            "weekly" -> {
                val gap = 6f * d
                val cw = (size.width - 28 * d - gap) / 2
                val ch = (size.height - 28 * d - gap * 3) / 4
                for (i in 0 until 8) {
                    val x = 14 * d + (i % 2) * (cw + gap)
                    val y = 14 * d + (i / 2) * (ch + gap)
                    if (i == 7) drawRoundRect(pink, Offset(x, y), Size(cw, ch), CornerRadius(6 * d))
                    else drawRoundRect(lineColor, Offset(x, y), Size(cw, ch), CornerRadius(6 * d), style = Stroke(1.5f * d))
                }
            }
            "cornell" -> {
                var y = 18f * d
                while (y < size.height - 50 * d) { drawLine(lineColor, Offset(4 * d, y), Offset(size.width - 4 * d, y), 1.2f * d); y += 18f * d }
                drawLine(marginColor, Offset(size.width * 0.32f, 2 * d), Offset(size.width * 0.32f, size.height - 46 * d), 2f * d)
                drawLine(marginColor, Offset(2 * d, size.height - 46 * d), Offset(size.width - 2 * d, size.height - 46 * d), 2f * d)
            }
        }
    }
}
