package dev.neolauncher.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.neolauncher.data.LauncherSettings
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Fonte de luz das sombras. É estado lido só na fase de desenho, então mudar a luz
 * redesenha as superfícies sem recompor a tela.
 */
@Stable
class LightState(angle: Float = 315f) {
    /** De onde vem a luz, em graus no plano da tela: 0 = topo, sentido horário. */
    var angle by mutableFloatStateOf(angle)

    /**
     * Quão rasante a luz chega na tela (multiplica a distância das sombras):
     * 1 = luz de lado, sombras longas; perto de 0 = luz a pino, sombras curtas.
     */
    var reach by mutableFloatStateOf(1f)
}

val LocalLight = staticCompositionLocalOf { LightState() }

/** Luz manual (ângulo das configurações) ou guiada pela inclinação do celular. */
@Composable
fun rememberLight(settings: LauncherSettings): LightState {
    val light = remember { LightState(settings.lightAngle) }
    if (settings.dynamicLight && !LocalInspectionMode.current) {
        DeviceTiltLight(light)
    } else {
        SideEffect {
            light.angle = settings.lightAngle
            light.reach = 1f
        }
    }
    return light
}

/** Liga o sensor só enquanto o launcher está na tela (não gasta bateria com outros apps abertos). */
@Composable
private fun DeviceTiltLight(light: LightState) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, light) {
        val tracker = TiltLightTracker(context, light)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> tracker.start()
                Lifecycle.Event.ON_PAUSE -> tracker.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            tracker.stop()
        }
    }
}

/**
 * Modelo: a lâmpada está acima da cabeça do usuário, e o usuário olha a tela de frente
 * (ou pela borda de baixo, quando o celular está deitado na mesa).
 *
 * Luz (no referencial do aparelho) = olho * EYE_DISTANCE + cima * LIGHT_HEIGHT.
 * A projeção dela no plano da tela dá a direção; o tamanho da projeção, o quão rasante ela é.
 * - Em pé: "cima" está no plano da tela, apontando para o topo -> luz do topo, sombras longas.
 * - Deitado: "cima" sai da tela -> sobra só o olho, levemente para a borda de baixo -> luz quase
 *   a pino vinda de baixo, sombras curtas.
 */
private class TiltLightTracker(
    private val context: Context,
    private val light: LightState,
) : SensorEventListener {

    private val sensors = context.getSystemService(SensorManager::class.java)
    // Gravidade já vem filtrada; acelerômetro é o plano B (o filtro abaixo tira a maior parte do tremor)
    private val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val up = FloatArray(3)
    private var hasReading = false
    private var running = false

    fun start() {
        if (running || sensor == null) return
        hasReading = false
        running = sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        sensors?.unregisterListener(this)
        running = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        // Parado, o sensor mede +g apontando para cima (para longe do chão)
        val v = event.values
        if (!hasReading) {
            v.copyInto(up, 0, 0, 3)
            hasReading = true
        } else {
            for (i in 0..2) up[i] += SMOOTHING * (v[i] - up[i])
        }
        val norm = sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2])
        if (norm < 1f) return
        val uz = up[2] / norm
        // Tela virada para baixo: ninguém está vendo, mantém a última luz
        if (uz < FACE_DOWN) return
        val (ux, uy) = toScreen(up[0] / norm, up[1] / norm)

        // Quanto mais deitado, mais o usuário olha pela borda de baixo da tela
        val flat = uz.coerceIn(0f, 1f)
        val eyeY = -EYE_TILT * flat
        val eyeNorm = sqrt(eyeY * eyeY + 1f)
        val lx = ux * LIGHT_HEIGHT
        val ly = uy * LIGHT_HEIGHT + eyeY / eyeNorm * EYE_DISTANCE

        val angle = (Math.toDegrees(atan2(lx, ly).toDouble()).toFloat() + 360f) % 360f
        val reach = (hypot(lx, ly) / LIGHT_HEIGHT).coerceIn(MIN_REACH, 1f)
        // Ignora variações imperceptíveis para não redesenhar à toa
        if (angleDelta(angle, light.angle) > 0.5f || abs(reach - light.reach) > 0.01f) {
            light.angle = angle
            light.reach = reach
        }
    }

    /** Converte do referencial do aparelho para o da tela, caso a tela esteja girada. */
    private fun toScreen(x: Float, y: Float): Pair<Float, Float> = when (displayRotation()) {
        Surface.ROTATION_90 -> y to -x
        Surface.ROTATION_180 -> -x to -y
        Surface.ROTATION_270 -> -y to x
        else -> x to y
    }

    @Suppress("DEPRECATION")
    private fun displayRotation(): Int =
        if (Build.VERSION.SDK_INT >= 30) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            context.getSystemService(WindowManager::class.java)?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }

    private fun angleDelta(a: Float, b: Float): Float = abs(((a - b + 540f) % 360f) - 180f)

    private companion object {
        const val SMOOTHING = 0.12f
        const val LIGHT_HEIGHT = 1f
        const val EYE_DISTANCE = 0.35f
        const val EYE_TILT = 0.8f
        const val MIN_REACH = 0.15f
        const val FACE_DOWN = -0.35f
    }
}
