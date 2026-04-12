package com.example.mymusicplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.example.mymusicplayer.service.MusicService

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {

            Intent.ACTION_BATTERY_LOW -> {
                val stopIntent = Intent(context, MusicService::class.java)
                context.stopService(stopIntent)

                Toast.makeText(context,
                    "Battery low - music playback stopped",
                    Toast.LENGTH_LONG).show()
            }

            Intent.ACTION_BATTERY_OKAY -> {
                val prefs = context.getSharedPreferences(MusicService.PREFS_NAME, Context.MODE_PRIVATE)
                val lastUri = prefs.getString(MusicService.PREF_LAST_URI, null)

                if (!lastUri.isNullOrBlank()) {
                    val restartIntent = Intent(context, MusicService::class.java).apply {
                        putExtra(MusicService.EXTRA_MUSIC_URI, lastUri)
                        putExtra(MusicService.EXTRA_MUSIC_TITLE, "Resumed after battery recovery")
                    }
                    ContextCompat.startForegroundService(context, restartIntent)
                }

                Toast.makeText(context,
                    "Battery okay - playback resumed",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}