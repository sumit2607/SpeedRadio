package com.speedradio.app.data

import com.speedradio.app.domain.AudioPost
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val dao: AudioPostDao
) {

    val posts: Flow<List<AudioPost>> = dao.getAllPosts()

    suspend fun addPost(post: AudioPost) {
        dao.insertPost(post)
    }

    suspend fun deletePost(postId: String) {
        val post = dao.getPostById(postId) ?: return
        File(post.filePath).delete()
        dao.deletePost(postId)
    }

    suspend fun getPost(postId: String): AudioPost? = dao.getPostById(postId)
}
