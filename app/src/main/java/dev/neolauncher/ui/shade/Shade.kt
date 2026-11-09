package dev.neolauncher.ui.shade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neolauncher.LauncherUiState
import dev.neolauncher.data.IconStyle
import dev.neolauncher.notifications.NotificationItem
import dev.neolauncher.ui.LauncherActions
import dev.neolauncher.ui.neu.NeuButton
import dev.neolauncher.ui.neu.NeuSlider
import dev.neolauncher.ui.neu.NeuSurface
import dev.neolauncher.ui.theme.LocalNeu
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Painel que desce da parte de cima da tela inicial: atalhos, brilho e notificações. */
@Composable
fun Shade(
    state: LauncherUiState,
    actions: LauncherActions,
    onClose: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
) {
    val p = LocalNeu.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .systemBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { SectionTitle("Atalhos") }
        item { QuickGrid(state, actions, onOpenLauncherSettings) }
        item { BrightnessRow(state, actions) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Notificações", Modifier.weight(1f))
                if (state.notifications.any { it.clearable }) {
                    NeuButton(
                        onClick = actions::dismissAllNotifications,
                        modifier = Modifier
                            .height(36.dp)
                            .width(96.dp),
                        cornerRadius = 18.dp,
                        elevation = 4.dp,
                    ) {
                        Text("Limpar", fontSize = 13.sp, color = p.content)
                    }
                }
            }
        }
        when {
            !state.notificationAccess -> item { AccessCard(onAllow = actions::requestNotificationAccess) }
            state.notifications.isEmpty() -> item {
                Text(
                    "Nenhuma notificação",
                    color = p.contentMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                )
            }
            else -> items(state.notifications, key = { it.key }) { item ->
                NotificationCard(
                    item = item,
                    onOpen = {
                        actions.openNotification(item)
                        onClose()
                    },
                    onDismiss = { actions.dismissNotification(item) },
                )
            }
        }

        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NeuButton(onClick = onClose, modifier = Modifier.size(52.dp), cornerRadius = 26.dp) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Fechar painel", tint = p.contentMuted)
                }
            }
        }
    }
}

private class QuickTile(
    val label: String,
    val icon: ImageVector,
    val active: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun QuickGrid(state: LauncherUiState, actions: LauncherActions, onOpenLauncherSettings: () -> Unit) {
    val q = state.quick
    val tiles = buildList {
        add(QuickTile("Internet", Icons.Rounded.Wifi, false, actions::openInternetPanel))
        add(QuickTile("Bluetooth", Icons.Rounded.Bluetooth, false, actions::openBluetooth))
        if (q.torchAvailable) {
            add(QuickTile("Lanterna", if (q.torchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, q.torchOn, actions::toggleTorch))
        }
        add(QuickTile("Não perturbe", Icons.Rounded.DoNotDisturbOn, q.dndOn, actions::toggleDnd))
        add(QuickTile("Girar tela", Icons.Rounded.ScreenRotation, q.autoRotate, actions::toggleAutoRotate))
        add(QuickTile("Volume", Icons.AutoMirrored.Rounded.VolumeUp, false, actions::openVolumePanel))
        add(QuickTile("Ícones mono", Icons.Rounded.Palette, state.settings.iconStyle == IconStyle.MONO, actions::toggleIconStyle))
        add(QuickTile("Personalizar", Icons.Rounded.Tune, false, onOpenLauncherSettings))
        add(QuickTile("Ajustes", Icons.Rounded.Settings, false, actions::openSettings))
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        tiles.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { QuickTileView(it) }
                repeat(4 - row.size) { Spacer(Modifier.width(72.dp)) }
            }
        }
    }
}

@Composable
private fun QuickTileView(tile: QuickTile) {
    val p = LocalNeu.current
    Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        NeuButton(
            onClick = tile.onClick,
            active = tile.active,
            modifier = Modifier.size(60.dp),
            cornerRadius = 20.dp,
        ) {
            Icon(tile.icon, contentDescription = tile.label, tint = if (tile.active) p.accent else p.content)
        }
        Spacer(Modifier.height(8.dp))
        Text(tile.label, fontSize = 11.sp, color = p.contentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BrightnessRow(state: LauncherUiState, actions: LauncherActions) {
    val p = LocalNeu.current
    if (!state.quick.canWriteSettings) {
        NeuButton(
            onClick = actions::requestWriteSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            cornerRadius = 24.dp,
        ) {
            Text("Permitir ajuste de brilho e rotação", fontSize = 14.sp, color = p.content)
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.BrightnessLow, contentDescription = null, tint = p.contentMuted)
        NeuSlider(
            value = state.quick.brightness,
            onValueChange = actions::setBrightness,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        Icon(Icons.Rounded.BrightnessHigh, contentDescription = null, tint = p.contentMuted)
    }
}

@Composable
private fun AccessCard(onAllow: () -> Unit) {
    val p = LocalNeu.current
    NeuSurface(Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevation = 6.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Para ver as notificações e controlar a música por aqui, o launcher precisa de acesso às notificações.",
                fontSize = 14.sp,
                color = p.content,
            )
            NeuButton(
                onClick = onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                cornerRadius = 22.dp,
                elevation = 4.dp,
            ) {
                Text("Permitir acesso", fontSize = 14.sp, color = p.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationItem, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalNeu.current
    NeuSurface(Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 6.dp, contentAlignment = Alignment.TopStart) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onOpen)
                .padding(16.dp),
        ) {
            NeuSurface(Modifier.size(40.dp), cornerRadius = 20.dp, elevation = 3.dp, pressed = true) {
                if (item.icon != null) {
                    Image(item.icon, contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(p.accent))
                } else {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${item.appLabel} · ${formatTime(item.postTime)}", fontSize = 12.sp, color = p.contentMuted, maxLines = 1)
                if (item.title.isNotBlank()) {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (item.text.isNotBlank()) {
                    Text(item.text, fontSize = 14.sp, color = p.content.copy(alpha = 0.8f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (item.clearable) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dispensar", tint = p.contentMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = LocalNeu.current.content, modifier = modifier)
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TimeFormat)
