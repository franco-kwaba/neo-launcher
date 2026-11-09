package dev.neolauncher.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit

enum class IconStyle { MONO, COLOR }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Immutable
data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Int = AccentPresets.first(),
    val iconStyle: IconStyle = IconStyle.MONO,
    /** 0 = sem sombra, 1 = padrão, 2 = forte. */
    val shadowIntensity: Float = 1f,
    /** Multiplicador do desfoque das sombras. */
    val shadowBlur: Float = 1f,
    /** Multiplicador da distância entre a superfície e a sombra. */
    val shadowDistance: Float = 1f,
    /** De onde vem a luz, em graus: 0 = topo, sentido horário (315 = superior esquerda). Só no modo manual. */
    val lightAngle: Float = 315f,
    /** Luz guiada pela inclinação do celular (sensor de gravidade), sempre acima da cabeça do usuário. */
    val dynamicLight: Boolean = true,
    /** Iluminescência: quanto os elementos ativos emitem luz na cor de destaque (0 = nada). */
    val glow: Float = 0.6f,
    /** Até onde o brilho se espalha (multiplicador). */
    val glowRadius: Float = 1f,
    /** Quanto o lado iluminado das sombras ganha a cor de destaque (0 = luz branca). */
    val lightTint: Float = 0.25f,
)

val AccentPresets: List<Int> = listOf(
    0xFF5B7CFA, // azul
    0xFF00C2E0, // ciano
    0xFF2BAAB0, // turquesa
    0xFF2ED3A0, // menta
    0xFF3DAE7A, // verde
    0xFF9BD13B, // lima
    0xFFF2C230, // amarelo
    0xFFF08A3C, // laranja
    0xFFFF6B5B, // coral
    0xFFE5534B, // vermelho
    0xFFE35D9B, // rosa
    0xFFD63CE0, // magenta
    0xFFA35BE8, // roxo
    0xFF7C5CFA, // índigo
    0xFF5C6675, // grafite
).map { it.toInt() }

class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun load(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            themeMode = enumOr(KEY_THEME, d.themeMode),
            accent = prefs.getInt(KEY_ACCENT, d.accent),
            iconStyle = enumOr(KEY_ICON_STYLE, d.iconStyle),
            shadowIntensity = prefs.getFloat(KEY_INTENSITY, d.shadowIntensity),
            shadowBlur = prefs.getFloat(KEY_BLUR, d.shadowBlur),
            shadowDistance = prefs.getFloat(KEY_DISTANCE, d.shadowDistance),
            lightAngle = prefs.getFloat(KEY_LIGHT_ANGLE, d.lightAngle),
            dynamicLight = prefs.getBoolean(KEY_DYNAMIC_LIGHT, d.dynamicLight),
            glow = prefs.getFloat(KEY_GLOW, d.glow),
            glowRadius = prefs.getFloat(KEY_GLOW_RADIUS, d.glowRadius),
            lightTint = prefs.getFloat(KEY_LIGHT_TINT, d.lightTint),
        )
    }

    fun save(s: LauncherSettings) = prefs.edit {
        putString(KEY_THEME, s.themeMode.name)
        putInt(KEY_ACCENT, s.accent)
        putString(KEY_ICON_STYLE, s.iconStyle.name)
        putFloat(KEY_INTENSITY, s.shadowIntensity)
        putFloat(KEY_BLUR, s.shadowBlur)
        putFloat(KEY_DISTANCE, s.shadowDistance)
        putFloat(KEY_LIGHT_ANGLE, s.lightAngle)
        putBoolean(KEY_DYNAMIC_LIGHT, s.dynamicLight)
        putFloat(KEY_GLOW, s.glow)
        putFloat(KEY_GLOW_RADIUS, s.glowRadius)
        putFloat(KEY_LIGHT_TINT, s.lightTint)
    }

    /** Apps na barra de atalhos e na grade da tela inicial ([AppEntry.key]). */
    var layout: HomeLayout
        get() = HomeLayout(
            dock = prefs.getString(KEY_FAVORITES, null)?.lines()?.filter { it.isNotBlank() }.orEmpty(),
            home = prefs.getString(KEY_HOME, null)?.lines().orEmpty()
                .mapNotNull { line ->
                    val (cell, key) = line.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                    cell.toIntOrNull()?.let { it to key }
                }
                .toMap(),
        )
        set(value) = prefs.edit {
            putString(KEY_FAVORITES, value.dock.joinToString("\n"))
            putString(KEY_HOME, value.home.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }

    private inline fun <reified T : Enum<T>> enumOr(key: String, default: T): T =
        runCatching { enumValueOf<T>(prefs.getString(key, null).orEmpty()) }.getOrDefault(default)

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_ACCENT = "accent"
        const val KEY_ICON_STYLE = "icon_style"
        const val KEY_INTENSITY = "shadow_intensity"
        const val KEY_BLUR = "shadow_blur"
        const val KEY_DISTANCE = "shadow_distance"
        const val KEY_LIGHT_ANGLE = "light_angle"
        const val KEY_DYNAMIC_LIGHT = "dynamic_light"
        const val KEY_GLOW = "glow"
        const val KEY_GLOW_RADIUS = "glow_radius"
        const val KEY_LIGHT_TINT = "light_tint"
        const val KEY_FAVORITES = "favorites"
        const val KEY_HOME = "home_apps"
    }
}
