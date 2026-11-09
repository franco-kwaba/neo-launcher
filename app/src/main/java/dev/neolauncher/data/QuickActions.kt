package dev.neolauncher.data

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dev.neolauncher.notifications.NeoNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class QuickState(
    val torchAvailable: Boolean = false,
    val torchOn: Boolean = false,
    val dndOn: Boolean = false,
    val autoRotate: Boolean = false,
    val canWriteSettings: Boolean = false,
    val brightness: Float = 0.5f,
)

/**
 * Atalhos do painel. Lanterna, Não perturbe, rotação e brilho mudam direto;
 * Wi-Fi, Bluetooth e volume abrem o painel do sistema, porque apps comuns não
 * podem ligar/desligar essas funções desde o Android 10.
 */
class QuickActions(private val context: Context) {

    private val camera = context.getSystemService(CameraManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val resolver = context.contentResolver

    private val torchId: String? = runCatching {
        camera.cameraIdList.firstOrNull {
            camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchId) _torchOn.value = enabled
        }
    }

    init {
        camera.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
    }

    fun release() = camera.unregisterTorchCallback(torchCallback)

    fun snapshot() = QuickState(
        torchAvailable = torchId != null,
        torchOn = _torchOn.value,
        dndOn = notifications.currentInterruptionFilter.let {
            it != NotificationManager.INTERRUPTION_FILTER_ALL && it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        },
        autoRotate = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1,
        canWriteSettings = Settings.System.canWrite(context),
        brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f,
    )

    fun toggleTorch() {
        val id = torchId ?: return
        runCatching { camera.setTorchMode(id, !_torchOn.value) }
    }

    fun toggleDnd() {
        if (!notifications.isNotificationPolicyAccessGranted) {
            open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        val on = notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        notifications.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
    }

    fun toggleAutoRotate() {
        if (!Settings.System.canWrite(context)) return requestWriteSettings()
        val on = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 0 else 1)
    }

    fun setBrightness(value: Float) {
        if (!Settings.System.canWrite(context)) return
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).roundToInt().coerceIn(1, 255))
    }

    fun requestWriteSettings() =
        open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))

    fun openInternetPanel() = open(
        Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS),
    )

    fun openBluetooth() = open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

    fun openVolumePanel() = open(
        Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS),
    )

    fun openSettings() = open(Intent(Settings.ACTION_SETTINGS))

    fun openBatterySettings() = open(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))

    fun openNotificationAccess() {
        val intent = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, NeoNotificationListener::class.java).flattenToString(),
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        open(intent)
    }

    private fun open(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
