package com.speedradio.app.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.speedradio.app.domain.AudioPost
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val database: SpeedRadioDatabase
) {
    private val dao = database.audioPostDao()

    val posts: Flow<List<AudioPost>> = dao.getAllPosts()

    fun getPostsPaged(): Flow<PagingData<AudioPost>> {
        return Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { dao.getAllPostsPaged() }
        ).flow
    }

    suspend fun insertPost(post: AudioPost) = dao.insertPost(post)
    suspend fun deletePost(postId: String) = dao.deletePost(postId)
    suspend fun getPost(postId: String) = dao.getPostById(postId)
}
