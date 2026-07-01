package com.michu117.michivibes.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.app.Notification
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.michu117.michivibes.MusicWrappedApp
import com.michu117.michivibes.data.models.ListeningSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaSessionMonitorService : NotificationListenerService() {
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var app: MusicWrappedApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeControllers = mutableMapOf<String, MediaController>()
    private val controllerCallbacks = mutableMapOf<String, PlayerCallback>()
    private var monitorJob: Job? = null

    companion object {
        private const val TAG = "MediaMonitor"
        private const val MIN_PLAY_DURATION_MS = 30000L
        private const val MIN_PLAY_PERCENTAGE = 0.3f
        private const val DEDUP_INTERVAL_MS = 60000L
        private const val SCAN_INTERVAL_MS = 5000L

        private val lastRecordedSongs = mutableMapOf<String, Long>()

        fun start(context: Context) {
            val intent = Intent(context, MediaSessionMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaSessionMonitorService::class.java)
            context.stopService(intent)
        }

        fun isNotificationAccessGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        }

        fun createNotificationAccessIntent(context: Context): Intent {
            return Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as MusicWrappedApp
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        NotificationHelper.createChannel(this)
        startForeground(1, NotificationHelper.buildNotification(this))
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startMonitoring()
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        startMonitoring()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        handleMediaNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        stopMonitoring()
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                scanActiveSessions()
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        controllerCallbacks.values.forEach { callback ->
            try {
                callback.controller.unregisterCallback(callback)
            } catch (_: Exception) {}
        }
        activeControllers.clear()
        controllerCallbacks.clear()
    }

    private fun scanActiveSessions() {
        try {
            val controllers = try {
                val cn = ComponentName(this, javaClass)
                mediaSessionManager.getActiveSessions(cn)
            } catch (_: SecurityException) {
                @Suppress("DEPRECATION")
                mediaSessionManager.getActiveSessions(null)
            }

            val activePackageNames = controllers.map { it.packageName }.toSet()

            activeControllers.keys
                .filter { it !in activePackageNames }
                .forEach { cleanupController(it) }

            for (controller in controllers) {
                val pkg = controller.packageName
                if (pkg == packageName) continue

                if (pkg !in activeControllers) {
                    val callback = PlayerCallback(controller, pkg)
                    controller.registerCallback(callback)
                    activeControllers[pkg] = controller
                    controllerCallbacks[pkg] = callback
                    Log.d(TAG, "Monitoring: $pkg")
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to access media sessions: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning sessions: ${e.message}")
        }
    }

    private fun handleMediaNotification(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras ?: return

        val mediaSessionToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        } ?: return

        try {
            val controller = MediaController(this, mediaSessionToken)
            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Desconocido"
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val pkg = sbn.packageName

            val albumArtUri = try {
                metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            } catch (_: Exception) {
                null
            }

            NowPlayingHolder.update(NowPlayingInfo(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                position = 0L,
                isPlaying = true,
                sourcePlayer = pkg,
                albumArtUri = albumArtUri
            ))

            updateNotification()
            recordFromNotification(title, artist, album, duration, pkg, albumArtUri)
        } catch (_: Exception) {}
    }

    private fun recordFromNotification(
        title: String,
        artist: String,
        album: String,
        duration: Long,
        sourcePlayer: String,
        albumArtUri: String?
    ) {
        if (duration <= 0) return

        val songKey = "$artist - $title"
        val now = System.currentTimeMillis()
        val lastRecorded = lastRecordedSongs[songKey]

        if (lastRecorded != null && (now - lastRecorded) < DEDUP_INTERVAL_MS) return

        lastRecordedSongs[songKey] = now
        if (lastRecordedSongs.size > 200) {
            val oldestKeys = lastRecordedSongs.entries
                .sortedBy { it.value }
                .take(50)
                .map { it.key }
            oldestKeys.forEach { lastRecordedSongs.remove(it) }
        }

        val session = ListeningSession(
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            playedAt = now,
            sourcePlayer = sourcePlayer,
            albumArtUri = albumArtUri
        )

        scope.launch {
            try {
                app.listeningSessionRepository.insert(session)
                Log.d(TAG, "Recorded from notification: $songKey from $sourcePlayer")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save: ${e.message}")
            }
        }
    }

    private fun cleanupController(packageName: String) {
        val callback = controllerCallbacks.remove(packageName)
        val controller = activeControllers.remove(packageName)
        try {
            callback?.let { controller?.unregisterCallback(it) }
        } catch (_: Exception) {}
    }

    inner class PlayerCallback(
        val controller: MediaController,
        private val packageName: String
    ) : MediaController.Callback() {

        private var currentSongKey: String? = null
        private var playbackStartTime: Long = 0L
        private var tracking = false

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            if (state == null) return

            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Desconocido"
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val songKey = "$artist - $title"

            val albumArtUri = try {
                metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            } catch (_: Exception) {
                null
            }

            NowPlayingHolder.update(NowPlayingInfo(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                position = state.position,
                isPlaying = state.state == PlaybackState.STATE_PLAYING,
                sourcePlayer = packageName,
                albumArtUri = albumArtUri
            ))

            updateNotification()

            when (state.state) {
                PlaybackState.STATE_PLAYING -> {
                    if (songKey != currentSongKey) {
                        currentSongKey = songKey
                        playbackStartTime = System.currentTimeMillis()
                        tracking = true
                    }
                }
                PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> {
                    if (tracking && currentSongKey != null) {
                        recordIfQualified(title, artist, album, duration, packageName, albumArtUri)
                        tracking = false
                        currentSongKey = null
                    }
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            if (metadata == null) return

            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Desconocido"
            val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
            val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val songKey = "$artist - $title"

            val albumArtUri = try {
                metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            } catch (_: Exception) {
                null
            }

            val playbackState = controller.playbackState
            NowPlayingHolder.update(NowPlayingInfo(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                position = playbackState?.position ?: 0L,
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                sourcePlayer = packageName,
                albumArtUri = albumArtUri
            ))

            updateNotification()

            if (songKey != currentSongKey) {
                if (tracking && currentSongKey != null) {
                    val oldMetadata = currentSongKey?.split(" - ")
                    if (oldMetadata?.size == 2) {
                        recordIfQualified(
                            oldMetadata[1], oldMetadata[0], "",
                            0L, packageName, null
                        )
                    }
                }
                currentSongKey = songKey
                playbackStartTime = System.currentTimeMillis()
                tracking = true
            }
        }

        private fun recordIfQualified(
            title: String,
            artist: String,
            album: String,
            duration: Long,
            sourcePlayer: String,
            albumArtUri: String?
        ) {
            val elapsed = System.currentTimeMillis() - playbackStartTime
            val qualifies = if (duration > 0) {
                elapsed >= MIN_PLAY_DURATION_MS || elapsed >= duration * MIN_PLAY_PERCENTAGE
            } else {
                elapsed >= MIN_PLAY_DURATION_MS
            }

            if (!qualifies) return

            val songKey = "$artist - $title"
            val lastRecorded = lastRecordedSongs[songKey]
            val now = System.currentTimeMillis()

            if (lastRecorded != null && (now - lastRecorded) < DEDUP_INTERVAL_MS) return

            lastRecordedSongs[songKey] = now
            if (lastRecordedSongs.size > 200) {
                val oldestKeys = lastRecordedSongs.entries
                    .sortedBy { it.value }
                    .take(50)
                    .map { it.key }
                oldestKeys.forEach { lastRecordedSongs.remove(it) }
            }

            val session = ListeningSession(
                title = title,
                artist = artist,
                album = album,
                duration = if (duration > 0) duration else elapsed,
                playedAt = playbackStartTime,
                sourcePlayer = sourcePlayer,
                albumArtUri = albumArtUri
            )

            scope.launch {
                try {
                    app.listeningSessionRepository.insert(session)
                    Log.d(TAG, "Recorded: $songKey from $sourcePlayer")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save: ${e.message}")
                }
            }
        }
    }

    private fun updateNotification() {
        val info = NowPlayingHolder.nowPlaying.value
        if (info.title.isNotEmpty()) {
            startForeground(1, NotificationHelper.buildNowPlayingNotification(this, info))
        }
    }
}
