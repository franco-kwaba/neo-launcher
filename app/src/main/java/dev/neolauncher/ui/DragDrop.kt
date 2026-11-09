package dev.neolauncher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neolauncher.data.AppEntry
import dev.neolauncher.data.AppSlot
import dev.neolauncher.ui.neu.neu
import dev.neolauncher.ui.theme.LocalNeu
import kotlin.math.roundToInt

/**
 * Estado do arrastar-e-soltar de apps entre gaveta, tela inicial e barra.
 * Posições em coordenadas da raiz (a tela inteira).
 */
@Stable
class DragState {
    var app by mutableStateOf<AppEntry?>(null)
        private set
    var source by mutableStateOf<AppSlot>(AppSlot.Drawer)
        private set
    var position by mutableStateOf(Offset.Zero)
        internal set

    /** Dedo sobre um ícone: evita que o toque longo da tela inicial abra Personalizar junto. */
    internal var touchingIcon = false

    private val targets = mutableMapOf<AppSlot, Rect>()

    /** Destino sob o dedo agora (null se não estiver arrastando ou se estiver fora de qualquer destino). */
    val hovered: AppSlot? by derivedStateOf {
        if (app == null) null else targets.entries.firstOrNull { it.value.contains(position) }?.key
    }

    internal fun start(app: AppEntry, source: AppSlot, at: Offset) {
        this.source = source
        this.position = at
        this.app = app
    }

    /** Termina o arrasto e devolve onde o app foi solto. */
    internal fun end(): AppSlot? {
        val target = hovered
        app = null
        return target
    }

    internal fun cancel() {
        app = null
    }

    internal fun register(slot: AppSlot, bounds: Rect) {
        targets[slot] = bounds
    }

    internal fun unregister(slot: AppSlot) {
        targets.remove(slot)
    }
}

/** Marca a área como um lugar onde dá para soltar um app. */
fun Modifier.dropTarget(drag: DragState, slot: AppSlot): Modifier = composed {
    DisposableEffect(drag, slot) {
        onDispose { drag.unregister(slot) }
    }
    onGloballyPositioned { drag.register(slot, it.boundsInRoot()) }
}

private enum class PressOutcome { Tap, Cancel }

/**
 * Gestos de um ícone de app:
 * - toque: abre;
 * - segurar e soltar sem mover: menu;
 * - segurar e arrastar: move o app ([DragState]).
 *
 * Mover o dedo antes do toque longo cancela, para deixar a rolagem e os deslizes da tela funcionarem.
 */
private fun Modifier.appGestures(
    app: AppEntry,
    slot: AppSlot,
    drag: DragState,
    interaction: MutableInteractionSource,
    onTap: () -> Unit,
    onMenu: () -> Unit,
    onDragStarted: () -> Unit,
    onDrop: (AppSlot?) -> Unit,
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val currentApp by rememberUpdatedState(app)
    val currentSlot by rememberUpdatedState(slot)
    val tap by rememberUpdatedState(onTap)
    val menu by rememberUpdatedState(onMenu)
    val dragStarted by rememberUpdatedState(onDragStarted)
    val drop by rememberUpdatedState(onDrop)

    fun toRoot(local: Offset): Offset = coords[0]?.takeIf { it.isAttached }?.localToRoot(local) ?: local

    onGloballyPositioned { coords[0] = it }
        .pointerInput(drag) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val press = PressInteraction.Press(down.position)
                interaction.tryEmit(press)
                drag.touchingIcon = true
                var dragging = false
                try {
                    val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        var result = PressOutcome.Cancel
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                if (!change.isConsumed) result = PressOutcome.Tap
                                break
                            }
                            if (change.isConsumed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                        }
                        result
                    }
                    interaction.tryEmit(PressInteraction.Release(press))
                    when (outcome) {
                        PressOutcome.Tap -> {
                            tap()
                            return@awaitEachGesture
                        }
                        PressOutcome.Cancel -> return@awaitEachGesture
                        null -> Unit // segurou: toque longo
                    }

                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) break
                        if (!dragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                            drag.start(currentApp, currentSlot, toRoot(change.position))
                            dragStarted()
                        }
                        if (dragging) drag.position = toRoot(change.position)
                        change.consume()
                    }
                    if (dragging) {
                        dragging = false
                        drop(drag.end())
                    } else {
                        menu()
                    }
                } finally {
                    drag.touchingIcon = false
                    // Gesto interrompido no meio do arrasto (ex.: o ícone saiu da tela)
                    if (dragging) drag.cancel()
                }
            }
        }
}

/** Ícone de app neumórfico que abre, mostra o menu e pode ser arrastado. */
@Composable
fun DraggableAppTile(
    app: AppEntry,
    slot: AppSlot,
    drag: DragState,
    onTap: () -> Unit,
    onMenu: () -> Unit,
    onDrop: (AppSlot?) -> Unit,
    modifier: Modifier = Modifier,
    onDragStarted: () -> Unit = {},
    size: Dp = 64.dp,
    cornerRadius: Dp = 20.dp,
    iconSize: Dp = 40.dp,
    elevation: Dp = 6.dp,
    showLabel: Boolean = true,
    highlight: Boolean = false,
) {
    val p = LocalNeu.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .neu(cornerRadius, elevation, pressed || highlight, lit = highlight)
                .appGestures(app, slot, drag, interaction, onTap, onMenu, onDragStarted, onDrop),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(app, iconSize)
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.label,
                fontSize = 12.sp,
                color = p.contentMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Cópia do ícone que acompanha o dedo durante o arrasto. */
@Composable
fun DragGhost(drag: DragState) {
    val app = drag.app ?: return
    Box(
        Modifier
            .offset {
                val half = 32.dp.roundToPx()
                IntOffset(drag.position.x.roundToInt() - half, drag.position.y.roundToInt() - half)
            }
            .size(64.dp)
            .graphicsLayer {
                scaleX = 1.12f
                scaleY = 1.12f
            }
            .neu(cornerRadius = 20.dp, elevation = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app, 40.dp)
    }
}
