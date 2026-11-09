package dev.neolauncher.ui.neu

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.neolauncher.ui.theme.LocalLight
import dev.neolauncher.ui.theme.LocalNeu
import dev.neolauncher.ui.theme.LocalNeuStyle
import dev.neolauncher.ui.theme.darkShadow
import dev.neolauncher.ui.theme.lightShadow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Desenha o efeito neumórfico atrás do conteúdo.
 *
 * - `pressed = false`: superfície "saltada" (sombra clara do lado da luz, escura do lado oposto).
 * - `pressed = true`: superfície "afundada" (sombras internas, invertidas).
 * - `lit = true`: além disso, a superfície emite luz na cor de destaque (halo por fora e borda acesa
 *   por dentro), conforme a iluminescência configurada.
 *
 * Intensidade, desfoque, distância, direção da luz e brilho vêm de [LocalNeuStyle].
 * As sombras vazam para fora dos limites do composable; dê espaço (padding/contentPadding)
 * ao redor e não aplique `clip` antes deste modifier.
 */
fun Modifier.neu(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    lit: Boolean = false,
): Modifier = composed {
    val palette = LocalNeu.current
    val style = LocalNeuStyle.current
    val light = LocalLight.current
    // A luz é lida aqui dentro: quando ela muda (sensor), só o desenho é refeito
    drawWithCache {
        val w = size.width
        val h = size.height
        val r = cornerRadius.toPx()
        val e = elevation.toPx()
        // setShadowLayer com raio 0 remove a sombra: mantém um mínimo
        val blur = (e * style.blur).coerceAtLeast(0.5f)
        // No padrão (distância 1, luz rasante) o deslocamento é e/2 em cada eixo
        val distance = e / 2 * sqrt(2f) * style.distance * light.reach
        val angle = Math.toRadians(light.angle.toDouble())
        val dx = (sin(angle) * distance).toFloat()
        val dy = (-cos(angle) * distance).toFloat()
        val bg = palette.background.toArgb()
        val light = style.lightShadow(palette).toArgb()
        val dark = style.darkShadow(palette).toArgb()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        val shape = Path().apply { addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW) }
        // Moldura em volta da forma: a sombra dela, recortada pela forma, vira sombra interna
        val pad = (blur + distance) * 3
        val frame = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(-pad, -pad, w + pad, h + pad, Path.Direction.CW)
            addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)
        }

        val glowing = lit && style.glow > 0f
        val glowRadius = (e * 1.8f * style.glowRadius).coerceAtLeast(1f)
        val halo = glowPaint(palette.accent, alpha = 0.55f * style.glow, radius = glowRadius)
        val rim = glowPaint(palette.accent, alpha = 0.7f * style.glow, radius = (glowRadius * 0.6f).coerceAtLeast(1f)).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = (e * 0.9f * style.glowRadius).coerceAtLeast(1f)
        }

        onDrawBehind {
            drawIntoCanvas { c ->
                val canvas = c.nativeCanvas
                if (glowing) canvas.drawPath(shape, halo)
                if (!pressed) {
                    paint.setShadowLayer(blur, dx, dy, light)
                    canvas.drawPath(shape, paint)
                    paint.setShadowLayer(blur, -dx, -dy, dark)
                    canvas.drawPath(shape, paint)
                } else {
                    paint.clearShadowLayer()
                    canvas.drawPath(shape, paint)
                    canvas.save()
                    canvas.clipPath(shape)
                    paint.setShadowLayer(blur, -dx, -dy, dark)
                    canvas.drawPath(frame, paint)
                    paint.setShadowLayer(blur, dx, dy, light)
                    canvas.drawPath(frame, paint)
                    canvas.restore()
                }
                if (glowing) {
                    canvas.save()
                    canvas.clipPath(shape)
                    canvas.drawPath(shape, rim)
                    canvas.restore()
                }
            }
        }
    }
}

/**
 * Halo de luz atrás do conteúdo (barras de progresso, pontos de cor etc.).
 * Usa a cor de destaque, ou [color] quando informado. Não aplique `clip` antes.
 */
fun Modifier.neuGlow(
    cornerRadius: Dp,
    spread: Dp = 10.dp,
    strength: Float = 1f,
    color: Color? = null,
): Modifier = composed {
    val palette = LocalNeu.current
    val style = LocalNeuStyle.current
    drawWithCache {
        val amount = style.glow * strength
        val r = cornerRadius.toPx()
        val paint = glowPaint(
            color ?: palette.accent,
            alpha = 0.6f * amount,
            radius = (spread.toPx() * style.glowRadius).coerceAtLeast(1f),
        )
        onDrawBehind {
            if (amount > 0f) {
                drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint) }
            }
        }
    }
}

private fun glowPaint(color: Color, alpha: Float, radius: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
    maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
}
