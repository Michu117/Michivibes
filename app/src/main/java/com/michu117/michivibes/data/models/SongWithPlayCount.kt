package com.michu117.michivibes.data.models

import androidx.room.ColumnInfo

data class SongWithPlayCount(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int
)
