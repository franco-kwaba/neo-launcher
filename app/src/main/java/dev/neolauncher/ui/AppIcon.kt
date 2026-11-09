package dev.neolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import dev.neolauncher.data.AppEntry
import dev.neolauncher.ui.theme.LocalNeu

@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalNeu.current
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        // O glifo monocromático ocupa menos área útil que o ícone colorido; compensa no tamanho
        modifier = modifier.size(if (app.tintable) size * 1.3f else size),
        colorFilter = if (app.tintable) ColorFilter.tint(p.content) else null,
    )
}
