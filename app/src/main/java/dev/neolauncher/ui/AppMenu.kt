package dev.neolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neolauncher.LauncherViewModel
import dev.neolauncher.data.AppEntry
import dev.neolauncher.data.AppSlot
import dev.neolauncher.data.HomeLayout
import dev.neolauncher.ui.neu.NeuButton
import dev.neolauncher.ui.neu.NeuSurface
import dev.neolauncher.ui.theme.LocalNeu

/** Menu do toque longo em um app: fixar na barra, reordenar, informações e desinstalar. */
@Composable
fun AppMenu(
    app: AppEntry,
    layout: HomeLayout,
    homeFull: Boolean,
    actions: LauncherActions,
    onDismiss: () -> Unit,
) {
    val p = LocalNeu.current
    val favorites = layout.dock
    val index = favorites.indexOf(app.key)
    val isFavorite = index >= 0
    val dockFull = favorites.size >= LauncherViewModel.MAX_FAVORITES
    val onHome = layout.slotOf(app.key) is AppSlot.Home

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        NeuSurface(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                // Toques dentro do cartão não fecham o menu
                .pointerInput(Unit) { detectTapGestures { } },
            cornerRadius = 30.dp,
            elevation = 10.dp,
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeuSurface(Modifier.size(56.dp), cornerRadius = 18.dp, elevation = 4.dp, pressed = true) {
                        AppIcon(app, 34.dp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        app.label,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = p.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                MenuButton(
                    icon = if (onHome) Icons.Rounded.Close else Icons.Rounded.Home,
                    label = when {
                        onHome -> "Remover da tela inicial"
                        homeFull -> "Tela inicial cheia"
                        else -> "Adicionar à tela inicial"
                    },
                    enabled = onHome || !homeFull,
                    highlight = !onHome,
                    onClick = {
                        if (onHome) actions.moveApp(app, AppSlot.Remove) else actions.addToHome(app)
                        onDismiss()
                    },
                )
                MenuButton(
                    icon = Icons.Rounded.PushPin,
                    label = when {
                        isFavorite -> "Remover da barra"
                        dockFull -> "Barra cheia (máx. ${LauncherViewModel.MAX_FAVORITES})"
                        else -> "Adicionar à barra"
                    },
                    enabled = isFavorite || !dockFull,
                    highlight = !isFavorite,
                    onClick = {
                        actions.toggleFavorite(app)
                        onDismiss()
                    },
                )
                if (isFavorite && favorites.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MenuButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            label = "Esquerda",
                            enabled = index > 0,
                            onClick = { actions.moveFavorite(app, -1) },
                            modifier = Modifier.weight(1f),
                        )
                        MenuButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            label = "Direita",
                            enabled = index < favorites.lastIndex,
                            onClick = { actions.moveFavorite(app, 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                MenuButton(
                    icon = Icons.Rounded.Info,
                    label = "Informações do app",
                    onClick = {
                        actions.openAppInfo(app)
                        onDismiss()
                    },
                )
                MenuButton(
                    icon = Icons.Rounded.Delete,
                    label = "Desinstalar",
                    onClick = {
                        actions.uninstall(app)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    highlight: Boolean = false,
) {
    val p = LocalNeu.current
    val color = if (highlight) p.accent else p.content
    NeuButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .height(50.dp)
            .alpha(if (enabled) 1f else 0.45f),
        cornerRadius = 25.dp,
        elevation = 5.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, fontSize = 15.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
