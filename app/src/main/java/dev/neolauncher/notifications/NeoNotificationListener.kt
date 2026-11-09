package dev.neolauncher.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class NotificationItem(
    val key: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val clearable: Boolean,
    val autoCancel: Boolean,
    /** Ícone pequeno da notificação (glifo monocromático, a UI aplica a cor). */
    val icon: ImageBitmap?,
    val contentIntent: PendingIntent?,
)

/** Ponte entre o serviço (criado pelo sistema) e a UI do launcher. */
object NotificationStore {

    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    internal var service: NeoNotificationListener? = null

    internal fun publish(items: List<NotificationItem>) {
        _items.value = items
    }

    fun isAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun open(item: NotificationItem) {
        runCatching { item.contentIntent?.send() }
        if (item.autoCancel) dismiss(item)
    }

    fun dismiss(item: NotificationItem) {
        service?.cancelNotification(item.key)
    }

    fun dismissAll() {
        service?.cancelAllNotifications()
    }
}

class NeoNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationStore.service = this
        publish()
    }

    override fun onListenerDisconnected() {
        NotificationStore.service = null
        NotificationStore.publish(emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = publish()

    override fun onNotificationRemoved(sbn: StatusBarNotification) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull().orEmpty()
        NotificationStore.publish(
            active
                .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
                .mapNotNull { it.toItem() }
                .sortedByDescending { it.postTime },
        )
    }

    private fun StatusBarNotification.toItem(): NotificationItem? {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        return NotificationItem(
            key = key,
            appLabel = appLabel(packageName),
            title = title,
            text = text,
            postTime = postTime,
            clearable = isClearable,
            autoCancel = notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
            icon = runCatching { notification.smallIcon?.loadDrawable(this@NeoNotificationListener)?.toBitmap(64, 64)?.asImageBitmap() }
                .getOrNull(),
            contentIntent = notification.contentIntent,
        )
    }

    private fun appLabel(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
