package app.cozy.launcher.ui.meadow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Store
import app.cozy.launcher.ui.CozyIcon
import app.cozy.launcher.ui.theme.LocalAnimate
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

/** A soft "stacked paper" shadow below a rounded shape. Put it before clip/background. */
fun Modifier.paperShadow(radius: Dp, color: Color = Color(0x1F4A3A2E), dy: Dp = 4.dp): Modifier = drawBehind {
    drawRoundRect(color, Offset(0f, dy.toPx()), size, CornerRadius(radius.toPx()))
}

fun Modifier.gingham(cell: Dp = 13.dp): Modifier = drawBehind {
    drawGingham(Offset.Zero, size, cell.toPx())
}

/** Lined notebook paper with a pink margin line. */
fun Modifier.notebookLines(step: Dp = 52.dp, margin: Dp? = 72.dp, first: Dp = 52.dp): Modifier = drawBehind {
    val line = Color(0xFFE7D9C6)
    var y = first.toPx()
    while (y < size.height) {
        drawRect(line, Offset(0f, y - 1f * density), Size(size.width, 2f * density))
        y += step.toPx()
    }
    if (margin != null) drawRect(Color(0xFFF0A3B1), Offset(margin.toPx(), 0f), Size(2f * density, size.height))
}

/** A cream paper label, like the ones in the mockups. */
@Composable
fun PaperTag(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp), content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.paperShadow(24.dp).clip(shape).background(p.card).border(2.dp, p.border, shape).padding(padding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/** A strip of washi tape. */
@Composable
fun Washi(modifier: Modifier, color: Color, width: Dp = 110.dp, angle: Float = -3f) {
    Box(modifier.rotate(angle).size(width, 28.dp).background(color.copy(alpha = 0.82f)))
}

/** Gingham header with a lace edge, used on "Today" cards. */
@Composable
fun GinghamHeader(title: String, modifier: Modifier = Modifier, height: Dp = 58.dp) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().height(height).gingham()) {
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 18.dp)
                .clip(RoundedCornerShape(14.dp)).background(p.card).padding(horizontal = 14.dp, vertical = 6.dp)
        ) { Txt(title, T.display(21, 500), maxLines = 1) }
        Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(12.dp)) {
            drawLace(size.height, size.width, p.card)
        }
    }
}

/** A tick box drawn as a strawberry: an outline when open, ripe and red when done. */
@Composable
fun StrawberryCheck(done: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 38f
        drawStrawberry(
            center.x, center.y - 2.5f * s, s,
            fill = if (done) MBerry else p.card,
            seeds = done,
            dashed = !done,
            stroke = 1.8f * density,
            ink = p.ink,
        )
    }
}

/** An illustrated icon for a home-screen tile, falling back to the line icon. */
@Composable
fun MeadowIcon(key: String, size: Dp) {
    if (key in IllustratedIcons) {
        Canvas(Modifier.size(size)) { drawTileIcon(key) }
    } else {
        CozyIcon(key, size = size * 0.8f)
    }
}

@Composable
fun StickerImage(kind: String, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawSticker(kind) }
}

/** The drink cup, with bubbles rising while [bubbling]. */
@Composable
fun CupView(frac: Float, mode: String, width: Dp, bubbling: Boolean = false, modifier: Modifier = Modifier) {
    val animate = LocalAnimate.current && bubbling
    val bubbleT = if (animate) {
        val t = rememberInfiniteTransition(label = "bubbles")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "bt")
    } else null
    Canvas(modifier.size(width, width * (420f / 300f))) {
        drawCup(frac, mode, bubbleT?.value)
    }
}

/** The painted banner at the top of a Meadow screen. */
@Composable
fun MeadowBanner(
    height: Dp,
    seed: Int,
    modifier: Modifier = Modifier,
    hy: Dp = height * 0.66f,
    dm: Dp = 40.dp,
    df: Dp = 76.dp,
    tall: SceneCloud? = null,
    clouds: List<SceneCloud> = listOf(SceneCloud(0.1f, 70f, 0.55f), SceneCloud(0.48f, 46f, 0.4f), SceneCloud(0.8f, 90f, 0.5f)),
    bunnies: List<SceneBunny> = emptyList(),
    flowers: Int = 80,
    content: @Composable BoxScope.() -> Unit,
) {
    val settings by Store.settings.collectAsState()
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))) {
        MeadowScene(
            Modifier.matchParentSize(),
            variant = skyVariant(settings),
            hy = hy, dm = dm, df = df, seed = seed,
            tall = tall, clouds = clouds,
            bunnies = if (settings.bunnies) bunnies else emptyList(),
            flowers = flowers,
            picnic = settings.scene == "picnic",
            animate = LocalAnimate.current && settings.driftClouds,
        )
        content()
    }
}
