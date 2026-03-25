package com.speedradio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speedradio.app.data.AudioRepository
import com.speedradio.app.domain.AudioPost
import com.speedradio.app.player.AudioPlayerManager
import com.speedradio.app.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: AudioRepository,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    val posts: StateFlow<List<AudioPost>> = repository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    fun playPost(postId: String) {
        viewModelScope.launch {
            val post = repository.getPost(postId) ?: return@launch
            playerManager.play(post.id, post.filePath)
        }
    }

    fun togglePlayPause() {
        val current = playbackState.value
        if (current.isPlaying) {
            playerManager.pause()
        } else if (current.currentPostId != null) {
            playerManager.resume()
        }
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun stopPlayback() {
        playerManager.stop()
    }
}
