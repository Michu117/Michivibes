package com.michu117.michivibes.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_sessions")
data class ListeningSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playedAt: Long,
    val sourcePlayer: String,
    val albumArtUri: String? = null
)
