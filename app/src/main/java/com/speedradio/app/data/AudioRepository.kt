package com.speedradio.app.data

import com.speedradio.app.domain.AudioPost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor() {

    private val _posts = MutableStateFlow<List<AudioPost>>(emptyList())
    val posts: Flow<List<AudioPost>> = _posts.asStateFlow()

    fun addPost(post: AudioPost) {
        _posts.value = listOf(post) + _posts.value
    }

    fun deletePost(id: String) {
        val post = _posts.value.find { it.id == id } ?: return
        File(post.filePath).delete()
        _posts.value = _posts.value.filter { it.id != id }
    }

    fun getPost(id: String): AudioPost? = _posts.value.find { it.id == id }
}
