package com.michu117.michivibes.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scrobbles")
data class Scrobble(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val songId: String,
    val playedAt: Long
)
