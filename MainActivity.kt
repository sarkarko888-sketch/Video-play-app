package com.yourpackage.name // Apna package name daalein

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var etVideoUrl: EditText
    private lateinit var btnPlay: Button
    private lateinit var btnEmbed: Button
    private lateinit var tvMetadata: TextView

    private var player: ExoPlayer? = null
    private lateinit var trackSelector: DefaultTrackSelector

    // Extracted Metadata save karne ke liye
    private val extractedQualities = mutableListOf<String>()
    private val extractedAudioLanguages = mutableListOf<String>()

    // Aapka Worker URL
    private val WORKER_URL = "https://dlproxy99.sarkar123rupan.workers.dev/api/create"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        etVideoUrl = findViewById(R.id.etVideoUrl)
        btnPlay = findViewById(R.id.btnPlay)
        btnEmbed = findViewById(R.id.btnEmbed)
        tvMetadata = findViewById(R.id.tvMetadata)

        initializePlayer()

        btnPlay.setOnClickListener {
            val url = etVideoUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                playVideo(url)
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        btnEmbed.setOnClickListener {
            val url = etVideoUrl.text.toString().trim()
            if (url.isEmpty() || extractedQualities.isEmpty()) {
                Toast.makeText(this, "Play video first to extract metadata!", Toast.LENGTH_SHORT).show()
            } else {
                createEmbedLink(url)
            }
        }
    }

    private fun initializePlayer() {
        // TrackSelector: Yeh Settings icon (⚙️) me audio/quality switch enable karta hai
        trackSelector = DefaultTrackSelector(this)

        // LoadControl (FAST LOADING HACK): Buffer time kam karke video turant start karta hai
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1500,  // Min Buffer: 1.5 sec (Jaldi start hoga)
                50000, // Max Buffer: 50 sec
                1000,  // Buffer for playback: 1 sec
                1000   // Buffer after rebuffer: 1 sec
            ).build()

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        playerView.player = player

        // METADATA EXTRACTOR: Jab video ka data load hota hai, yeh auto run hota hai
        player?.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                extractedQualities.clear()
                extractedAudioLanguages.clear()

                for (group in tracks.groups) {
                    val trackGroup = group.mediaTrackGroup
                    
                    // Extract Video Qualities (e.g., 1080p, 720p)
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until trackGroup.length) {
                            val format = trackGroup.getFormat(i)
                            if (format.height > 0) {
                                val quality = "${format.height}p"
                                if (!extractedQualities.contains(quality)) extractedQualities.add(quality)
                            }
                        }
                    }
                    
                    // Extract Audio Tracks (Languages)
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until trackGroup.length) {
                            val format = trackGroup.getFormat(i)
                            val lang = format.language ?: "Unknown"
                            if (!extractedAudioLanguages.contains(lang)) extractedAudioLanguages.add(lang)
                        }
                    }
                }

                // UI par dikhayein
                tvMetadata.text = "Detected -> Qualities: $extractedQualities | Audios: $extractedAudioLanguages"
                
                // Note: Quality and Audio switching functionality is AUTOMATICALLY handled 
                // by ExoPlayer's DefaultTrackSelector inside the PlayerView's Settings (⚙️) icon.
            }
        })
    }

    private fun playVideo(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    private fun createEmbedLink(originalUrl: String) {
        btnEmbed.text = "Creating..."
        btnEmbed.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. JSON Payload banayein metadata ke sath
                val jsonObject = JSONObject()
                jsonObject.put("originalUrl", originalUrl)

                val metadataObj = JSONObject()
                metadataObj.put("qualities", JSONArray(extractedQualities))
                metadataObj.put("audioTracks", JSONArray(extractedAudioLanguages))
                
                jsonObject.put("metadata", metadataObj)

                // 2. OkHttp request setup karein
                val client = OkHttpClient()
                val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(WORKER_URL)
                    .post(requestBody)
                    .build()

                // 3. Cloudflare Worker ko request bhejein
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                withContext(Dispatchers.Main) {
                    btnEmbed.text = "Create Embed"
                    btnEmbed.isEnabled = true

                    if (response.isSuccessful && responseData != null) {
                        val jsonResponse = JSONObject(responseData)
                        val embedLink = jsonResponse.getString("embedLink")
                        
                        // Result UI me dikhayein
                        tvMetadata.text = "SUCCESS! Embed Link: \n$embedLink"
                        Toast.makeText(this@MainActivity, "Link Copied (Feature to be added)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Error creating link", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnEmbed.text = "Create Embed"
                    btnEmbed.isEnabled = true
                    Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
