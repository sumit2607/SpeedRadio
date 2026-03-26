package com.speedradio.app.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
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

    companion object {
        private const val TAG = "MEDIA_DEBUG"
        const val CHANNEL_ID = "speedradio_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        createNotificationChannel()

        // super.onCreate() triggers Hilt field injection
        super.onCreate()

        // Configure Media3 notification provider BEFORE adding the session
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )

        buildAndRegisterSession()

        Log.d(TAG, "PlaybackService onCreate complete, session=${mediaSession?.id}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SpeedRadio background audio controls"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: ${channel.id}")
        }
    }

    private fun buildAndRegisterSession() {
        if (mediaSession != null) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val session = MediaSession.Builder(this, audioPlayerManager.player)
            .setSessionActivity(pendingIntent)
            .build()

        mediaSession = session

        // CRITICAL: Register the session with the service.
        // Without addSession(), the MediaNotificationManager never knows about the
        // session and will never post the playback notification. This is separate from
        // just building the session.
        addSession(session)

        Log.d(TAG, "MediaSession built & registered, id=${session.id}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PlaybackService onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    // Called by external MediaControllers (e.g. system media UI, Bluetooth).
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "PlaybackService onGetSession")
        return mediaSession
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        Log.d(TAG, "onUpdateNotification: startInForeground=$startInForegroundRequired " +
                "isPlaying=${session.player.isPlaying} state=${session.player.playbackState}")
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        Log.d(TAG, "PlaybackService onDestroy")
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved, playWhenReady=${audioPlayerManager.player.playWhenReady}")
        if (!audioPlayerManager.player.playWhenReady) {
            stopSelf()
        }
    }
}
