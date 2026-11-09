package dev.neolauncher.ui.neu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neolauncher.ui.theme.LocalNeu
import dev.neolauncher.ui.theme.NeoTheme
import kotlin.math.roundToInt

@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.neu(cornerRadius, elevation, pressed),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * Botão que "afunda" enquanto está pressionado.
 * `active = true` deixa ele afundado de vez (estado ligado de um interruptor).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    active: Boolean = false,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .neu(cornerRadius, elevation, pressed || active, lit = active)
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Campo de texto afundado. */
@Composable
fun NeuSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val p = LocalNeu.current
    NeuSurface(
        modifier = modifier.height(52.dp),
        cornerRadius = 26.dp,
        elevation = 5.dp,
        pressed = true,
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = p.content, fontSize = 16.sp),
            cursorBrush = SolidColor(p.accent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = p.contentMuted, fontSize = 16.sp)
                    inner()
                }
            },
        )
    }
}

/** Barra de progresso: trilho afundado com preenchimento na cor de destaque. */
@Composable
fun NeuProgress(fraction: Float, modifier: Modifier = Modifier) {
    val p = LocalNeu.current
    NeuSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
        cornerRadius = 5.dp,
        elevation = 2.dp,
        pressed = true,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .neuGlow(cornerRadius = 5.dp, spread = 8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(p.accent),
        )
    }
}

/**
 * Slider: trilho afundado + botão saltado que se arrasta. Valor entre 0 e 1.
 * Com [trackBrush], o trilho mostra esse gradiente (ex.: arco-íris do seletor de cor) em vez do
 * preenchimento; [thumbColor] põe um ponto dessa cor no botão.
 */
@Composable
fun NeuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackBrush: Brush? = null,
    thumbColor: Color? = null,
) {
    val onChange by rememberUpdatedState(onValueChange)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val thumb = 30.dp

    Box(
        modifier = modifier
            .height(40.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            // Só reage a arrasto horizontal ou toque: deslizar na vertical rola a página sem mudar o valor
            .pointerInput(Unit) {
                fun fraction(x: Float) = (x / widthPx).coerceIn(0f, 1f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (start != null) {
                        onChange(fraction(start.position.x))
                        horizontalDrag(start.id) { change ->
                            onChange(fraction(change.position.x))
                            change.consume()
                        }
                    } else {
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (up != null && up.changedToUpIgnoreConsumed() && !up.isConsumed &&
                            (up.position - down.position).getDistance() < viewConfiguration.touchSlop
                        ) {
                            onChange(fraction(up.position.x))
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (trackBrush == null) {
            NeuProgress(fraction = value, modifier = Modifier.height(12.dp))
        } else {
            NeuSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                cornerRadius = 8.dp,
                elevation = 2.dp,
                pressed = true,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(trackBrush),
                )
            }
        }
        Box(
            Modifier
                .offset {
                    val thumbPx = thumb.roundToPx()
                    IntOffset(((widthPx - thumbPx) * value.coerceIn(0f, 1f)).roundToInt(), 0)
                }
                .size(thumb)
                .neu(cornerRadius = thumb / 2, elevation = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbColor != null) {
                Box(
                    Modifier
                        .size(14.dp)
                        .neuGlow(cornerRadius = 7.dp, spread = 6.dp, color = thumbColor)
                        .clip(CircleShape)
                        .background(thumbColor),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE4E9F0, widthDp = 360)
@Composable
private fun NeuComponentsPreview() {
    NeoTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            var text by remember { mutableStateOf("") }
            var level by remember { mutableFloatStateOf(0.6f) }
            NeuSearchField(text, { text = it }, "Buscar apps", Modifier.fillMaxWidth())
            NeuSlider(level, { level = it }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                NeuSurface(Modifier.size(72.dp)) { Text("up") }
                NeuSurface(Modifier.size(72.dp), pressed = true) { Text("down") }
                NeuButton(onClick = {}, modifier = Modifier.size(72.dp), cornerRadius = 36.dp) { Text("tap") }
            }
        }
    }
}
