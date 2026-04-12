package com.example.mymusicplayer

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mymusicplayer.model.AudioModel
import com.example.mymusicplayer.service.MusicService

class MainActivity : AppCompatActivity() {

    private val audioList = mutableListOf<AudioModel>()
    private lateinit var listView: ListView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private var selectedAudio: AudioModel? = null

    companion object {
        private const val REQUEST_CODE_MEDIA_PERMISSION = 101
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        startBtn = findViewById(R.id.btnStart)
        stopBtn = findViewById(R.id.btnStop)

        checkAndRequestPermissions()

        startBtn.setOnClickListener {
            val toPlay = selectedAudio ?: audioList.firstOrNull()

            if (toPlay != null) {
                startPlayback(toPlay)
            } else {
                Toast.makeText(this, "No audio files found on device", Toast.LENGTH_SHORT).show()
            }
        }

        stopBtn.setOnClickListener {
            stopService(Intent(this, MusicService::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedAudio = audioList[position]
            startPlayback(selectedAudio!!)
        }
    }

    private fun startPlayback(audio: AudioModel) {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra(MusicService.EXTRA_MUSIC_URI, audio.uri)
            putExtra(MusicService.EXTRA_MUSIC_TITLE, audio.title)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Playing: ${audio.title}", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestPermissions() {
        val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val mediaGranted = ContextCompat.checkSelfPermission(this, mediaPermission) ==
            PackageManager.PERMISSION_GRANTED

        if (!mediaGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(mediaPermission),
                REQUEST_CODE_MEDIA_PERMISSION
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!notificationGranted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }

        loadAudio()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_MEDIA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAudio()
            } else {
                Toast.makeText(this, "Media permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadAudio() {

        audioList.clear()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)

        cursor?.use {

            val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val idIndex = it.getColumnIndex(MediaStore.Audio.Media._ID)

            while (it.moveToNext()) {

                val title = it.getString(titleIndex)
                val artist = it.getString(artistIndex) ?: "Unknown artist"
                val id = it.getLong(idIndex)

                val contentUri = ContentUris.withAppendedId(uri, id)

                audioList.add(
                    AudioModel(
                        title = title,
                        artist = artist,
                        uri = contentUri.toString()
                    )
                )
            }
        }

        selectedAudio = audioList.firstOrNull()

        val displayList = audioList.map { audio ->
            "${audio.title}\n${audio.artist}\n${audio.uri}"
        }

        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayList
        )

        if (audioList.isEmpty()) {
            Toast.makeText(this, "No audio files found", Toast.LENGTH_LONG).show()
        }
    }
}