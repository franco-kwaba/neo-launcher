package dev.neolauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class Weather(val tempC: Int, val code: Int, val place: String?)

/** Clima atual pela API aberta do Open-Meteo (sem chave), usando a localização aproximada. */
class WeatherRepository(private val context: Context) {

    private val locationManager = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun fetch(): Weather? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        runCatching {
            val location = currentLocation() ?: return@runCatching null
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}&longitude=${location.longitude}" +
                    "&current=temperature_2m,weather_code&timezone=auto",
            )
            val body = (url.openConnection() as HttpURLConnection).run {
                connectTimeout = 8_000
                readTimeout = 8_000
                try {
                    inputStream.bufferedReader().readText()
                } finally {
                    disconnect()
                }
            }
            val current = JSONObject(body).getJSONObject("current")
            Weather(
                tempC = current.getDouble("temperature_2m").roundToInt(),
                code = current.getInt("weather_code"),
                place = placeName(location),
            )
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val last = locationManager.getProviders(true)
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
        if (last != null || Build.VERSION.SDK_INT < 30) return last

        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { locationManager.isProviderEnabled(it) } ?: return null
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                locationManager.getCurrentLocation(provider, null, context.mainExecutor) { cont.resume(it) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun placeName(location: Location): String? = runCatching {
        Geocoder(context, Locale.forLanguageTag("pt-BR"))
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()
}
