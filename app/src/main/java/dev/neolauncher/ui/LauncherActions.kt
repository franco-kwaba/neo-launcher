package dev.neolauncher.ui

import dev.neolauncher.data.AppEntry
import dev.neolauncher.data.AppSlot
import dev.neolauncher.data.LauncherSettings
import dev.neolauncher.notifications.NotificationItem

/** Tudo que a UI pode pedir. Implementações vazias por padrão, para os @Preview. */
interface LauncherActions {
    fun launch(app: AppEntry) {}
    fun openAppInfo(app: AppEntry) {}
    fun uninstall(app: AppEntry) {}
    fun toggleFavorite(app: AppEntry) {}
    fun moveFavorite(app: AppEntry, delta: Int) {}
    fun moveApp(app: AppEntry, to: AppSlot) {}
    fun addToHome(app: AppEntry) {}
    fun setHomeCellCount(count: Int) {}

    fun refreshWeather() {}
    fun openBatterySettings() {}
    fun mediaPlayPause() {}
    fun mediaNext() {}
    fun mediaPrevious() {}

    fun refreshQuickState() {}
    fun toggleTorch() {}
    fun toggleDnd() {}
    fun toggleAutoRotate() {}
    fun setBrightness(value: Float) {}
    fun requestWriteSettings() {}
    fun openInternetPanel() {}
    fun openBluetooth() {}
    fun openVolumePanel() {}
    fun openSettings() {}
    fun toggleIconStyle() {}

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {}
    fun resetSettings() {}

    fun requestNotificationAccess() {}
    fun openNotification(item: NotificationItem) {}
    fun dismissNotification(item: NotificationItem) {}
    fun dismissAllNotifications() {}
}
