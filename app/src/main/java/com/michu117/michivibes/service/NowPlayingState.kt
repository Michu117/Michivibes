package com.michu117.michivibes.service

import android.graphics.Bitmap

data class NowPlayingInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
    val position: Long = 0L,
    val isPlaying: Boolean = false,
    val sourcePlayer: String = "",
    val albumArt: Bitmap? = null,
    val albumArtUri: String? = null
) {
    val songKey: String get() = "$artist - $title"
}
