package com.speedradio.app.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val currentPostId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = isPlaying,
                        durationMs = if (exo.duration > 0) exo.duration else 0L
                    )
                    if (isPlaying) startProgressPolling() else stopProgressPolling()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _playbackState.value = _playbackState.value.copy(
                            durationMs = if (exo.duration > 0) exo.duration else 0L
                        )
                    } else if (state == Player.STATE_ENDED) {
                        stopProgressPolling()
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            positionMs = 0L
                        )
                        // If it ends, it remains "active" but not playing.
                    }
                }
            })
        }
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

    fun play(postId: String, filePath: String) {
        // "Play as many times as I want" logic:
        // If it's the same track and already playing, keep playing (or restart if user prefers).
        // Standard full-screen player behavior normally allows multiple plays via a dedicated play btn.
        // We'll prioritize playing.
        
        if (_playbackState.value.currentPostId != postId) {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(filePath))
            player.prepare()
            _playbackState.value = _playbackState.value.copy(
                currentPostId = postId,
                isPlaying = true,
                positionMs = 0L
            )
        } else if (!_playbackState.value.isPlaying) {
            // If it was paused or ended, resume it
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
        }

        player.play()
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    fun pause() {
        player.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        if (_playbackState.value.currentPostId != null && !_playbackState.value.isPlaying) {
            player.play()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(positionMs = positionMs)
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        stopProgressPolling()
        _playbackState.value = PlaybackState()
    }

    fun release() {
        stopProgressPolling()
        player.release()
        _playbackState.value = PlaybackState()
    }
}
