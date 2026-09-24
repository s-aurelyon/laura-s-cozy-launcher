package app.cozy.launcher.ui.meadow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Settings
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class ScenePal(
    val sky: Array<Pair<Float, Color>>,
    val mtn: Color, val snow: Color,
    val h1: Color, val h2: Color, val h3: Color,
    val tree: Color, val tree2: Color,
    val cShade: Color, val cWhite: Color,
    val flowers: List<Color>,
    val tuft: Color,
    val night: Boolean = false,
)

val SunnyPal = ScenePal(
    arrayOf(0f to Color(0xFF5E9FD3), 0.42f to Color(0xFF8CC1E5), 0.76f to Color(0xFFD8EDF6), 1f to Color(0xFFEAF5EE)),
    Color(0xFFA7C0D3), Color.White,
    Color(0xFFA9D07A), Color(0xFF8DBF5A), Color(0xFF7AB04B),
    Color(0xFF5E9440), Color(0xFF6FA548),
    Color(0xFFDCE8F1), Color.White,
    listOf(Color.White, Color(0xFFF7AFC0), Color(0xFFFFE59A)),
    Color(0xFF5E9440),
)

val GoldenPal = ScenePal(
    arrayOf(0f to Color(0xFFE98E7A), 0.40f to Color(0xFFF4B48C), 0.78f to Color(0xFFFBDDB0), 1f to Color(0xFFFCEBCB)),
    Color(0xFFC9A1AE), Color(0xFFFCE6D8),
    Color(0xFFBFCB7C), Color(0xFFA2BC61), Color(0xFF8AAE4F),
    Color(0xFF6C8E45), Color(0xFF7FA150),
    Color(0xFFF2C4B4), Color(0xFFFFF3E8),
    listOf(Color(0xFFFFF3E0), Color(0xFFF7A8B8), Color(0xFFFFD27A)),
    Color(0xFF6C8E45),
)

val NightPal = ScenePal(
    arrayOf(0f to Color(0xFF1F2B55), 0.5f to Color(0xFF33457A), 1f to Color(0xFF5A6C9E)),
    Color(0xFF3C4A78), Color(0xFFC9D2EC),
    Color(0xFF3F6A55), Color(0xFF34604A), Color(0xFF2B533F),
    Color(0xFF22412F), Color(0xFF28493A),
    Color(0xFF6F7FB0), Color(0xFF98A6CF),
    listOf(Color(0xFFD9DEF2), Color(0xFFC7A3C8), Color(0xFFF2E39B)),
    Color(0xFF22412F),
    night = true,
)

fun scenePal(variant: String) = when (variant) {
    "golden" -> GoldenPal
    "night" -> NightPal
    else -> SunnyPal
}

/** Which sky to paint: follows the clock, or the scene she picked. */
fun skyVariant(s: Settings, hour: Int = LocalTime.now().hour): String =
    if (s.skyFollowsTime) {
        when (hour) {
            in 6..15 -> "sunny"
            in 16..18 -> "golden"
            else -> "night"
        }
    } else {
        when (s.scene) {
            "golden" -> "golden"
            "night" -> "night"
            else -> "sunny"
        }
    }

data class SceneCloud(val xFrac: Float, val yDp: Float, val scale: Float)
data class SceneBunny(val xFrac: Float, val yDp: Float, val scale: Float, val flip: Boolean = false)

private class Bloom(val fx: Float, val t: Float, val k: Float, val c: Int, val band: Int)
private class SceneSeed(val blooms: List<Bloom>, val tufts: FloatArray, val stars: FloatArray)

private fun makeSeed(seed: Int, n: Int, tuftCount: Int): SceneSeed {
    val r = Random(seed)
    fun pickColor(): Int {
        val t = r.nextFloat()
        return when {
            t < 0.45f -> 0
            t < 0.85f -> 1
            else -> 2
        }
    }
    val b = ArrayList<Bloom>()
    for ((band, count) in listOf(0 to n / 4, 1 to n / 3, 2 to n)) {
        repeat(count) { b += Bloom(r.nextFloat(), r.nextFloat(), 0.75f + r.nextFloat() * 0.35f, pickColor(), band) }
    }
    val tufts = FloatArray(tuftCount * 2) { r.nextFloat() }
    val stars = FloatArray(90) { r.nextFloat() }
    return SceneSeed(b, tufts, stars)
}

private class Motion(val drift: State<Float>, val drift2: State<Float>, val hop1: State<Float>, val hop2: State<Float>)

@Composable
private fun rememberMotion(): Motion {
    val t = rememberInfiniteTransition(label = "meadow")
    return Motion(
        t.animateFloat(0f, 26f, infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift"),
        t.animateFloat(26f, 0f, infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift2"),
        t.animateFloat(0f, 0f, infiniteRepeatable(keyframes {
            durationMillis = 3600
            0f at 2520
            -10f at 2808
            0f at 3096
        }), label = "hop1"),
        t.animateFloat(0f, 0f, infiniteRepeatable(keyframes {
            durationMillis = 4400
            0f at 3080
            -10f at 3432
            0f at 3784
        }), label = "hop2"),
    )
}

/**
 * The painted meadow: sky, clouds, mountains, three hills full of little flowers, and bunnies.
 * Drawn in three layers so only the moving bits (clouds, bunnies) redraw while animating.
 */
@Composable
fun MeadowScene(
    modifier: Modifier,
    variant: String,
    hy: Dp,
    dm: Dp = 55.dp,
    df: Dp = 100.dp,
    seed: Int = 1,
    tall: SceneCloud? = null,
    clouds: List<SceneCloud> = emptyList(),
    bunnies: List<SceneBunny> = emptyList(),
    flowers: Int = 90,
    tree: Boolean = true,
    mountains: Boolean = true,
    picnic: Boolean = false,
    wave: Color? = null,
    animate: Boolean = true,
) {
    val pal = scenePal(variant)
    val s = remember(seed, flowers) { makeSeed(seed, flowers, 70) }
    val motion = if (animate) rememberMotion() else null
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val d0 = motion?.drift?.value ?: 0f
            val d1 = motion?.drift2?.value ?: 0f
            drawSky(pal, s, hy.toPx(), clouds, tall, d0 * density, d1 * density)
        }
        Canvas(Modifier.matchParentSize()) {
            drawLand(pal, s, hy.toPx(), dm.toPx(), df.toPx(), tree, mountains, picnic, wave)
        }
        if (bunnies.isNotEmpty()) {
            Canvas(Modifier.matchParentSize()) {
                bunnies.forEachIndexed { i, b ->
                    val hop = if (motion == null) 0f else if (i % 2 == 0) motion.hop1.value else motion.hop2.value
                    drawBunny(b.xFrac * size.width, (b.yDp + hop) * density, b.scale * density, b.flip)
                }
            }
        }
    }
}

private fun DrawScope.drawSky(pal: ScenePal, s: SceneSeed, hy: Float, clouds: List<SceneCloud>, tall: SceneCloud?, drift: Float, drift2: Float) {
    val d = density
    drawRect(Brush.verticalGradient(*pal.sky, startY = 0f, endY = hy + 120f * d))
    if (pal.night) {
        var i = 0
        while (i + 2 < s.stars.size) {
            val x = s.stars[i] * size.width
            val y = s.stars[i + 1] * (hy - 20f * d)
            drawCircle(Color(0xFFFFF6D6).copy(alpha = 0.5f + s.stars[i + 2] * 0.5f), (0.6f + s.stars[i + 2]) * d, Offset(x, y))
            i += 3
        }
        val mx = size.width * 0.8f
        val my = hy * 0.28f
        drawCircle(Color(0xFFFFF3C4), 16f * d, Offset(mx, my))
        drawCircle(pal.sky[0].second, 14f * d, Offset(mx + 7f * d, my - 4f * d))
    }
    for (c in clouds) drawCloud(c.xFrac * size.width + drift, c.yDp * d, c.scale * d, false, pal.cShade, pal.cWhite)
    if (tall != null) drawCloud(tall.xFrac * size.width + drift2, hy - 40f * tall.scale * d, tall.scale * d, true, pal.cShade, pal.cWhite)
}

private fun DrawScope.drawLand(
    pal: ScenePal, s: SceneSeed, hy: Float, dm: Float, df: Float,
    tree: Boolean, mountains: Boolean, picnic: Boolean, wave: Color?,
) {
    val W = size.width
    val H = size.height
    val d = density
    val k = min(1f, W / (800f * d))
    if (mountains) {
        val pts = floatArrayOf(0f, -20f, .11f, -70f, .19f, -50f, .30f, -110f, .41f, -45f, .52f, -60f, .65f, -100f, .75f, -55f, .86f, -80f, 1f, -40f)
        val p = Path().apply {
            moveTo(0f, hy + 20f * d)
            var i = 0
            while (i < pts.size) {
                lineTo(pts[i] * W, hy + pts[i + 1] * d * k)
                i += 2
            }
            lineTo(W, hy + 20f * d)
            close()
        }
        drawPath(p, pal.mtn)
        var i = 0
        while (i < pts.size) {
            if (pts[i + 1] <= -80f) {
                val px = pts[i] * W
                val py = hy + pts[i + 1] * d * k
                val c = 18f * d * k
                val cap = Path().apply {
                    moveTo(px, py); lineTo(px + c, py + c); lineTo(px + c * .45f, py + c * .75f); lineTo(px, py + c * 1.2f)
                    lineTo(px - c * .4f, py + c * .8f); lineTo(px - c, py + c); close()
                }
                drawPath(cap, pal.snow)
            }
            i += 2
        }
    }

    // Back hill
    val b = hy + 10f * d
    drawPath(Path().apply {
        moveTo(0f, b)
        cubicTo(W * .15f, b - 30f * d, W * .32f, b - 14f * d, W * .48f, b + 2f * d)
        cubicTo(W * .64f, b + 18f * d, W * .8f, b - 26f * d, W, b - 6f * d)
        lineTo(W, H); lineTo(0f, H); close()
    }, pal.h1)
    if (tree) {
        val tx = W * .86f
        val ty = b - 14f * d
        val ts = k * d
        drawRect(Color(0xFF7A5A3E), Offset(tx - 3f * ts, ty - 10f * ts), Size(6f * ts, 16f * ts))
        drawCircle(pal.tree, 13f * ts, Offset(tx - 12f * ts, ty - 22f * ts))
        drawCircle(pal.tree, 14f * ts, Offset(tx + 10f * ts, ty - 24f * ts))
        drawCircle(pal.tree2, 15f * ts, Offset(tx, ty - 36f * ts))
        drawCircle(pal.tree2, 12f * ts, Offset(tx - 2f * ts, ty - 20f * ts))
    }
    blooms(pal, s, 0, W, b - 4f * d, hy + dm, 0.8f, 1.6f)

    // Middle hill
    val m = hy + dm
    drawPath(Path().apply {
        moveTo(0f, m)
        cubicTo(W * .18f, m - 28f * d, W * .38f, m - 8f * d, W * .54f, m + 6f * d)
        cubicTo(W * .70f, m + 20f * d, W * .84f, m - 22f * d, W, m - 4f * d)
        lineTo(W, H); lineTo(0f, H); close()
    }, pal.h2)
    blooms(pal, s, 1, W, m - 6f * d, hy + df, 1.2f, 2.6f)

    // Front meadow
    val fr = hy + df
    drawPath(Path().apply {
        moveTo(0f, fr)
        cubicTo(W * .2f, fr - 14f * d, W * .42f, fr + 6f * d, W * .6f, fr)
        cubicTo(W * .78f, fr - 6f * d, W * .86f, fr - 12f * d, W, fr - 4f * d)
        lineTo(W, H); lineTo(0f, H); close()
    }, pal.h3)
    blooms(pal, s, 2, W, fr - 4f * d, H - 4f * d, 2.2f, if (H - fr > 60f * d) 5.2f else 3.6f)

    // Grass tufts
    val tuftColor = pal.tuft.copy(alpha = 0.7f)
    var i = 0
    while (i + 1 < s.tufts.size) {
        val x = s.tufts[i] * W
        val y = fr + 6f * d + s.tufts[i + 1] * (H - fr - 6f * d)
        drawLine(tuftColor, Offset(x, y), Offset(x - 3f * d, y - 8f * d), 1.6f * d, StrokeCap.Round)
        drawLine(tuftColor, Offset(x + 2f * d, y), Offset(x + 3f * d, y - 10f * d), 1.6f * d, StrokeCap.Round)
        drawLine(tuftColor, Offset(x + 4f * d, y), Offset(x + 8f * d, y - 7f * d), 1.6f * d, StrokeCap.Round)
        i += 2
    }

    if (picnic) {
        val cx = W * 0.64f
        val cy = fr + (H - fr) * 0.45f
        withTransform({ translate(cx, cy) }) {
            val cloth = Path().apply {
                moveTo(0f, 20f * d); lineTo(140f * d, 0f); lineTo(180f * d, 40f * d); lineTo(30f * d, 64f * d); close()
            }
            drawPath(cloth, Color(0xFFE8665C).copy(alpha = 0.85f))
            drawPath(cloth, Color.White.copy(alpha = 0.25f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f * d))
        }
        drawBasket(cx + 50f * d, cy - 20f * d, 0.34f * d, 5)
    }

    if (wave != null) {
        drawPath(Path().apply {
            moveTo(0f, H - 16f * d)
            cubicTo(W * .125f, H - 30f * d, W * .25f, H - 6f * d, W * .375f, H - 20f * d)
            cubicTo(W * .5f, H - 34f * d, W * .625f, H - 32f * d, W * .75f, H - 16f * d)
            cubicTo(W * .875f, H, W * .925f, H - 6f * d, W, H - 22f * d)
            lineTo(W, H); lineTo(0f, H); close()
        }, wave)
    }
}

private fun DrawScope.blooms(pal: ScenePal, s: SceneSeed, band: Int, W: Float, y0: Float, y1: Float, rmin: Float, rmax: Float) {
    val d = density
    val centre = Color(0xFFF2C14E)
    for (bl in s.blooms) {
        if (bl.band != band) continue
        val x = bl.fx * W
        val y = y0 + (y1 - y0) * bl.t
        val r = (rmin + (rmax - rmin) * bl.t) * bl.k * d
        val c = pal.flowers[bl.c]
        if (r > 3.3f * d) {
            for (p in 0 until 5) {
                val a = p * 72f * (Math.PI.toFloat() / 180f)
                drawCircle(c, r * 0.5f, Offset(x + cos(a) * r * 0.62f, y + sin(a) * r * 0.62f))
            }
            drawCircle(centre, r * 0.3f, Offset(x, y))
        } else {
            drawCircle(c, r * 0.7f, Offset(x, y))
        }
    }
}

