package dev.neolauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neolauncher.data.AccentPresets
import dev.neolauncher.data.IconStyle
import dev.neolauncher.data.LauncherSettings
import dev.neolauncher.data.ThemeMode
import dev.neolauncher.ui.LauncherActions
import dev.neolauncher.ui.neu.NeuButton
import dev.neolauncher.ui.neu.NeuProgress
import dev.neolauncher.ui.neu.NeuSlider
import dev.neolauncher.ui.neu.NeuSurface
import dev.neolauncher.ui.neu.neuGlow
import dev.neolauncher.ui.theme.LocalLight
import dev.neolauncher.ui.theme.LocalNeu
import dev.neolauncher.ui.theme.LocalNeuStyle
import dev.neolauncher.ui.theme.textGlow
import kotlin.math.roundToInt

/** Tela "Personalizar": tudo é aplicado na hora e salvo automaticamente. */
@Composable
fun SettingsPage(settings: LauncherSettings, actions: LauncherActions, onClose: () -> Unit) {
    val p = LocalNeu.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .systemBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuButton(onClick = onClose, modifier = Modifier.size(48.dp), cornerRadius = 24.dp, elevation = 5.dp) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = p.content)
                }
                Spacer(Modifier.width(18.dp))
                Text("Personalizar", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = p.content)
            }
        }

        item { PreviewCard() }

        item {
            Setting("Tema") {
                Segmented(
                    options = listOf("Sistema", "Claro", "Escuro"),
                    selected = settings.themeMode.ordinal,
                    onSelect = { i -> actions.updateSettings { it.copy(themeMode = ThemeMode.entries[i]) } },
                )
            }
        }

        item {
            Setting("Cor de destaque") {
                Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    AccentPicker(
                        selected = settings.accent,
                        onSelect = { color -> actions.updateSettings { it.copy(accent = color) } },
                    )
                    CustomColorPicker(
                        color = settings.accent,
                        onChange = { color -> actions.updateSettings { it.copy(accent = color) } },
                    )
                }
            }
        }

        item {
            Text("Iluminescência", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = p.content)
        }

        item {
            RangeSetting(
                title = "Brilho",
                valueLabel = "${(settings.glow * 100).roundToInt()}%",
                value = settings.glow,
                range = 0f..1.5f,
                onChange = { v -> actions.updateSettings { it.copy(glow = v) } },
            )
        }

        item {
            RangeSetting(
                title = "Alcance do brilho",
                valueLabel = "${(settings.glowRadius * 100).roundToInt()}%",
                value = settings.glowRadius,
                range = 0.3f..2.5f,
                onChange = { v -> actions.updateSettings { it.copy(glowRadius = v) } },
            )
        }

        item {
            RangeSetting(
                title = "Luz colorida",
                valueLabel = "${(settings.lightTint * 100).roundToInt()}%",
                value = settings.lightTint,
                range = 0f..1f,
                onChange = { v -> actions.updateSettings { it.copy(lightTint = v) } },
            )
        }

        item {
            Setting("Ícones") {
                Segmented(
                    options = listOf("Monocromáticos", "Coloridos"),
                    selected = settings.iconStyle.ordinal,
                    onSelect = { i -> actions.updateSettings { it.copy(iconStyle = IconStyle.entries[i]) } },
                )
            }
        }

        item {
            Text("Sombras", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = p.content)
        }

        item {
            RangeSetting(
                title = "Intensidade da sombra",
                valueLabel = "${(settings.shadowIntensity * 100).roundToInt()}%",
                value = settings.shadowIntensity,
                range = 0f..2f,
                onChange = { v -> actions.updateSettings { it.copy(shadowIntensity = v) } },
            )
        }

        item {
            RangeSetting(
                title = "Desfoque",
                valueLabel = "${(settings.shadowBlur * 100).roundToInt()}%",
                value = settings.shadowBlur,
                range = 0.2f..2.5f,
                onChange = { v -> actions.updateSettings { it.copy(shadowBlur = v) } },
            )
        }

        item {
            RangeSetting(
                title = "Distância",
                valueLabel = "${(settings.shadowDistance * 100).roundToInt()}%",
                value = settings.shadowDistance,
                range = 0f..2.5f,
                onChange = { v -> actions.updateSettings { it.copy(shadowDistance = v) } },
            )
        }

        item {
            Setting("Fonte de luz") {
                Segmented(
                    options = listOf("Sensor", "Manual"),
                    selected = if (settings.dynamicLight) 0 else 1,
                    onSelect = { i -> actions.updateSettings { it.copy(dynamicLight = i == 0) } },
                )
            }
        }

        item {
            if (settings.dynamicLight) {
                LiveLightInfo()
            } else {
                RangeSetting(
                    title = "Direção da luz",
                    valueLabel = lightDirectionName(settings.lightAngle),
                    value = settings.lightAngle,
                    range = 0f..360f,
                    onChange = { v -> actions.updateSettings { it.copy(lightAngle = v) } },
                )
            }
        }

        item {
            NeuButton(
                onClick = actions::resetSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                cornerRadius = 26.dp,
            ) {
                Text("Restaurar padrão", fontSize = 15.sp, color = p.content)
            }
        }
    }
}

/** Amostra fixa no topo para ver o efeito enquanto mexe nos controles. */
@Composable
private fun PreviewCard() {
    val p = LocalNeu.current
    NeuSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        cornerRadius = 30.dp,
        elevation = 10.dp,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NeuSurface(Modifier.size(56.dp), cornerRadius = 18.dp) {}
                NeuSurface(Modifier.size(56.dp), cornerRadius = 18.dp, pressed = true) {}
                NeuButton(onClick = {}, active = true, modifier = Modifier.size(56.dp), cornerRadius = 28.dp) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = p.accent)
                }
                NeuButton(onClick = {}, modifier = Modifier.size(56.dp), cornerRadius = 28.dp) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = p.contentMuted)
                }
            }
            NeuProgress(fraction = 0.65f)
        }
    }
}

@Composable
private fun Setting(title: String, valueLabel: String? = null, content: @Composable () -> Unit) {
    val p = LocalNeu.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = p.content, modifier = Modifier.weight(1f))
            if (valueLabel != null) Text(valueLabel, fontSize = 14.sp, color = p.contentMuted)
        }
        content()
    }
}

@Composable
private fun RangeSetting(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    val span = range.endInclusive - range.start
    Setting(title, valueLabel) {
        NeuSlider(
            value = (value - range.start) / span,
            onValueChange = { onChange(range.start + it * span) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalNeu.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            NeuButton(
                onClick = { onSelect(i) },
                active = active,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                cornerRadius = 23.dp,
                elevation = 5.dp,
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) p.accent else p.content,
                    style = TextStyle(shadow = if (active) LocalNeuStyle.current.textGlow(p.accent, 0.6f) else null),
                )
            }
        }
    }
}

@Composable
private fun AccentPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AccentPresets.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { color ->
                    NeuButton(
                        onClick = { onSelect(color) },
                        active = color == selected,
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 24.dp,
                        elevation = 5.dp,
                    ) {
                        ColorDot(Color(color), if (color == selected) 20.dp else 24.dp)
                    }
                }
            }
        }
    }
}

/** Seletor livre: matiz (arco-íris), saturação e brilho da cor. */
@Composable
private fun CustomColorPicker(color: Int, onChange: (Int) -> Unit) {
    val p = LocalNeu.current
    // Guarda H/S/V localmente: sem isso, zerar a saturação faria a matiz voltar para o vermelho
    var hsv by remember { mutableStateOf(color.toHsv()) }
    LaunchedEffect(color) {
        if (hsv.toArgb() != color) hsv = color.toHsv()
    }
    fun update(new: FloatArray) {
        hsv = new
        onChange(new.toArgb())
    }
    val (h, s, v) = hsv
    val current = Color(color)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeuSurface(Modifier.size(44.dp), cornerRadius = 22.dp, elevation = 4.dp, pressed = true) {
                ColorDot(current, 22.dp)
            }
            Spacer(Modifier.width(14.dp))
            Text("Cor personalizada", fontSize = 14.sp, color = p.content, modifier = Modifier.weight(1f))
            Text("#%06X".format(color and 0xFFFFFF), fontSize = 13.sp, color = p.contentMuted)
        }
        ColorSlider(
            label = "Matiz",
            value = h / 360f,
            brush = Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }),
            thumb = Color.hsv(h, 1f, 1f),
            onChange = { update(floatArrayOf(it * 360f, s, v)) },
        )
        ColorSlider(
            label = "Saturação",
            value = s,
            brush = Brush.horizontalGradient(listOf(Color.hsv(h, 0f, v), Color.hsv(h, 1f, v))),
            thumb = current,
            onChange = { update(floatArrayOf(h, it, v)) },
        )
        // Brilho mínimo de 25%: abaixo disso a cor some no tema escuro
        ColorSlider(
            label = "Luminosidade",
            value = ((v - 0.25f) / 0.75f).coerceIn(0f, 1f),
            brush = Brush.horizontalGradient(listOf(Color.hsv(h, s, 0.25f), Color.hsv(h, s, 1f))),
            thumb = current,
            onChange = { update(floatArrayOf(h, s, 0.25f + it * 0.75f)) },
        )
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, brush: Brush, thumb: Color, onChange: (Float) -> Unit) {
    val p = LocalNeu.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = p.contentMuted, modifier = Modifier.width(96.dp))
        NeuSlider(
            value = value,
            onValueChange = onChange,
            trackBrush = brush,
            thumbColor = thumb,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColorDot(color: Color, size: Dp) {
    Box(
        Modifier
            .size(size)
            .neuGlow(cornerRadius = size / 2, spread = 6.dp, strength = 0.7f, color = color)
            .clip(CircleShape)
            .background(color),
    )
}

private fun Int.toHsv(): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(this, it) }

private fun FloatArray.toArgb(): Int = android.graphics.Color.HSVToColor(this)

/** No modo sensor: mostra para onde a luz está vindo agora (atualiza ao inclinar o celular). */
@Composable
private fun LiveLightInfo() {
    val p = LocalNeu.current
    val light = LocalLight.current
    val height = when {
        light.reach > 0.75f -> "luz de lado"
        light.reach > 0.35f -> "luz alta"
        else -> "luz a pino"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "A luz fica acima da sua cabeça e acompanha a inclinação do celular.",
            fontSize = 14.sp,
            color = p.content,
        )
        Text(
            "Agora: ${lightDirectionName(light.angle).lowercase()} · $height",
            fontSize = 13.sp,
            color = p.contentMuted,
        )
    }
}

private val LightDirections = listOf(
    "Topo", "Superior direita", "Direita", "Inferior direita",
    "Base", "Inferior esquerda", "Esquerda", "Superior esquerda",
)

private fun lightDirectionName(angle: Float): String =
    LightDirections[(((angle + 22.5f) / 45f).toInt()) % LightDirections.size]
