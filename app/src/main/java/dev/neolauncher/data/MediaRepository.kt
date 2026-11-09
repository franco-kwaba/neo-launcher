package dev.neolauncher.data

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.neolauncher.notifications.NeoNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class MediaInfo(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val art: ImageBitmap?,
)

/**
 * Música tocando em qualquer app. O Android só libera as sessões de mídia para apps
 * com acesso às notificações, por isso depende do [NeoNotificationListener].
 */
class MediaRepository(context: Context) {

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, NeoNotificationListener::class.java)
    private var controller: MediaController? = null
    private var started = false

    private val _state = MutableStateFlow<MediaInfo?>(null)
    val state: StateFlow<MediaInfo?> = _state.asStateFlow()

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish()
        }

        override fun onSessionDestroyed() {
            attach(activeSessions().firstOrNull())
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        attach(sessions?.firstOrNull())
    }

    fun start() {
        if (started) return
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
            started = true
            attach(activeSessions().firstOrNull())
        }
    }

    fun stop() {
        if (!started) return
        manager.removeOnActiveSessionsChangedListener(sessionsListener)
        attach(null)
        started = false
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    private fun activeSessions(): List<MediaController> =
        runCatching { manager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList())

    private fun attach(new: MediaController?) {
        controller?.unregisterCallback(callback)
        controller = new
        new?.registerCallback(callback)
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        _state.value = if (c == null || meta == null) {
            null
        } else {
            MediaInfo(
                title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: meta.description?.title?.toString().orEmpty(),
                artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                art = (meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART))
                    ?.asImageBitmap(),
            )
        }
    }
}
