package com.speedradio.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.speedradio.app.domain.AudioPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentPostId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class AudioPlayerManager(
    private val context: Context
) {
    // SINGLE SOURCE OF TRUTH: The actual ExoPlayer
    val player: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        ExoPlayer.Builder(context).build().also { exo ->
            exo.setAudioAttributes(audioAttributes, true)
            exo.setHandleAudioBecomingNoisy(true)
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = isPlaying,
                        durationMs = if (exo.duration > 0) exo.duration else 0L
                    )
                    if (isPlaying) {
                        startProgressPolling()
                    } else {
                        stopProgressPolling()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val postId = mediaItem?.mediaId
                    _playbackState.value = _playbackState.value.copy(
                        currentPostId = postId,
                        positionMs = 0L
                    )
                }
            })
        }
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    
    private var currentQueue: List<AudioPost> = emptyList()

    private fun startService() {
        android.util.Log.d("MEDIA_DEBUG", "startService called")
        val intent = Intent(context, PlaybackService::class.java)
        // Use standard startService. Since the app is in the foreground when play is clicked,
        // this is legal. Media3 will automatically promote the service to foreground (and post
        // the notification) once the buffer is ready and playback actually begins.
        context.startService(intent)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = player.currentPosition,
                        durationMs = if (player.duration > 0) player.duration else 0L
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun play(post: AudioPost, fullQueue: List<AudioPost> = emptyList()) {
        currentQueue = if (fullQueue.isNotEmpty()) fullQueue else listOf(post)
        val targetIndex = currentQueue.indexOfFirst { it.id == post.id }.coerceAtLeast(0)

        // Start service FIRST before doing any player operations
        startService()

        if (player.mediaItemCount != currentQueue.size) {
            val mediaItems = currentQueue.map { item ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist("Clip")
                    .setArtworkUri(Uri.parse("https://picsum.photos/seed/${item.id.hashCode()}/500/500"))
                    .build()

                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(item.filePath)
                    .setMediaMetadata(metadata)
                    .build()
            }
            player.stop()
            player.clearMediaItems()
            player.setMediaItems(mediaItems)
            player.seekTo(targetIndex, 0L)
            player.prepare()
        } else if (player.currentMediaItemIndex != targetIndex) {
            player.seekTo(targetIndex, 0L)
        }

        player.play()
    }

    fun pause() = player.pause()
    fun resume() = player.play()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
            player.play()
        }
    }

    fun playPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
            player.play()
        }
    }

    fun stop() {
        player.stop()
        _playbackState.value = PlaybackState()
        // Stop service explicitly
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    fun release() {
        stopProgressPolling()
        player.release()
    }
}
