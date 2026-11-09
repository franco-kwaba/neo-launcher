package dev.neolauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap

/**
 * Padroniza os ícones dos apps:
 * - MONO: usa a camada monocromática (Android 13+), que a UI pinta com a cor do tema.
 * - COLOR (ou app sem camada monocromática): ícone adaptativo inteiro, recortado no
 *   mesmo cantinho arredondado dos blocos neumórficos, em vez do círculo do sistema.
 */
object IconRenderer {

    class Rendered(val bitmap: Bitmap, val tintable: Boolean)

    // Mesma proporção do canto dos blocos (20dp em 64dp)
    private const val CORNER_FRACTION = 0.3f

    fun render(drawable: Drawable, style: IconStyle, sizePx: Int): Rendered {
        if (drawable is AdaptiveIconDrawable) {
            if (style == IconStyle.MONO && Build.VERSION.SDK_INT >= 33) {
                drawable.monochrome?.let { return Rendered(drawLayers(sizePx, clip = false, it), tintable = true) }
            }
            return Rendered(drawLayers(sizePx, clip = true, drawable.background, drawable.foreground), tintable = false)
        }
        return Rendered(drawable.toBitmap(sizePx, sizePx), tintable = false)
    }

    private fun drawLayers(size: Int, clip: Boolean, vararg layers: Drawable?): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (clip) {
            val r = size * CORNER_FRACTION
            canvas.clipPath(Path().apply { addRoundRect(0f, 0f, size.toFloat(), size.toFloat(), r, r, Path.Direction.CW) })
        }
        // Camadas adaptativas têm 108dp com 72dp visíveis: sobram 18dp (1/4 de 72) de cada lado
        val inset = size / 4
        layers.filterNotNull().forEach { layer ->
            layer.setBounds(-inset, -inset, size + inset, size + inset)
            layer.draw(canvas)
        }
        return bitmap
    }
}
