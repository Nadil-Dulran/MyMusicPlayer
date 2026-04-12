package com.example.mymusicplayer.service

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mymusicplayer.MainActivity

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val channelId = "music_channel"

    companion object {
        const val EXTRA_MUSIC_URI = "music_uri"
        const val EXTRA_MUSIC_TITLE = "music_title"
        const val PREFS_NAME = "music_prefs"
        const val PREF_LAST_URI = "last_uri"
        const val PREF_IS_PLAYING = "is_playing"
        const val PREF_RESUME_ON_BATTERY_OKAY = "resume_on_battery_okay"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val uriString = intent?.getStringExtra(EXTRA_MUSIC_URI)
        val title = intent?.getStringExtra(EXTRA_MUSIC_TITLE) ?: "Playing music"
        val uri = uriString?.let(Uri::parse)

        if (uri != null) {
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }

            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_LAST_URI, uri.toString())
                .putBoolean(PREF_IS_PLAYING, true)
                .putBoolean(PREF_RESUME_ON_BATTERY_OKAY, false)
                .apply()
        } else if (mediaPlayer == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Music Player")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_IS_PLAYING, false)
            .apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Music Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}