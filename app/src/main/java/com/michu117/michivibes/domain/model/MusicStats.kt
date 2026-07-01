package com.michu117.michivibes.domain.model

import com.michu117.michivibes.data.models.AlbumStats
import com.michu117.michivibes.data.models.ArtistStats
import com.michu117.michivibes.data.models.SongWithPlayCount

data class MusicStats(
    val totalScrobbles: Long,
    val totalListeningTimeMs: Long,
    val totalSongs: Int,
    val totalArtists: Int,
    val totalAlbums: Int,
    val topSongs: List<SongWithPlayCount>,
    val topArtists: List<ArtistStats>,
    val topAlbums: List<AlbumStats>,
    val monthlyStats: List<MonthStats>,
    val weeklyStats: List<WeekStats>
)

data class MonthStats(
    val month: Int,
    val year: Int,
    val totalScrobbles: Int,
    val totalListeningTimeMs: Long,
    val topSong: String?,
    val topArtist: String?
)

data class WeekStats(
    val weekStart: Long,
    val totalScrobbles: Int,
    val totalListeningTimeMs: Long
)

data class WrappedData(
    val year: Int,
    val totalListeningTimeMs: Long,
    val totalScrobbles: Long,
    val totalSongs: Int,
    val totalArtists: Int,
    val topArtist: ArtistStats,
    val topSong: SongWithPlayCount,
    val mostActiveMonth: String,
    val topArtists: List<ArtistStats>,
    val topSongs: List<SongWithPlayCount>,
    val totalAlbums: Int
)
