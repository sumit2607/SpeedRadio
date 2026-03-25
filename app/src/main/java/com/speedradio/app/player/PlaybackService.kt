package com.speedradio.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.speedradio.app.MainActivity
import com.speedradio.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        

        initializeSession()
    }

    private fun initializeSession() {
        if (mediaSession != null) return
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Important: Shared Player singleton
        mediaSession = MediaSession.Builder(this, audioPlayerManager.player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null) initializeSession()
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Clear session if we are not playing anymore
        if (!audioPlayerManager.player.isPlaying) {
             pauseAllPlayersAndStopSelf()
        }
    }
}
