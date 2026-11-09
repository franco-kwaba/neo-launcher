package dev.neolauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import dev.neolauncher.data.LauncherSettings
import dev.neolauncher.data.ThemeMode

/**
 * Paleta neumórfica: uma única cor de fundo + uma sombra clara (lado da luz)
 * e uma sombra escura (lado oposto).
 */
@Immutable
data class NeuPalette(
    val background: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val content: Color,
    val contentMuted: Color,
    val accent: Color,
)

val LightNeu = NeuPalette(
    background = Color(0xFFE4E9F0),
    lightShadow = Color(0xFFFFFFFF),
    darkShadow = Color(0xFFA6B4C8),
    content = Color(0xFF39424E),
    contentMuted = Color(0xFF7D8898),
    accent = Color(0xFF5B7CFA),
)

val DarkNeu = NeuPalette(
    background = Color(0xFF2B2F36),
    lightShadow = Color(0xFF383D46),
    darkShadow = Color(0xFF1B1E22),
    content = Color(0xFFE1E5EC),
    contentMuted = Color(0xFF8A93A3),
    accent = Color(0xFF7C9BFF),
)

/** Parâmetros do efeito, ajustáveis na tela Personalizar. */
@Immutable
data class NeuStyle(
    val intensity: Float = 1f,
    val blur: Float = 1f,
    val distance: Float = 1f,
    val glow: Float = 0.6f,
    val glowRadius: Float = 1f,
    val lightTint: Float = 0.25f,
)

// Com "luz colorida", o lado iluminado ganha a cor de destaque
fun NeuStyle.lightShadow(p: NeuPalette): Color =
    lerp(shadowColor(p.background, p.lightShadow, Color.White, intensity), p.accent, (lightTint * 0.5f).coerceIn(0f, 1f))

/** Sombra de texto que faz o texto "acender" na cor dada; null se a iluminescência estiver desligada. */
fun NeuStyle.textGlow(color: Color, strength: Float = 1f): Shadow? {
    val amount = (glow * strength).coerceIn(0f, 1f)
    if (amount <= 0f) return null
    return Shadow(color = color.copy(alpha = amount), blurRadius = 28f * glowRadius * strength)
}

fun NeuStyle.darkShadow(p: NeuPalette): Color = shadowColor(p.background, p.darkShadow, Color.Black, intensity)

// Até 1: do fundo (sem sombra) até a cor da paleta; acima de 1: da paleta em direção ao extremo
private fun shadowColor(background: Color, base: Color, extreme: Color, t: Float): Color =
    if (t <= 1f) lerp(background, base, t.coerceAtLeast(0f)) else lerp(base, extreme, ((t - 1f) * 0.5f).coerceAtMost(1f))

val LocalNeu = staticCompositionLocalOf { LightNeu }
val LocalNeuStyle = staticCompositionLocalOf { NeuStyle() }

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun NeoTheme(
    settings: LauncherSettings = LauncherSettings(),
    darkTheme: Boolean = isDarkTheme(settings.themeMode),
    content: @Composable () -> Unit,
) {
    val palette = (if (darkTheme) DarkNeu else LightNeu).copy(accent = Color(settings.accent))
    val style = NeuStyle(
        intensity = settings.shadowIntensity,
        blur = settings.shadowBlur,
        distance = settings.shadowDistance,
        glow = settings.glow,
        glowRadius = settings.glowRadius,
        lightTint = settings.lightTint,
    )
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.background,
            onBackground = palette.content,
            onSurface = palette.content,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.background,
            onBackground = palette.content,
            onSurface = palette.content,
        )
    }
    val light = rememberLight(settings)
    CompositionLocalProvider(LocalNeu provides palette, LocalNeuStyle provides style, LocalLight provides light) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
