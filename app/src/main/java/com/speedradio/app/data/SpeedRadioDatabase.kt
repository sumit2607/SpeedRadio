package com.speedradio.app.data

import androidx.room.*
import com.speedradio.app.domain.AudioPost
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioPostDao {
    @Query("SELECT * FROM audio_posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<AudioPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: AudioPost)

    @Query("DELETE FROM audio_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("SELECT * FROM audio_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): AudioPost?
}

@Database(entities = [AudioPost::class], version = 1, exportSchema = false)
abstract class SpeedRadioDatabase : RoomDatabase() {
    abstract fun audioPostDao(): AudioPostDao
}
