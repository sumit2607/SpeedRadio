package com.speedradio.app.di

import android.content.Context
import androidx.room.Room
import com.speedradio.app.data.AudioPostDao
import com.speedradio.app.data.AudioRepository
import com.speedradio.app.data.SpeedRadioDatabase
import com.speedradio.app.player.AudioPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpeedRadioDatabase {
        return Room.databaseBuilder(
            context,
            SpeedRadioDatabase::class.java,
            "speedradio_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAudioRepository(database: SpeedRadioDatabase): AudioRepository = AudioRepository(database)

    @Provides
    @Singleton
    fun provideAudioPlayerManager(@ApplicationContext context: Context): AudioPlayerManager {
        return AudioPlayerManager(context)
    }
}
