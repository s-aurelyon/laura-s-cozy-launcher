package app.cozy.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cozy.launcher.ui.theme.LocalAnimate
import app.cozy.launcher.ui.theme.LocalPalette

enum class Mood { HAPPY, SLEEPY, JOY }

private val mouth = PathParser().parsePathString("M54 76q6 6 12 0").toPath()
private val bigSmile = PathParser().parsePathString("M52 75q8 9 16 0").toPath()
private val sleepyEyes = PathParser().parsePathString("M38 64q6 5 12 0M70 64q6 5 12 0").toPath()
private val joyEyes = PathParser().parsePathString("M38 66q6-7 12 0M70 66q6-7 12 0").toPath()

/** Laura's little bunny-dumpling. Drawn on a 120×112 grid. */
@Composable
fun Mascot(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    mood: Mood = Mood.HAPPY,
    bob: Boolean = true,
) {
    val p = LocalPalette.current
    val animate = LocalAnimate.current
    val t = rememberInfiniteTransition(label = "mascot")
    val lift by t.animateFloat(
        initialValue = 0f, targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val blink by t.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 5200
                1f at 0
                1f at 4800
                0.1f at 4950
                1f at 5100
            }
        ),
        label = "blink",
    )
    val ink = p.ink
    val body = if (p.eink) Color.White else Color(0xFFFFFDF8)
    val blush = if (p.eink) Color.Transparent else Color(0xFFF0A3B1)
    val dy = if (animate && bob) lift else 0f
    val eye = if (animate) blink else 1f

    Canvas(modifier.size(size, size * (112f / 120f))) {
        val s = this.size.width / 120f
        scale(s, s, pivot = Offset.Zero) {
            translate(top = dy) {
                drawMascot(ink, body, blush, mood, eye)
            }
        }
    }
}

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, fill: Color, ink: Color?, w: Float = 3f) {
    drawOval(fill, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
    if (ink != null) drawOval(ink, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2), style = Stroke(w))
}

private fun DrawScope.drawMascot(ink: Color, body: Color, blush: Color, mood: Mood, eye: Float) {
    rotate(-14f, pivot = Offset(38f, 26f)) { oval(38f, 26f, 11f, 18f, body, ink) }
    rotate(14f, pivot = Offset(82f, 26f)) { oval(82f, 26f, 11f, 18f, body, ink) }
    oval(60f, 68f, 48f, 40f, body, ink)
    val round = Stroke(3f, cap = StrokeCap.Round)
    when (mood) {
        Mood.HAPPY -> {
            oval(44f, 64f, 4f, 5f * eye, ink, null)
            oval(76f, 64f, 4f, 5f * eye, ink, null)
        }
        Mood.SLEEPY -> drawPath(sleepyEyes, ink, style = round)
        Mood.JOY -> drawPath(joyEyes, ink, style = round)
    }
    oval(34f, 78f, 7f, 4f, blush, null)
    oval(86f, 78f, 7f, 4f, blush, null)
    drawPath(if (mood == Mood.JOY) bigSmile else mouth, ink, style = round)
}

/** A four-point sparkle that twinkles. */
@Composable
fun Sparkle(modifier: Modifier = Modifier, size: Dp = 22.dp, delayMs: Int = 0, color: Color = Color.White) {
    val animate = LocalAnimate.current
    val t = rememberInfiniteTransition(label = "sparkle")
    val a by t.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, delayMillis = delayMs, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle",
    )
    val alpha = if (animate) a else 1f
    val star = remember24Star
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawPath(star, color.copy(alpha = alpha))
        }
    }
}

private val remember24Star = PathParser().parsePathString("M12 2l2.4 7.6L22 12l-7.6 2.4L12 22l-2.4-7.6L2 12l7.6-2.4z").toPath()
