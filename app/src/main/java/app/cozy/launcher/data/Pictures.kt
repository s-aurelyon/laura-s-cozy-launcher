package app.cozy.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.InputStream
import kotlin.math.max

/**
 * Her own pictures: a background behind every screen, a photo in place of the painted meadow,
 * and one on the home screen's mascot card. A picked picture is copied into the app's private
 * storage (scaled to the screen), so it stays even if the original leaves the gallery.
 */
object Pictures {
    /** Behind every screen of a theme. */
    fun page(theme: String) = "$theme.page"

    /** On the mascot card of the Cozy and Paper home screens. */
    fun card(theme: String) = "$theme.card"

    /** In place of the painted sky and meadow. */
    const val MEADOW_SCENE = "meadow.scene"

    private val cache = LruCache<String, ImageBitmap>(4)

    private fun dir() = File(Store.app.filesDir, "cozy/pictures").apply { mkdirs() }

    fun cached(name: String): ImageBitmap? = cache.get(name)

    /** Reads a stored picture. Call off the main thread. */
    fun load(name: String): ImageBitmap? {
        cache.get(name)?.let { return it }
        val bmp = try {
            BitmapFactory.decodeFile(File(dir(), name).path)
        } catch (e: OutOfMemoryError) {
            null
        } ?: return null
        return bmp.asImageBitmap().also { cache.put(name, it) }
    }

    /**
     * Copies a picked picture in, no bigger than [maxSide] pixels on its long side and turned
     * the right way up. Returns its stored name, or null if it couldn't be read. Call off the main thread.
     */
    fun import(ctx: Context, uri: Uri, slot: String, maxSide: Int): String? {
        return try {
            val resolver = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

            val longest = max(bmp.width, bmp.height)
            if (longest > maxSide) {
                val k = maxSide.toFloat() / longest
                bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true)
            }
            val degrees = resolver.openInputStream(uri)?.use { rotationOf(it) } ?: 0
            if (degrees != 0) {
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
            }

            val png = bmp.hasAlpha()
            val name = "$slot-${System.currentTimeMillis()}." + if (png) "png" else "jpg"
            File(dir(), name).outputStream().use {
                bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 90, it)
            }
            cache.put(name, bmp.asImageBitmap())
            name
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Camera photos are often stored sideways, with a note saying which way is up. */
    private fun rotationOf(input: InputStream): Int = try {
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    /** Puts [name] in [slot] (null clears it), and tidies away the picture it replaces. */
    fun set(slot: String, name: String?) {
        val old = Store.settings.value.pictures[slot]
        Store.updateSettings { s -> s.copy(pictures = if (name == null) s.pictures - slot else s.pictures + (slot to name)) }
        if (old != null && old != name) {
            cache.remove(old)
            File(dir(), old).delete()
        }
    }
}
