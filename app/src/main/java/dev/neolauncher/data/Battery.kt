package dev.neolauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryState(val percent: Int, val charging: Boolean)

/** Nível e estado de carga, atualizados a cada mudança enviada pelo sistema. */
fun Context.batteryFlow(): Flow<BatteryState> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent.toBatteryState())
        }
    }
    val sticky = ContextCompat.registerReceiver(
        this@batteryFlow,
        receiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    sticky?.let { trySend(it.toBatteryState()) }
    awaitClose { unregisterReceiver(receiver) }
}

private fun Intent.toBatteryState(): BatteryState {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    return BatteryState(
        percent = level * 100 / scale,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
    )
}
