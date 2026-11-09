package dev.neolauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.neolauncher.data.AppEntry
import dev.neolauncher.data.AppRepository
import dev.neolauncher.data.AppSlot
import dev.neolauncher.data.HomeLayout
import dev.neolauncher.data.BatteryState
import dev.neolauncher.data.IconStyle
import dev.neolauncher.data.LauncherPrefs
import dev.neolauncher.data.LauncherSettings
import dev.neolauncher.data.MediaInfo
import dev.neolauncher.data.MediaRepository
import dev.neolauncher.data.QuickActions
import dev.neolauncher.data.QuickState
import dev.neolauncher.data.Weather
import dev.neolauncher.data.WeatherRepository
import dev.neolauncher.data.batteryFlow
import dev.neolauncher.notifications.NotificationItem
import dev.neolauncher.notifications.NotificationStore
import dev.neolauncher.ui.LauncherActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LauncherUiState(
    val apps: List<AppEntry> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    /** Apps na barra de atalhos e na grade da tela inicial. */
    val layout: HomeLayout = HomeLayout(),
    /** Quantas células cabem na grade da tela inicial (depende da altura da tela). */
    val homeCellCount: Int = 0,
    val battery: BatteryState? = null,
    val weather: Weather? = null,
    val needsLocation: Boolean = false,
    val media: MediaInfo? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val notificationAccess: Boolean = false,
    val quick: QuickState = QuickState(),
    /** Muda a cada toque no botão Home com o launcher aberto: fecha gaveta e painel. */
    val homeTick: Int = 0,
)

class LauncherViewModel(private val app: Application) : AndroidViewModel(app), LauncherActions {

    private val apps = AppRepository(app)
    private val prefs = LauncherPrefs(app)
    private val quick = QuickActions(app)
    private val weatherRepo = WeatherRepository(app)
    private val media = MediaRepository(app)
    private var lastWeatherFetch = 0L

    private val _ui = MutableStateFlow(LauncherUiState(settings = prefs.load(), layout = prefs.layout))
    val ui: StateFlow<LauncherUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { app.batteryFlow().collect { b -> _ui.update { it.copy(battery = b) } } }
        viewModelScope.launch { NotificationStore.items.collect { n -> _ui.update { it.copy(notifications = n) } } }
        viewModelScope.launch { media.state.collect { m -> _ui.update { it.copy(media = m) } } }
        viewModelScope.launch { quick.torchOn.collect { refreshQuickState() } }
    }

    // TODO: trocar por BroadcastReceiver de PACKAGE_ADDED/REMOVED em vez de recarregar a cada onResume
    fun onResume() {
        loadApps()
        refreshQuickState()
        val access = NotificationStore.isAccessGranted(app)
        _ui.update { it.copy(notificationAccess = access) }
        if (access) media.start()
        if (System.currentTimeMillis() - lastWeatherFetch > WEATHER_TTL_MS) refreshWeather()
    }

    fun onHomePressed() = _ui.update { it.copy(homeTick = it.homeTick + 1) }

    private fun loadApps() {
        viewModelScope.launch {
            val list = apps.loadApps(_ui.value.settings.iconStyle)
            _ui.update { it.copy(apps = list) }
        }
    }

    override fun launch(app: AppEntry) = apps.launch(app)

    override fun openAppInfo(app: AppEntry) = apps.openAppInfo(app)

    override fun uninstall(app: AppEntry) = apps.uninstall(app)

    override fun toggleFavorite(app: AppEntry) = updateLayout { layout ->
        when {
            layout.slotOf(app.key) is AppSlot.Dock -> layout.move(app.key, AppSlot.Remove, homeCells)
            layout.dock.size < MAX_FAVORITES -> layout.move(app.key, AppSlot.Dock(layout.dock.size), homeCells)
            else -> layout
        }
    }

    override fun moveFavorite(app: AppEntry, delta: Int) = updateLayout { layout ->
        val from = layout.dock.indexOf(app.key)
        val to = from + delta
        if (from < 0 || to !in layout.dock.indices) {
            layout
        } else {
            layout.copy(dock = layout.dock.toMutableList().apply { add(to, removeAt(from)) })
        }
    }

    override fun moveApp(app: AppEntry, to: AppSlot) = updateLayout { it.move(app.key, to, homeCells) }

    override fun addToHome(app: AppEntry) = updateLayout { layout ->
        layout.firstFreeCell(homeCells)?.let { layout.move(app.key, AppSlot.Home(it), homeCells) } ?: layout
    }

    override fun setHomeCellCount(count: Int) = _ui.update { it.copy(homeCellCount = count) }

    private val homeCells get() = _ui.value.homeCellCount

    private fun updateLayout(transform: (HomeLayout) -> HomeLayout) {
        val new = transform(_ui.value.layout)
        if (new == _ui.value.layout) return
        prefs.layout = new
        _ui.update { it.copy(layout = new) }
    }

    override fun refreshWeather() {
        val granted = weatherRepo.hasPermission()
        _ui.update { it.copy(needsLocation = !granted) }
        if (!granted) return
        viewModelScope.launch {
            val weather = weatherRepo.fetch() ?: return@launch
            lastWeatherFetch = System.currentTimeMillis()
            _ui.update { it.copy(weather = weather) }
        }
    }

    override fun openBatterySettings() = quick.openBatterySettings()

    override fun mediaPlayPause() = media.playPause()

    override fun mediaNext() = media.next()

    override fun mediaPrevious() = media.previous()

    override fun refreshQuickState() = _ui.update { it.copy(quick = quick.snapshot()) }

    override fun toggleTorch() = quick.toggleTorch()

    override fun toggleDnd() {
        quick.toggleDnd()
        refreshQuickState()
    }

    override fun toggleAutoRotate() {
        quick.toggleAutoRotate()
        refreshQuickState()
    }

    override fun setBrightness(value: Float) {
        quick.setBrightness(value)
        _ui.update { it.copy(quick = it.quick.copy(brightness = value)) }
    }

    override fun requestWriteSettings() = quick.requestWriteSettings()

    override fun openInternetPanel() = quick.openInternetPanel()

    override fun openBluetooth() = quick.openBluetooth()

    override fun openVolumePanel() = quick.openVolumePanel()

    override fun openSettings() = quick.openSettings()

    override fun toggleIconStyle() = updateSettings {
        it.copy(iconStyle = if (it.iconStyle == IconStyle.MONO) IconStyle.COLOR else IconStyle.MONO)
    }

    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val old = _ui.value.settings
        val new = transform(old)
        if (new == old) return
        prefs.save(new)
        _ui.update { it.copy(settings = new) }
        if (new.iconStyle != old.iconStyle) loadApps()
    }

    override fun resetSettings() = updateSettings { LauncherSettings() }

    override fun requestNotificationAccess() = quick.openNotificationAccess()

    override fun openNotification(item: NotificationItem) = NotificationStore.open(item)

    override fun dismissNotification(item: NotificationItem) = NotificationStore.dismiss(item)

    override fun dismissAllNotifications() = NotificationStore.dismissAll()

    override fun onCleared() {
        media.stop()
        quick.release()
    }

    companion object {
        const val MAX_FAVORITES = HomeLayout.MAX_DOCK
        private const val WEATHER_TTL_MS = 30 * 60 * 1000L
    }
}
