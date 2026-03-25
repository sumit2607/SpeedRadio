package com.speedradio.app.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _playbackState.value = PlaybackState()
                    }
                }
            })
        }
    }

    fun play(postId: String, filePath: String) {
        if (_playbackState.value.currentPostId == postId && _playbackState.value.isPlaying) {
            player.pause()
            return
        }

        if (_playbackState.value.currentPostId != postId) {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(filePath))
            player.prepare()
        }

        _playbackState.value = PlaybackState(currentPostId = postId, isPlaying = true)
        player.play()
    }

    fun pause() {
        player.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        if (_playbackState.value.currentPostId != null && !_playbackState.value.isPlaying) {
            player.play()
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _playbackState.value = PlaybackState()
    }

    fun release() {
        player.release()
        _playbackState.value = PlaybackState()
    }
}
