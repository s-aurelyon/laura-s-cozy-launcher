package app.cozy.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.cozy.launcher.data.Pictures
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.themeId
import app.cozy.launcher.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A stored picture by name, loaded in the background. Null while loading or if there's none. */
@Composable
fun rememberPicture(name: String?): ImageBitmap? {
    if (name == null) return null
    val pic by produceState(Pictures.cached(name), name) {
        value = Pictures.cached(name) ?: withContext(Dispatchers.IO) { Pictures.load(name) }
    }
    return pic
}

private val greyscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** A picture filling its box, cropped to fit. Grey on the Paper theme, like the e-ink screen shows it. */
@Composable
fun PictureFill(pic: ImageBitmap, modifier: Modifier) {
    Image(
        pic, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop,
        colorFilter = if (LocalPalette.current.eink) greyscale else null,
    )
}

/** Her own background picture behind a whole screen, softened with the page colour so text stays readable. */
@Composable
fun BoxScope.PageBackdrop() {
    val s by Store.settings.collectAsState()
    val pic = rememberPicture(s.pictures[Pictures.page(s.themeId())]) ?: return
    PictureFill(pic, Modifier.matchParentSize())
    if (s.pictureVeil > 0f) Box(Modifier.matchParentSize().background(LocalPalette.current.bg.copy(alpha = s.pictureVeil)))
}
