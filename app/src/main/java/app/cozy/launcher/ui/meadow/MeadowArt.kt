package app.cozy.launcher.ui.meadow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

// Hand-drawn art for the Meadow theme. Every piece is drawn in its own little grid
// (the same numbers as the design mockups) and scaled into place.

val MInk = Color(0xFF4A3A2E)
val MBerry = Color(0xFFE2524B)
val MPaper = Color(0xFFFFFDF7)
private val MLeaf = Color(0xFF6FA548)
private val MSeed = Color(0xFFFFE7A3)
private val MWicker = Color(0xFFD8B888)
private val MWickerDark = Color(0xFFB8905E)

object Art {
    private val cache = HashMap<String, Path>()
    fun p(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

    const val BERRY = "M0 -9 C 11 -11 15 0 10 9 C 7 15 3 19 0 21 C -3 19 -7 15 -10 9 C -15 0 -11 -11 0 -9 Z"
    const val LEAVES = "M-9 -9 L-3 -11 L0 -16 L3 -11 L9 -9 L3 -7 L0 -4 L-3 -7 Z"
    val SEEDS = floatArrayOf(-5f, -2f, 0f, 1f, 5f, -2f, -3f, 7f, 3f, 7f, 0f, 13f, -7f, 4f, 7f, 4f)

    const val CUP = "M48 150 L252 150 L230 390 Q228 402 216 402 L84 402 Q72 402 70 390 Z"
    const val HEART_LABEL = "M150 290 C 132 276 128 262 138 256 C 144 252 150 256 150 262 C 150 256 156 252 162 256 C 172 262 168 276 150 290 Z"
    const val CREAM = "M52 152 C 44 118 80 100 112 100 C 108 76 132 58 160 62 C 190 66 204 90 190 104 C 228 108 256 128 248 152 Z"
    const val SWIRL = "M84 130 C 120 114 176 116 222 134 M124 96 C 140 86 164 88 176 98"
    const val MATCHA_DRIPS = "M66 150 q 6 20 12 0 M146 150 q 5 26 10 0 M214 150 q 6 18 12 0"
    const val HONEY = "M68 138 C 100 116 140 150 170 112 C 190 92 222 124 238 140"
    const val HONEY_DRIPS = "M120 150 q 5 24 10 0 M204 150 q 5 18 10 0"

    const val HANDLE = "M22 70 C 22 -40 178 -40 178 70"
    const val BASKET = "M10 70 L190 70 L176 176 Q174 188 162 188 L38 188 Q26 188 24 176 Z"
    const val BASKET_CLOTH = "M26 64 L174 64 L166 80 L34 80 Z"

    const val MINI_HANDLE = "M14 30 C 14 6 50 6 50 30"
    const val MINI_BASKET = "M8 32 L56 32 L51 54 Q50 58 46 58 L18 58 Q14 58 13 54 Z"
    const val HEART = "M32 54 C 12 40 8 26 16 18 C 22 12 30 14 32 22 C 34 14 42 12 48 18 C 56 26 52 40 32 54 Z"
    const val BOW = "M32 22 C 24 12 18 18 26 22 Z M32 22 C 40 12 46 18 38 22 Z"
    const val SUN_FACE = "M26 32 q1.5 -2 3 0 M35 32 q1.5 -2 3 0 M28 37 q4 3 8 0"

    const val HAND_CIRCLE = "M26 5 C 42 4 49 16 47 26 C 45 38 32 45 20 43 C 8 41 3 30 5 20 C 7 11 16 6 28 7"

    // Tile icons (56 grid)
    const val BOOK_SPINES = "M14 40 h10 M18 27 h10 M16 14 h10"
    const val NOTE_STEM = "M22 42 V16 L43 11 V37"
    const val BELL = "M13 39 C 17 35 16 29 16 25 C 16 16 21 11 28 11 C 35 11 40 16 40 25 C 40 29 39 35 43 39 Z"
    const val BELL_BOW = "M28 10 C 20 2 14 9 22 12 Z M28 10 C 36 2 42 9 34 12 Z"
    const val CAL_TOP = "M8 20 a8 8 0 0 1 8 -8 h24 a8 8 0 0 1 8 8 v4 h-40 z"
}

/** A strawberry centred at (cx, cy). [s] scales it; [stroke] is the outline width in the current units. */
fun DrawScope.drawStrawberry(
    cx: Float, cy: Float, s: Float,
    fill: Color = MBerry,
    seeds: Boolean = true,
    dashed: Boolean = false,
    rot: Float = 0f,
    stroke: Float = 1.6f * density,
    ink: Color = MInk,
) {
    withTransform({
        translate(cx, cy)
        if (rot != 0f) rotate(rot, Offset.Zero)
        scale(s, s, Offset.Zero)
    }) {
        val sw = stroke / s
        val body = Art.p(Art.BERRY)
        drawPath(body, fill)
        drawPath(
            body, ink,
            style = Stroke(sw, join = StrokeJoin.Round, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(sw * 2f, sw * 1.6f)) else null),
        )
        if (seeds) {
            var i = 0
            while (i < Art.SEEDS.size) {
                drawOval(MSeed, Offset(Art.SEEDS[i] - 0.9f, Art.SEEDS[i + 1] - 1.3f), Size(1.8f, 2.6f))
                i += 2
            }
        }
        val lv = Art.p(Art.LEAVES)
        drawPath(lv, MLeaf)
        drawPath(lv, ink, style = Stroke(sw, join = StrokeJoin.Round))
    }
}

private val CLOUD_BASE = floatArrayOf(0f, 0f, 34f, 32f, -20f, 40f, 70f, -8f, 34f, 98f, 8f, 24f, -30f, 10f, 24f, 38f, 14f, 30f, 70f, 16f, 24f)
private val CLOUD_TALL = floatArrayOf(10f, -54f, 34f, 46f, -70f, 40f, 24f, -104f, 32f, 58f, -118f, 28f, 78f, -60f, 30f, 40f, -146f, 24f, -14f, -30f, 28f, 88f, -30f, 30f, 30f, -170f, 16f)

/** A puffy cloud. [unit] = pixels per design unit. */
fun DrawScope.drawCloud(x: Float, y: Float, unit: Float, tall: Boolean = false, shade: Color = Color(0xFFDCE8F1), white: Color = Color.White) {
    fun pass(arr: FloatArray, c: Color, dy: Float) {
        var i = 0
        while (i < arr.size) {
            drawCircle(c, arr[i + 2] * unit, Offset(x + arr[i] * unit, y + (arr[i + 1] + dy) * unit))
            i += 3
        }
    }
    pass(CLOUD_BASE, shade, 7f)
    if (tall) pass(CLOUD_TALL, shade, 7f)
    pass(CLOUD_BASE, white, 0f)
    if (tall) pass(CLOUD_TALL, white, 0f)
}

fun DrawScope.drawDaisy(
    cx: Float, cy: Float, r: Float,
    petal: Color = Color.White,
    edge: Color = Color(0xFFD9C8B4),
    center: Color = Color(0xFFF2C14E),
    n: Int = 8,
) {
    val sw = r * 0.07f
    val tl = Offset(cx - r * 0.28f, cy - r * 0.62f - r * 0.45f)
    val sz = Size(r * 0.56f, r * 0.9f)
    for (k in 0 until n) {
        rotate(k * 360f / n, Offset(cx, cy)) {
            drawOval(petal, tl, sz)
            drawOval(edge, tl, sz, style = Stroke(sw))
        }
    }
    drawCircle(center, r * 0.3f, Offset(cx, cy))
}

/** A little white bunny standing on (x, y). */
fun DrawScope.drawBunny(x: Float, y: Float, unit: Float, flip: Boolean = false, stroke: Float = 1.6f * density, ink: Color = MInk) {
    withTransform({
        translate(x, y)
        scale(if (flip) -unit else unit, unit, Offset.Zero)
    }) {
        val st = Stroke(stroke / unit)
        val white = Color.White
        for (e in EARS) {
            rotate(e[2], Offset(e[0], e[1])) {
                drawOval(white, Offset(e[0] - 3.6f, e[1] - 10f), Size(7.2f, 20f))
                drawOval(ink, Offset(e[0] - 3.6f, e[1] - 10f), Size(7.2f, 20f), style = st)
                drawOval(Color(0xFFF4B6BD), Offset(e[0] - 1.5f, e[1] - 5.2f), Size(3f, 12.4f))
            }
        }
        drawOval(white, Offset(-17f, -24f), Size(34f, 24f))
        drawOval(ink, Offset(-17f, -24f), Size(34f, 24f), style = st)
        drawCircle(white, 10f, Offset(13f, -25f))
        drawCircle(ink, 10f, Offset(13f, -25f), style = st)
        drawCircle(white, 4.5f, Offset(-16f, -14f))
        drawCircle(ink, 4.5f, Offset(-16f, -14f), style = st)
        drawCircle(ink, 1.5f, Offset(17f, -26f))
        drawOval(Color(0xFFF4A7B3), Offset(16.4f, -22.5f), Size(5.2f, 3f))
        drawCircle(Color(0xFFE58A9A), 1f, Offset(22.4f, -24f))
    }
}

private val EARS = listOf(floatArrayOf(9f, -40f, -14f), floatArrayOf(17f, -41f, 12f))

// ---------- The drink cup (300 × 420 grid) ----------

fun cupColor(mode: String): Color = when (mode) {
    "short" -> Color(0xFFB9D38F)
    "long" -> Color(0xFFF5D98B)
    else -> Color(0xFFF6B8C0)
}

private fun liquidPath(frac: Float): Path {
    val y = 400f - 240f * frac.coerceIn(0.06f, 1f)
    val k = 22f / 240f
    val xl = 48f + (y - 150f) * k
    val xr = 252f - (y - 150f) * k
    val seg = (xr - xl) / 4f
    return Path().apply {
        moveTo(xl, y)
        for (i in 0 until 4) {
            val x0 = xl + seg * i
            quadraticBezierTo(x0 + seg / 2f, y + if (i % 2 == 1) 6f else -6f, x0 + seg, y)
        }
        lineTo(230f, 390f)
        quadraticBezierTo(228f, 402f, 216f, 402f)
        lineTo(84f, 402f)
        quadraticBezierTo(72f, 402f, 70f, 390f)
        close()
    }
}

/** Draws the cup to fill the current width. [frac] is how full it is; [bubbleT] animates bubbles (0..1). */
fun DrawScope.drawCup(frac: Float, mode: String, bubbleT: Float? = null) {
    val u = size.width / 300f
    withTransform({ scale(u, u, Offset.Zero) }) { drawCupLocal(frac, mode, bubbleT) }
}

fun DrawScope.drawCupLocal(frac: Float, mode: String, bubbleT: Float? = null) {
    val round = StrokeCap.Round
    drawLine(MInk, Offset(186f, 158f), Offset(216f, 26f), 18f, round)
    drawLine(Color(0xFF9CC6E4), Offset(186f, 158f), Offset(216f, 26f), 11f, round)
    drawPath(liquidPath(frac), cupColor(mode))
    if (mode == "focus") {
        rotate(-8f, Offset(86f, 330f)) {
            drawOval(MBerry, Offset(74f, 310f), Size(24f, 40f))
            drawOval(Color(0xFFF9D3D6), Offset(82f, 317f), Size(12f, 26f))
        }
        rotate(8f, Offset(214f, 350f)) {
            drawOval(MBerry, Offset(202f, 330f), Size(24f, 40f))
            drawOval(Color(0xFFF9D3D6), Offset(206f, 337f), Size(12f, 26f))
        }
    }
    if (bubbleT != null) {
        val xs = floatArrayOf(120f, 170f, 206f)
        val rs = floatArrayOf(7f, 5f, 6f)
        for (i in 0 until 3) {
            val ph = (bubbleT + i * 0.33f) % 1f
            val a = if (ph < 0.2f) ph / 0.2f * 0.8f else 0.8f * (1f - ph) / 0.8f
            drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), rs[i], Offset(xs[i], 380f - ph * 150f))
        }
    }
    val cup = Art.p(Art.CUP)
    drawPath(cup, Color.White.copy(alpha = 0.28f))
    drawPath(cup, MInk, style = Stroke(5f, join = StrokeJoin.Round))
    drawLine(Color.White.copy(alpha = 0.75f), Offset(70f, 176f), Offset(86f, 368f), 8f, round)
    drawRoundRect(MPaper, Offset(118f, 236f), Size(64f, 72f), androidx.compose.ui.geometry.CornerRadius(12f))
    drawRoundRect(MInk, Offset(118f, 236f), Size(64f, 72f), androidx.compose.ui.geometry.CornerRadius(12f), style = Stroke(3.5f))
    val heart = Art.p(Art.HEART_LABEL)
    drawPath(heart, MBerry)
    drawPath(heart, MInk, style = Stroke(2.5f, join = StrokeJoin.Round))
    val cream = Art.p(Art.CREAM)
    drawPath(cream, MPaper)
    drawPath(cream, MInk, style = Stroke(5f, join = StrokeJoin.Round))
    drawPath(Art.p(Art.SWIRL), Color(0xFFE7D9C6), style = Stroke(5f, cap = round))
    when (mode) {
        "short" -> {
            val dots = floatArrayOf(92f, 120f, 5f, 120f, 110f, 4f, 150f, 100f, 5f, 178f, 112f, 4f, 206f, 126f, 5f, 140f, 80f, 4f, 166f, 74f, 3.5f, 110f, 132f, 3.5f, 196f, 138f, 3.5f)
            var i = 0
            while (i < dots.size) { drawCircle(Color(0xFF7FA650), dots[i + 2], Offset(dots[i], dots[i + 1])); i += 3 }
            drawPath(Art.p(Art.MATCHA_DRIPS), Color(0xFF8DB35C), style = Stroke(7f, cap = round))
        }
        "long" -> {
            drawPath(Art.p(Art.HONEY), Color(0xFFE8B43C), style = Stroke(8f, cap = round))
            drawPath(Art.p(Art.HONEY_DRIPS), Color(0xFFE8B43C), style = Stroke(7f, cap = round))
        }
        else -> {
            drawStrawberry(112f, 86f, 2.3f, rot = -12f, stroke = 3f)
            drawStrawberry(196f, 82f, 2.1f, rot = 14f, stroke = 3f)
        }
    }
}

// ---------- Basket (200 × 190 grid) ----------

private val BASKET_SPOTS = floatArrayOf(58f, 60f, 90f, 56f, 122f, 58f, 152f, 62f, 74f, 42f, 106f, 38f, 138f, 44f)

/** A wicker basket holding [count] strawberries (up to 7). Top-left at (x, y). */
fun DrawScope.drawBasket(x: Float, y: Float, unit: Float, count: Int) {
    withTransform({
        translate(x, y)
        scale(unit, unit, Offset.Zero)
    }) {
        val handle = Art.p(Art.HANDLE)
        drawPath(handle, MInk, style = Stroke(20f, cap = StrokeCap.Round))
        drawPath(handle, MWicker, style = Stroke(12f, cap = StrokeCap.Round))
        val cloth = Art.p(Art.BASKET_CLOTH)
        drawPath(cloth, Color(0xFFFFF4EE))
        drawPath(cloth, MInk, style = Stroke(3f))
        for (i in 0 until count.coerceIn(0, 7)) {
            drawStrawberry(BASKET_SPOTS[i * 2], BASKET_SPOTS[i * 2 + 1], 1.55f, rot = ((i * 7) % 24 - 12).toFloat(), stroke = 2.4f)
        }
        val body = Art.p(Art.BASKET)
        drawPath(body, MWicker)
        for (yy in floatArrayOf(100f, 130f, 160f)) drawLine(MWickerDark, Offset(14f, yy), Offset(186f, yy), 5f)
        var xx = 40f
        while (xx < 180f) {
            drawLine(Color(0xFFC7A06F), Offset(xx, 72f), Offset(xx - 2f, 186f), 3f)
            xx += 22f
        }
        drawPath(body, MInk, style = Stroke(4f, join = StrokeJoin.Round))
        val tri = Path().apply { moveTo(30f, 70f); lineTo(60f, 70f); lineTo(44f, 104f); close() }
        drawPath(tri, Color(0xFFF4A6B8).copy(alpha = 0.9f))
    }
}

// ---------- Stickers (64 × 64 grid) ----------

val StickerKinds = listOf("strawberry", "bunny", "cloud", "daisy", "cup", "basket", "sun", "heart")

/** Draws a sticker to fill the current size. */
fun DrawScope.drawSticker(kind: String) {
    val u = size.minDimension / 64f
    withTransform({ scale(u, u, Offset.Zero) }) { drawStickerLocal(kind) }
}

fun DrawScope.drawStickerLocal(kind: String) {
    when (kind) {
        "strawberry" -> drawStrawberry(32f, 32f, 1.55f, stroke = 2.2f)
        "bunny" -> drawBunny(18f, 56f, 1.1f, stroke = 2f)
        "cloud" -> drawCloud(20f, 38f, 0.36f, shade = Color(0xFFCFE0EC))
        "daisy" -> drawDaisy(32f, 32f, 20f)
        "cup" -> withTransform({
            translate(13f, 2f)
            scale(0.14f, 0.14f, Offset.Zero)
        }) { drawCupLocal(0.6f, "short") }
        "basket" -> {
            val h = Art.p(Art.MINI_HANDLE)
            drawPath(h, MInk, style = Stroke(6f))
            drawPath(h, MWicker, style = Stroke(3f))
            drawStrawberry(24f, 30f, 0.7f, stroke = 1.6f)
            drawStrawberry(38f, 29f, 0.7f, rot = 10f, stroke = 1.6f)
            drawStrawberry(31f, 26f, 0.65f, stroke = 1.6f)
            val b = Art.p(Art.MINI_BASKET)
            drawPath(b, MWicker)
            drawLine(MWickerDark, Offset(11f, 40f), Offset(53f, 40f), 2.2f)
            drawLine(MWickerDark, Offset(12.5f, 48f), Offset(51.5f, 48f), 2.2f)
            drawPath(b, MInk, style = Stroke(2.4f, join = StrokeJoin.Round))
        }
        "sun" -> {
            for (a in 0 until 360 step 45) {
                rotate(a.toFloat(), Offset(32f, 32f)) { drawLine(Color(0xFFE8B43C), Offset(32f, 8f), Offset(32f, 14f), 3.2f, StrokeCap.Round) }
            }
            drawCircle(Color(0xFFF8D267), 13f, Offset(32f, 32f))
            drawCircle(MInk, 13f, Offset(32f, 32f), style = Stroke(2.2f))
            drawPath(Art.p(Art.SUN_FACE), MInk, style = Stroke(2f, cap = StrokeCap.Round))
        }
        "heart" -> {
            val h = Art.p(Art.HEART)
            drawPath(h, Color(0xFFF4A6B8))
            drawPath(h, MInk, style = Stroke(2.4f, join = StrokeJoin.Round))
            val bow = Art.p(Art.BOW)
            drawPath(bow, MPaper)
            drawPath(bow, MInk, style = Stroke(2f, join = StrokeJoin.Round))
        }
    }
}

// ---------- Illustrated tile icons (56 × 56 grid) ----------

val IllustratedIcons = setOf("notes", "book", "music", "bell", "timer", "calendar", "heart")

fun DrawScope.drawTileIcon(key: String) {
    val u = size.minDimension / 56f
    withTransform({ scale(u, u, Offset.Zero) }) {
        val ink = MInk
        val cr = androidx.compose.ui.geometry.CornerRadius(6f)
        when (key) {
            "notes" -> {
                drawRoundRect(MPaper, Offset(13f, 7f), Size(33f, 43f), cr)
                drawRoundRect(ink, Offset(13f, 7f), Size(33f, 43f), cr, style = Stroke(2.4f))
                for (yy in floatArrayOf(15f, 24f, 33f, 42f)) drawLine(ink, Offset(9f, yy), Offset(17f, yy), 2.6f, StrokeCap.Round)
                val line = Color(0xFFD9C8B4)
                drawLine(line, Offset(22f, 18f), Offset(38f, 18f), 2.6f, StrokeCap.Round)
                drawLine(line, Offset(22f, 26f), Offset(38f, 26f), 2.6f, StrokeCap.Round)
                drawLine(line, Offset(22f, 34f), Offset(31f, 34f), 2.6f, StrokeCap.Round)
                drawStrawberry(40f, 42f, 0.62f, stroke = 1.6f)
            }
            "book" -> {
                val r3 = androidx.compose.ui.geometry.CornerRadius(3f)
                drawRoundRect(Color(0xFFF4B6BD), Offset(7f, 34f), Size(42f, 12f), r3)
                drawRoundRect(ink, Offset(7f, 34f), Size(42f, 12f), r3, style = Stroke(2.4f))
                drawRoundRect(Color(0xFFCFE3B4), Offset(11f, 21f), Size(36f, 12f), r3)
                drawRoundRect(ink, Offset(11f, 21f), Size(36f, 12f), r3, style = Stroke(2.4f))
                rotate(-5f, Offset(28f, 14f)) {
                    drawRoundRect(Color(0xFFF8E4A8), Offset(9f, 8f), Size(38f, 12f), r3)
                    drawRoundRect(ink, Offset(9f, 8f), Size(38f, 12f), r3, style = Stroke(2.4f))
                }
                drawPath(Art.p(Art.BOOK_SPINES), ink.copy(alpha = 0.55f), style = Stroke(2f, cap = StrokeCap.Round))
            }
            "music" -> {
                drawPath(Art.p(Art.NOTE_STEM), ink, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(ink, 6.5f, Offset(16.5f, 42f))
                drawCircle(ink, 6.5f, Offset(37.5f, 37f))
                drawDaisy(12f, 13f, 9f, petal = Color(0xFFF7C9CF), edge = Color(0xFFE7A5B0), n = 6)
            }
            "bell" -> {
                val bell = Art.p(Art.BELL)
                drawPath(bell, Color(0xFFF8E4A8))
                drawPath(bell, ink, style = Stroke(2.4f, join = StrokeJoin.Round))
                drawCircle(ink, 3.6f, Offset(28f, 44f))
                val bow = Art.p(Art.BELL_BOW)
                drawPath(bow, Color(0xFFF4B6BD))
                drawPath(bow, ink, style = Stroke(2f, join = StrokeJoin.Round))
                drawCircle(Color(0xFFE58A9A), 2.6f, Offset(28f, 10f))
            }
            "timer" -> withTransform({
                translate(8f, 1f)
                scale(0.135f, 0.135f, Offset.Zero)
            }) { drawCupLocal(0.55f, "focus") }
            "calendar" -> {
                val r7 = androidx.compose.ui.geometry.CornerRadius(7f)
                drawRoundRect(MPaper, Offset(8f, 12f), Size(40f, 36f), r7)
                drawRoundRect(ink, Offset(8f, 12f), Size(40f, 36f), r7, style = Stroke(2.4f))
                val top = Art.p(Art.CAL_TOP)
                drawPath(top, Color(0xFFF4B6BD))
                drawPath(top, ink, style = Stroke(2.4f, join = StrokeJoin.Round))
                drawLine(ink, Offset(18f, 8f), Offset(18f, 16f), 2.6f, StrokeCap.Round)
                drawLine(ink, Offset(38f, 8f), Offset(38f, 16f), 2.6f, StrokeCap.Round)
                drawDaisy(28f, 36f, 8f)
            }
            "heart" -> withTransform({
                translate(-4f, -4f)
                scale(1f, 1f, Offset.Zero)
            }) { drawStickerLocal("heart") }
        }
    }
}

/** The pink hand-drawn ring for "today" on the calendar (52 × 48 grid). */
fun DrawScope.drawHandCircle(color: Color = Color(0xFFE0708A)) {
    val u = size.width / 52f
    withTransform({ scale(u, u, Offset.Zero) }) {
        drawPath(Art.p(Art.HAND_CIRCLE), color, style = Stroke(3f, cap = StrokeCap.Round))
    }
}

/** Red-and-white picnic gingham. */
fun DrawScope.drawGingham(topLeft: Offset, area: Size, cell: Float, base: Color = Color(0xFFFFF4EE), stripe: Color = Color(0x6BE25E56)) {
    drawRect(base, topLeft, area)
    var x = 0f
    while (x < area.width) {
        drawRect(stripe, Offset(topLeft.x + x, topLeft.y), Size(minOf(cell, area.width - x), area.height))
        x += cell * 2
    }
    var y = 0f
    while (y < area.height) {
        drawRect(stripe, Offset(topLeft.x, topLeft.y + y), Size(area.width, minOf(cell, area.height - y)))
        y += cell * 2
    }
}

/** A row of lace scallops along y (bulging upwards), in [color]. */
fun DrawScope.drawLace(y: Float, width: Float, color: Color = MPaper) {
    val step = 14f * density
    val r = 6.5f * density
    var x = step / 2
    while (x < width + step) {
        drawCircle(color, r, Offset(x, y))
        x += step
    }
}
