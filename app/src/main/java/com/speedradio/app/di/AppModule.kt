package com.speedradio.app.di

import com.speedradio.app.data.AudioRepository
import com.speedradio.app.player.AudioPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioRepository(): AudioRepository = AudioRepository()
}
