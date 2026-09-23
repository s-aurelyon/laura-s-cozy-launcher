package app.cozy.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cozy.launcher.ui.theme.LocalPalette

/** The line icons from the design, drawn from SVG path data on a 24×24 grid. */
object Icons {
    private class Part(val d: String, val fill: Boolean = false)

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 ${-r},$r h${-(w - 2 * r)} a$r,$r 0 0 1 ${-r},${-r} v${-(h - 2 * r)} a$r,$r 0 0 1 $r,${-r} z"

    private val defs: Map<String, List<Part>> = mapOf(
        "notes" to listOf(Part("M6 3h9l4 4v14H6z"), Part("M15 3v4h4"), Part("M9 12h7M9 16h5")),
        "book" to listOf(Part("M12 6c-2-2-5-2-8-1v14c3-1 6-1 8 1 2-2 5-2 8-1V5c-3-1-6-1-8 1z"), Part("M12 6v14")),
        "music" to listOf(Part("M9 18V5l11-2v13"), Part(circle(6f, 18f, 3f)), Part(circle(17f, 16f, 3f))),
        "bell" to listOf(Part("M6 9a6 6 0 0 1 12 0c0 6 3 8 3 8H3s3-2 3-8"), Part("M10 20a2 2 0 0 0 4 0")),
        "timer" to listOf(Part(circle(12f, 13f, 8f)), Part("M12 9v4l2.5 2"), Part("M9 2h6")),
        "calendar" to listOf(Part(rrect(3.5f, 5f, 17f, 15.5f, 3f)), Part("M8 3v4M16 3v4M3.5 10h17"), Part(circle(12f, 15f, 1.4f), true)),
        "apps" to listOf(Part(rrect(4f, 4f, 6f, 6f, 2f)), Part(rrect(14f, 4f, 6f, 6f, 2f)), Part(rrect(4f, 14f, 6f, 6f, 2f)), Part(rrect(14f, 14f, 6f, 6f, 2f))),
        "heart" to listOf(Part("M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.5-7 10-7 10z")),
        "star" to listOf(Part("M12 3l2.6 5.6 6 0.6-4.5 4 1.3 6L12 16.2 6.6 19.2l1.3-6-4.5-4 6-0.6z")),
        "pencil" to listOf(Part("M4 20h4L20 8l-4-4L4 16z"), Part("M14 6l4 4")),
        "back" to listOf(Part("M15 5l-7 7 7 7")),
        "forward" to listOf(Part("M9 5l7 7-7 7")),
        "plus" to listOf(Part("M12 5v14M5 12h14")),
        "search" to listOf(Part(circle(11f, 11f, 7f)), Part("M20 20l-4-4")),
        "pin" to listOf(Part("M9 4h6l-1 6 4 3H6l4-3z"), Part("M12 13v7")),
        "share" to listOf(Part("M12 3v12M7 8l5-5 5 5"), Part("M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6")),
        "more" to listOf(Part(circle(5f, 12f, 2f), true), Part(circle(12f, 12f, 2f), true), Part(circle(19f, 12f, 2f), true)),
        "check" to listOf(Part("M5 12l5 5L20 7")),
        "close" to listOf(Part("M6 6l12 12M18 6L6 18")),
        "trash" to listOf(Part("M4 7h16"), Part("M9 7V4h6v3"), Part("M6 7l1 13h10l1-13")),
        "pen" to listOf(Part("M4 20l2-6L16 4l4 4L10 18z"), Part("M14 6l4 4")),
        "highlighter" to listOf(Part("M9 15l6-10 4 2.5-6 10z"), Part("M9 15l-1 4h4"), Part("M4 21h16")),
        "eraser" to listOf(Part("M8 20h12"), Part("M4.5 15.5l9-9a2 2 0 0 1 2.8 0l2.2 2.2a2 2 0 0 1 0 2.8L12 18H7z")),
        "table" to listOf(Part(rrect(3f, 4f, 18f, 16f, 2.5f)), Part("M3 10h18M3 15h18M9.5 4v16M15.5 4v16")),
        "checklist" to listOf(Part(rrect(3f, 4f, 6f, 6f, 1.5f)), Part("M4.5 7l1.2 1.2L8 6"), Part(rrect(3f, 14f, 6f, 6f, 1.5f)), Part("M12 7h9M12 17h9")),
        "bullets" to listOf(Part(circle(5f, 7f, 1.3f), true), Part(circle(5f, 12f, 1.3f), true), Part(circle(5f, 17f, 1.3f), true), Part("M10 7h10M10 12h10M10 17h10")),
        "undo" to listOf(Part("M9 14L4 9l5-5"), Part("M4 9h10a6 6 0 0 1 0 12h-3")),
        "sun" to listOf(Part(circle(12f, 12f, 4f)), Part("M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.4 1.4M17.6 17.6L19 19M5 19l1.4-1.4M17.6 6.4L19 5")),
        "list" to listOf(Part(circle(4.5f, 6f, 1.3f), true), Part(circle(4.5f, 12f, 1.3f), true), Part(circle(4.5f, 18f, 1.3f), true), Part("M9 6h11M9 12h11M9 18h11")),
        "circlecheck" to listOf(Part(circle(12f, 12f, 9f)), Part("M8 12l3 3 5-6")),
        "settings" to listOf(Part(circle(12f, 12f, 3f)), Part("M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1")),
        "keyboard" to listOf(Part(rrect(2.5f, 6f, 19f, 12f, 2.5f)), Part("M6 10h1M10 10h1M14 10h1M18 10h0.01M7 14h10")),
        "text" to listOf(Part("M5 6V4h14v2"), Part("M12 4v16"), Part("M9 20h6")),
        "repeat" to listOf(Part("M17 2l3 3-3 3"), Part("M4 11V9a4 4 0 0 1 4-4h12"), Part("M7 22l-3-3 3-3"), Part("M20 13v2a4 4 0 0 1-4 4H4")),
        "folder" to listOf(Part("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")),
        "drag" to listOf(Part(circle(9f, 6f, 1.3f), true), Part(circle(15f, 6f, 1.3f), true), Part(circle(9f, 12f, 1.3f), true), Part(circle(15f, 12f, 1.3f), true), Part(circle(9f, 18f, 1.3f), true), Part(circle(15f, 18f, 1.3f), true)),
        "camera" to listOf(Part(rrect(3f, 7f, 18f, 13f, 3f)), Part("M9 7l1.5-3h3L15 7"), Part(circle(12f, 13.5f, 3.5f))),
        "chat" to listOf(Part("M4 5h16v11H9l-5 4z")),
        "globe" to listOf(Part(circle(12f, 12f, 9f)), Part("M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18")),
        "phone" to listOf(Part(rrect(7f, 2.5f, 10f, 19f, 2.5f)), Part("M11 18h2")),
    )

    val pickable = listOf("notes", "book", "music", "bell", "timer", "calendar", "heart", "star", "camera", "chat", "globe", "phone", "apps")

    private val cache = HashMap<String, List<Pair<Path, Boolean>>>()

    fun paths(key: String): List<Pair<Path, Boolean>> = cache.getOrPut(key) {
        (defs[key] ?: defs.getValue("star")).map { PathParser().parsePathString(it.d).toPath() to it.fill }
    }
}

@Composable
fun CozyIcon(
    key: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalPalette.current.ink,
    weight: Float = 2f,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            Icons.paths(key).forEach { (path, fill) ->
                if (fill) drawPath(path, tint)
                else drawPath(path, tint, style = Stroke(width = weight, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
