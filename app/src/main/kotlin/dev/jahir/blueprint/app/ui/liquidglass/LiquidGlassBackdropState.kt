package dev.jahir.blueprint.app.ui.liquidglass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

class LiquidGlassBackdropState {

    var image by mutableStateOf<ImageBitmap?>(null)
        private set
    var contentTopLeftInWindow by mutableStateOf(Offset.Zero)
        private set
    var fullWidth by mutableIntStateOf(0)
        private set
    var fullHeight by mutableIntStateOf(0)
        private set

    private var bitmap: Bitmap? = null
    private val location = IntArray(2)

    fun capture(view: View) {
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return

        val bitmapWidth = (width * SnapshotScale).toInt().coerceAtLeast(1)
        val bitmapHeight = (height * SnapshotScale).toInt().coerceAtLeast(1)
        val target = bitmap
            ?.takeIf { it.width == bitmapWidth && it.height == bitmapHeight }
            ?: Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                .also {
                    bitmap?.recycle()
                    bitmap = it
                }

        target.eraseColor(0)
        Canvas(target).run {
            save()
            scale(SnapshotScale, SnapshotScale)
            view.draw(this)
            restore()
        }

        view.getLocationInWindow(location)
        contentTopLeftInWindow = Offset(location[0].toFloat(), location[1].toFloat())
        fullWidth = width
        fullHeight = height
        image = target.asImageBitmap()
    }

    private companion object {
        const val SnapshotScale = 0.18f
    }
}
