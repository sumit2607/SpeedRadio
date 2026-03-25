package com.speedradio.app.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_posts")
data class AudioPost(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)
