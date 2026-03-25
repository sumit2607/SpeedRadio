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
class FeedViewModel @Inject constructor(
    private val repository: AudioRepository,
    private val playerManager: AudioPlayerManager
) : ViewModel() {

    val posts: StateFlow<List<AudioPost>> = repository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    fun onPostClicked(post: AudioPost) {
        // Provide the entire list so System UI can support Next/Prev
        playerManager.play(post, posts.value)
    }

    fun pausePlayback() = playerManager.pause()
    fun resumePlayback() = playerManager.resume()

    fun deletePost(postId: String) {
        viewModelScope.launch {
            if (playbackState.value.currentPostId == postId) {
                playerManager.stop()
            }
            repository.deletePost(postId)
        }
    }
}
