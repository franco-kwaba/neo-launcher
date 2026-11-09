package dev.neolauncher.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neolauncher.data.BatteryState
import dev.neolauncher.data.MediaInfo
import dev.neolauncher.data.Weather
import dev.neolauncher.ui.neu.NeuButton
import dev.neolauncher.ui.neu.NeuProgress
import dev.neolauncher.ui.neu.NeuSurface
import dev.neolauncher.ui.theme.LocalNeu

@Composable
fun BatteryWidget(battery: BatteryState?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalNeu.current
    val charging = battery?.charging == true
    NeuButton(onClick = onClick, modifier = modifier, cornerRadius = 26.dp, elevation = 8.dp) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WidgetHeader(
                icon = if (charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                label = if (charging) "Carregando" else "Bateria",
                highlight = charging,
            )
            Text(
                text = "${battery?.percent ?: "--"}%",
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                color = p.content,
            )
            NeuProgress(fraction = (battery?.percent ?: 0) / 100f)
        }
    }
}

@Composable
fun WeatherWidget(weather: Weather?, needsLocation: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalNeu.current
    val (description, icon) = weather?.let { describeWeather(it.code) } ?: ("Clima" to Icons.Rounded.WbSunny)
    NeuButton(onClick = onClick, modifier = modifier, cornerRadius = 26.dp, elevation = 8.dp) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WidgetHeader(icon = if (needsLocation) Icons.Rounded.LocationOn else icon, label = weather?.place ?: "Clima")
            Text(
                text = weather?.let { "${it.tempC}°" } ?: "--°",
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                color = p.content,
            )
            Text(
                text = when {
                    needsLocation -> "Toque para usar a localização"
                    weather == null -> "Carregando…"
                    else -> description
                },
                fontSize = 12.sp,
                color = p.contentMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MediaWidget(
    media: MediaInfo,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNeu.current
    NeuSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        cornerRadius = 28.dp,
        elevation = 8.dp,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeuSurface(Modifier.size(56.dp), cornerRadius = 18.dp, elevation = 3.dp, pressed = true) {
                if (media.art != null) {
                    Image(
                        bitmap = media.art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = p.contentMuted)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(media.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = p.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (media.artist.isNotBlank()) {
                    Text(media.artist, fontSize = 13.sp, color = p.contentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            MediaButton(Icons.Rounded.SkipPrevious, "Anterior", onPrevious)
            MediaButton(
                icon = if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                description = if (media.playing) "Pausar" else "Tocar",
                onClick = onPlayPause,
                active = media.playing,
            )
            MediaButton(Icons.Rounded.SkipNext, "Próxima", onNext)
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, description: String, onClick: () -> Unit, active: Boolean = false) {
    val p = LocalNeu.current
    NeuButton(
        onClick = onClick,
        active = active,
        modifier = Modifier
            .padding(start = 8.dp)
            .size(40.dp),
        cornerRadius = 20.dp,
        elevation = 4.dp,
    ) {
        Icon(icon, contentDescription = description, tint = if (active) p.accent else p.content, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun WidgetHeader(icon: ImageVector, label: String, highlight: Boolean = false) {
    val p = LocalNeu.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (highlight) p.accent else p.contentMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, color = p.contentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Códigos WMO usados pelo Open-Meteo. */
private fun describeWeather(code: Int): Pair<String, ImageVector> = when (code) {
    0 -> "Céu limpo" to Icons.Rounded.WbSunny
    1, 2 -> "Parcialmente nublado" to Icons.Rounded.WbCloudy
    3 -> "Nublado" to Icons.Rounded.Cloud
    45, 48 -> "Neblina" to Icons.Rounded.Cloud
    in 51..57 -> "Garoa" to Icons.Rounded.Umbrella
    in 61..67, in 80..82 -> "Chuva" to Icons.Rounded.Umbrella
    in 71..77, 85, 86 -> "Neve" to Icons.Rounded.AcUnit
    in 95..99 -> "Tempestade" to Icons.Rounded.FlashOn
    else -> "Clima" to Icons.Rounded.WbCloudy
}
