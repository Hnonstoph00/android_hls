package com.example.hlstest

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.datasource.cache.CacheSpan
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var simpleCache: SimpleCache
    private lateinit var downloadManager: DownloadManager
    private lateinit var dataSourceFactory: DefaultDataSource.Factory
    private lateinit var cacheDataSourceFactory: CacheDataSource.Factory
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SimpleCache (caching for the HLS stream)
        val cacheDir = File(cacheDir, "media")
        if (!cacheDir.exists()) {
            Log.d("file", "not exist")
            cacheDir.mkdirs() // Create the directory if it doesn't exist
        }
        val databaseProvider: DatabaseProvider = ExoDatabaseProvider(this)
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100 MB cache size
        simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

        // Add a listener to monitor cache events


        // Create a DataSource Factory for downloading the video stream
        val dataSourceFactory = DefaultDataSource.Factory(this)

        // Cache DataSource Factory
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(dataSourceFactory)


        // Register the cache listener

        // Register the custom cache listener
        simpleCache.addListener("https://dramabox.store/nquEby0.ts", object : Cache.Listener {
            override fun onSpanAdded(
                cache: Cache,
                span: com.google.android.exoplayer2.upstream.cache.CacheSpan
            ) {
                Log.d("Cache12", "add ${cache.keys}")
            }

            override fun onSpanRemoved(
                cache: Cache,
                span: com.google.android.exoplayer2.upstream.cache.CacheSpan
            ) {
                TODO("Not yet implemented")
                Log.d("Cache12", "remove ${cache.keys}")

            }

            override fun onSpanTouched(
                cache: Cache,
                oldSpan: com.google.android.exoplayer2.upstream.cache.CacheSpan,
                newSpan: com.google.android.exoplayer2.upstream.cache.CacheSpan
            ) {
                TODO("Not yet implemented")
            }

        })

        // Set up the MediaSourceFactory with cache support
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        // Create a LoadControl with min and max buffering
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000, // minBufferMs (5 seconds)
                10000, // maxBufferMs (10 seconds)
                1500, // bufferForPlaybackMs (1.5 seconds)
                3000 // bufferForPlaybackAfterRebufferMs (3 seconds)
            )
            .build()

        // Initialize ExoPlayer with custom LoadControl
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl) // Set the LoadControl
            .build()

        // Bind the player to the PlayerView
        val playerView = findViewById<PlayerView>(R.id.player_view)
        playerView.player = player
        // Set up data source factory


        // Set up executor for background tasks
        executor = Executors.newSingleThreadExecutor()

        // Initialize DownloadManager
        downloadManager = DownloadManager(
            this,
            ExoDatabaseProvider(this),   // DatabaseProvider
            simpleCache,                  // Cache
            cacheDataSourceFactory,       // Factory for upstream data source
            executor                      // Executor
        )


        player.addListener(object: Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                Log.d("Video player", "LOG + $events")
            }
        })

        // Button to start the player
        val startButton: Button = findViewById(R.id.start_player_button)
        startButton.setOnClickListener {
            startPlayer()
        }

        // Button to stop the player
        val stopButton: Button = findViewById(R.id.stop_player_button)
        stopButton.setOnClickListener {
            stopPlayer()
        }
    }
    private fun startPlayer() {
        // Prepare and start playback
        val mediaItem = MediaItem.fromUri("https://dramabox.store/UzUZ3i.m3u8")
        val mediaSource: MediaSource = DefaultMediaSourceFactory(DefaultDataSource.Factory(this))
            .createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    private fun stopPlayer() {
        // Stop the player and release resources
        startDownload("https://dramabox.store/N3U3Qn.m3u8")
        startDownload("https://dramabox.store/UzUZ3i0.ts")


    }
    private fun startDownload(url: String) {
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                super.onDownloadChanged(downloadManager, download, finalException)
                Log.d("Download xys", "Download ${download.state} complete, file saved to cache.")

                if (download.state == Download.STATE_COMPLETED) {
                    // Download completed, file saved to cache
                    Log.d("Download xys", "Download complete, file saved to cache.")
                    // Access the downloaded file from cache

                    // You can now use the cached file
                }
            }
        })
        // Create a download request
        val uri = Uri.parse(url)
        val request = DownloadRequest.Builder(url, uri).build();

        // Add the download to the DownloadManager
        downloadManager.addDownload(request)

        // Set up a listener to track download progress and completion

    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the player and cache when the activity is destroyed
        player.release()
        simpleCache.release()
    }
}


