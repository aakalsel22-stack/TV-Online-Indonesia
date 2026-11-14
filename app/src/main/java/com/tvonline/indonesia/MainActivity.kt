package com.tvonline.indonesia

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

data class Channel(val name: String, val url: String)

class MainActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var channelList: LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var status: TextView
    private val channels = mutableListOf<Channel>()
    private val favorites = mutableSetOf<String>()
    private var showFavoritesOnly = false
    private var fullScreen = false

    private val prefs by lazy { getSharedPreferences("tv_online", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())
        buildScreen()
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                status.text = "Gagal memutar. Periksa URL, koneksi, atau izin siaran."
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) status.text = "Siaran siap diputar"
                if (playbackState == Player.STATE_BUFFERING) status.text = "Memuat siaran..."
            }
        })
        renderChannels()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 12)
            setBackgroundColor(0xFF101820.toInt())
        }
        val heading = TextView(this).apply {
            text = "TV ONLINE INDONESIA"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 4, 0, 12)
        }
        page.addView(heading)

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(0xFF000000.toInt())
        }
        page.addView(playerView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(220)
        ))

        status = TextView(this).apply {
            text = "Import playlist M3U untuk menambahkan channel."
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setPadding(0, 8, 0, 8)
        }
        page.addView(status)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(makeButton("Import M3U") { openM3uPicker() }, weightedParams())
        actions.addView(makeButton("Favorit") {
            showFavoritesOnly = !showFavoritesOnly
            renderChannels()
        }, weightedParams())
        actions.addView(makeButton("Layar penuh") { toggleFullscreen() }, weightedParams())
        page.addView(actions)

        searchBox = EditText(this).apply {
            hint = "Cari channel..."
            singleLine = true
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFBBBBBB.toInt())
            addTextChangedListener(SimpleTextWatcher { renderChannels() })
        }
        page.addView(searchBox)

        val scroll = ScrollView(this)
        channelList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(channelList)
        page.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(page)
    }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply { text = label; setOnClickListener { action() } }

    private fun weightedParams() = LinearLayout.LayoutParams(0, dp(48), 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun openM3uPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, 1001)
    }

    @Deprecated("Use Activity Result APIs in a future update")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val imported = parseM3u(reader.readLines())
                    channels.clear()
                    channels.addAll(imported)
                    showFavoritesOnly = false
                    status.text = "${channels.size} channel berhasil dimuat"
                    renderChannels()
                }
            } catch (e: Exception) {
                status.text = "Playlist tidak dapat dibaca: ${e.message ?: "format file"}"
            }
        }
    }

    private fun parseM3u(lines: List<String>): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = "Channel"
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                name = line.substringAfter(",", "Channel").trim().ifEmpty { "Channel" }
            } else if (!line.startsWith("#") &&
                (line.startsWith("http://") || line.startsWith("https://"))) {
                result.add(Channel(name, line))
                name = "Channel"
            }
        }
        return result
    }

    private fun renderChannels() {
        if (!::channelList.isInitialized) return
        channelList.removeAllViews()
        val query = searchBox.text.toString().trim()
        val visible = channels.filter {
            (!showFavoritesOnly || favorites.contains(it.url)) &&
            it.name.contains(query, ignoreCase = true)
        }
        if (visible.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (channels.isEmpty()) "Belum ada channel. Tekan Import M3U."
                       else "Channel tidak ditemukan."
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(8, 18, 8, 18)
            }
            channelList.addView(empty)
        }
        visible.forEach { channel ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val play = Button(this).apply {
                text = channel.name
                isAllCaps = false
                setOnClickListener { playChannel(channel) }
            }
            row.addView(play, LinearLayout.LayoutParams(0, dp(52), 1f))
            val fav = Button(this).apply {
                text = if (favorites.contains(channel.url)) "★" else "☆"
                setOnClickListener {
                    if (!favorites.add(channel.url)) favorites.remove(channel.url)
                    prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
                    renderChannels()
                }
            }
            row.addView(fav, LinearLayout.LayoutParams(dp(60), dp(52)))
            channelList.addView(row)
        }
    }

    private fun playChannel(channel: Channel) {
        status.text = "Memuat: ${channel.name}"
        player?.setMediaItem(MediaItem.fromUri(channel.url))
        player?.prepare()
        player?.play()
    }

    private fun toggleFullscreen() {
        fullScreen = !fullScreen
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (fullScreen) {
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        } else {
            View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}

private class SimpleTextWatcher(val changed: () -> Unit) :
    android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        changed()
    }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
