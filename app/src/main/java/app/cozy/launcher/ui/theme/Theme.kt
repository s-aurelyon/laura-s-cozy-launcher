package app.cozy.launcher.ui.theme

import android.content.res.AssetManager
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The colours of the design. E-ink mode swaps in a black-and-white set. */
data class Palette(
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val card: Color,
    val border: Color,
    val soft: Color,
    val accent: Color,
    val mint: Color,
    val blush: Color,
    val dashed: Color,
    val holiday: Color,
    val butter: Color,
    val blushInk: Color,
    val onInk: Color,
    val eink: Boolean,
    val line: Dp,
    val meadow: Boolean = false,
    /** Strong strawberry red for buttons. */
    val berry: Color = Color(0xFFC2382F),
    val skyCard: Color = Color(0xFFCFE8D8),
    val skyBorder: Color = Color(0xFFCFE8D8),
)

fun cozyPalette(accent: Color) = Palette(
    bg = Color(0xFFFBF6EF),
    ink = Color(0xFF3A2E2A),
    muted = Color(0xFF6E5E55),
    card = Color.White,
    border = Color(0xFFEFE6DA),
    soft = Color(0xFFF5EEE4),
    accent = accent,
    mint = Color(0xFFCFE8D8),
    blush = Color(0xFFFDEEF1),
    dashed = Color(0xFFCBB9A6),
    holiday = Color(0xFFA04A3E),
    butter = Color(0xFFF6E2A8),
    blushInk = Color(0xFFF0A3B1),
    onInk = Color.White,
    eink = false,
    line = 2.dp,
)

fun meadowPalette() = Palette(
    bg = Color(0xFFFFF8EC),
    ink = Color(0xFF4A3A2E),
    muted = Color(0xFF7A6656),
    card = Color(0xFFFFFDF7),
    border = Color(0xFFEADFCB),
    soft = Color(0xFFF5EEE4),
    accent = Color(0xFFF7C9CF),
    mint = Color(0xFFCFE3B4),
    blush = Color(0xFFFDEEF1),
    dashed = Color(0xFFCBB9A6),
    holiday = Color(0xFFA04A3E),
    butter = Color(0xFFF8E4A8),
    blushInk = Color(0xFFF0A3B1),
    onInk = Color.White,
    eink = false,
    line = 2.dp,
    meadow = true,
    skyCard = Color(0xFFDCEEF7),
    skyBorder = Color(0xFFC4DDEB),
)

fun einkPalette() = Palette(
    bg = Color.White,
    ink = Color(0xFF111111),
    muted = Color(0xFF333333),
    card = Color.White,
    border = Color(0xFF111111),
    soft = Color(0xFFEDEDED),
    accent = Color(0xFFE2E2E2),
    mint = Color(0xFFEDEDED),
    blush = Color(0xFFF2F2F2),
    dashed = Color(0xFF555555),
    holiday = Color(0xFF111111),
    butter = Color(0xFFE2E2E2),
    blushInk = Color(0xFF999999),
    onInk = Color.White,
    eink = true,
    line = 2.5.dp,
)

val LocalPalette = staticCompositionLocalOf { cozyPalette(Color(0xFFF4C2CB)) }
val LocalAnimate = staticCompositionLocalOf { true }

val accentChoices = listOf(0xFFF4C2CB, 0xFFD9D0F2, 0xFFF6E2A8, 0xFFBFE0F0)

/** Fonts from assets/fonts when present, system fonts otherwise. */
object Fonts {
    var display: FontFamily = FontFamily.SansSerif
        private set
    var body: FontFamily = FontFamily.SansSerif
        private set
    var serif: FontFamily = FontFamily.Serif
        private set
    var hand: FontFamily = FontFamily.Cursive
        private set
    val mono: FontFamily = FontFamily.Monospace

    fun init(assets: AssetManager) {
        display = load(assets, "fredoka.ttf", null, FontFamily.SansSerif, 500, 600)
        body = load(assets, "nunito.ttf", null, FontFamily.SansSerif, 400, 600, 700, 800)
        serif = load(assets, "fraunces.ttf", "fraunces_italic.ttf", FontFamily.Serif, 500, 600)
        hand = load(assets, "caveat.ttf", null, FontFamily.Cursive, 500, 700)
    }

    private fun exists(assets: AssetManager, file: String): Boolean = try {
        assets.open("fonts/$file").close(); true
    } catch (e: Exception) {
        false
    }

    @OptIn(ExperimentalTextApi::class)
    private fun load(assets: AssetManager, file: String, italicFile: String?, fallback: FontFamily, vararg weights: Int): FontFamily {
        if (!exists(assets, file)) return fallback
        return try {
            val fonts = mutableListOf<Font>()
            weights.forEach { w -> fonts += Font("fonts/$file", assets, FontWeight(w), FontStyle.Normal) }
            if (italicFile != null && exists(assets, italicFile)) {
                weights.forEach { w -> fonts += Font("fonts/$italicFile", assets, FontWeight(w), FontStyle.Italic) }
            }
            FontFamily(fonts)
        } catch (e: Exception) {
            fallback
        }
    }
}

object T {
    fun display(size: Int, weight: Int = 600) =
        TextStyle(fontFamily = Fonts.display, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = (size * 1.2f).sp)

    fun body(size: Int, weight: Int = 400) =
        TextStyle(fontFamily = Fonts.body, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = (size * 1.45f).sp)

    fun serif(size: Int, weight: Int = 600, italic: Boolean = false) =
        TextStyle(
            fontFamily = Fonts.serif, fontWeight = FontWeight(weight), fontSize = size.sp,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal, lineHeight = (size * 1.15f).sp,
        )

    /** Handwriting, for little notes from the mascot. */
    fun hand(size: Int, weight: Int = 700) =
        TextStyle(fontFamily = Fonts.hand, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = (size * 1.2f).sp)

    fun mono(size: Int) = TextStyle(fontFamily = Fonts.mono, fontSize = size.sp, lineHeight = (size * 1.45f).sp)
}

@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.ink,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    strike: Boolean = false,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = color,
            textAlign = align ?: TextAlign.Unspecified,
            textDecoration = if (strike) TextDecoration.LineThrough else null,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun CozyTheme(theme: String, accent: Long, animations: Boolean, content: @Composable () -> Unit) {
    val palette = when (theme) {
        "paper" -> einkPalette()
        "meadow" -> meadowPalette()
        else -> cozyPalette(Color(accent))
    }
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalAnimate provides (animations && theme != "paper"),
        content = content,
    )
}
