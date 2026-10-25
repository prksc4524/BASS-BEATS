
package com.bassbeats.music

import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.MediaStore
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {

    private var player: MediaPlayer? = null
    private var bass: BassBoost? = null
    private val songs = mutableListOf<Pair<String, String>>()
    private var current = 0
    private lateinit var list: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 12, 20)
        window.navigationBarColor = Color.rgb(12, 12, 20)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(Color.rgb(12, 12, 20))
        }

        val title = TextView(this).apply {
            text = "🎧 BASS BEATS"
            textSize = 28f
            setTextColor(Color.CYAN)
            gravity = 17
        }
        root.addView(title)

        status = TextView(this).apply {
            text = "Your offline music"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = 17
            setPadding(0, 20, 0, 20)
        }
        root.addView(status)

        val controls = LinearLayout(this).apply {
            gravity = 17
        }

        fun button(label: String, action: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { action() }
            controls.addView(b)
        }

        button("⏮") { play(current - 1) }
        button("▶ / ⏸") {
            if (player?.isPlaying == true) player?.pause()
            else if (player != null) player?.start()
            else play(current)
        }
        button("⏭") { play(current + 1) }
        root.addView(controls)

        val bassLabel = TextView(this).apply {
            text = "BASS BOOST"
            setTextColor(Color.CYAN)
        }
        root.addView(bassLabel)

        val bassSlider = SeekBar(this).apply { max = 100 }
        root.addView(bassSlider)
        bassSlider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    s: SeekBar?, p: Int, user: Boolean
                ) {
                    try {
                        bass?.setStrength((p * 10).toShort())
                    } catch (_: Exception) {}
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            }
        )

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            -1, 0, 1f
        ))

        setContentView(root)

        if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            scanSongs()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                10
            )
        }
    }

    private fun scanSongs() {
        songs.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )
        val cursor: Cursor? = contentResolver.query(
            collection, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0) ?: "Unknown"
                val path = it.getString(1) ?: continue
                songs.add(Pair(name, path))
            }
        }
        list.removeAllViews()
        if (songs.isEmpty()) {
            status.text = "No songs found"
            return
        }
        status.text = "${songs.size} songs found"
        songs.forEachIndexed { index, song ->
            val item = TextView(this).apply {
                text = "♫  ${song.first}"
                textSize = 17f
                setTextColor(Color.WHITE)
                setPadding(12, 16, 12, 16)
                setOnClickListener { play(index) }
            }
            list.addView(item)
        }
    }

    private fun play(index: Int) {
        if (songs.isEmpty()) return
        current = ((index % songs.size) + songs.size) % songs.size
        try {
            player?.release()
            bass?.release()
            player = MediaPlayer().apply {
                setDataSource(songs[current].second)
                setOnCompletionListener { play(current + 1) }
                prepare()
                start()
            }
            bass = BassBoost(0, player!!.audioSessionId).apply {
                enabled = true
            }
            status.text = "▶ ${songs[current].first}"
        } catch (e: Exception) {
            status.text = "Cannot play this song"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        if (requestCode == 10 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            scanSongs()
        } else {
            status.text = "Audio permission required"
        }
    }

    override fun onDestroy() {
        player?.release()
        bass?.release()
        super.onDestroy()
    }
}
