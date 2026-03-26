package com.speedradio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.speedradio.app.data.AudioRepository
import com.speedradio.app.domain.AudioPost
import com.speedradio.app.player.AudioPlayerManager
import com.speedradio.app.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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

    // Regular flow for state-based logic (like current track info)
    val postsFlow: StateFlow<List<AudioPost>> = repository.posts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Paged flow for the LazyColumn
    val pagedPosts: Flow<PagingData<AudioPost>> = repository.getPostsPaged()
        .cachedIn(viewModelScope)

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    fun onPostClicked(post: AudioPost) {
        // We still use the full current list for the player queue
        playerManager.play(post, postsFlow.value)
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
