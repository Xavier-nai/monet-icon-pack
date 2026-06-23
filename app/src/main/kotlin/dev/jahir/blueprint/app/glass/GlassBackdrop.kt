package dev.jahir.blueprint.app.glass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Holds a live, down-scaled snapshot of whatever is drawn *behind* the liquid-glass
 * bar (the fragments container) plus where it sits in window coordinates, so the glass
 * can refract the real, scrolling UI in real time.
 *
 * The snapshot is rendered at [SCALE] of the real size — a blurred glass backdrop does
 * not need full resolution, and the down-scale keeps per-frame capture cheap enough to
 * run while the page scrolls without jank. The full (un-scaled) size is recorded so the
 * consumer can stretch the small bitmap back to 1:1 window space.
 */
class GlassBackdropState {

    var image by mutableStateOf<ImageBitmap?>(null)
        private set

    /** Top-left of the captured content in window coordinates. */
    var contentTopLeftInWindow by mutableStateOf(Offset.Zero)
        private set

    /** Real (un-scaled) size of the captured view, in px. */
    var fullWidth by mutableStateOf(0)
        private set
    var fullHeight by mutableStateOf(0)
        private set

    private var backing: Bitmap? = null
    private val location = IntArray(2)

    /** Renders [view] into the reusable down-scaled bitmap. Must run on the main thread. */
    fun capture(view: View) {
        val w = view.width
        val h = view.height
        if (w <= 0 || h <= 0) return
        val bw = (w * SCALE).toInt().coerceAtLeast(1)
        val bh = (h * SCALE).toInt().coerceAtLeast(1)
        var bmp = backing
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            backing = bmp
        }
        val target = bmp!!
        target.eraseColor(0)
        val canvas = Canvas(target)
        canvas.save()
        canvas.scale(SCALE, SCALE)
        view.draw(canvas)
        canvas.restore()
        view.getLocationInWindow(location)
        contentTopLeftInWindow = Offset(location[0].toFloat(), location[1].toFloat())
        fullWidth = w
        fullHeight = h
        image = target.asImageBitmap()
    }

    private companion object {
        const val SCALE = 0.34f
    }
}
