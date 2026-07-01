package com.michu117.michivibes.data.repository

import com.michu117.michivibes.data.dao.ScrobbleDao
import com.michu117.michivibes.data.models.AlbumStats
import com.michu117.michivibes.data.models.ArtistStats
import com.michu117.michivibes.data.models.Scrobble
import com.michu117.michivibes.data.models.SongWithPlayCount

class ScrobbleRepository(private val scrobbleDao: ScrobbleDao) {
    suspend fun insertAll(scrobbles: List<Scrobble>) = scrobbleDao.insertAll(scrobbles)

    suspend fun getScrobbleCount(): Long = scrobbleDao.getScrobbleCount()

    suspend fun getAllScrobbles(): List<Scrobble> = scrobbleDao.getAllScrobbles()

    suspend fun getScrobblesBetween(startTime: Long, endTime: Long): List<Scrobble> =
        scrobbleDao.getScrobblesBetween(startTime, endTime)

    suspend fun getScrobblesForSong(songId: String): List<Scrobble> =
        scrobbleDao.getScrobblesForSong(songId)

    suspend fun getPlayCountForSong(songId: String): Int =
        scrobbleDao.getPlayCountForSong(songId)

    suspend fun getUniqueSongCount(): Int = scrobbleDao.getUniqueSongCount()

    suspend fun deleteAll() = scrobbleDao.deleteAll()

    suspend fun getTotalListeningTime(): Long = scrobbleDao.getTotalListeningTime() ?: 0L

    suspend fun getTopSongs(startTime: Long, endTime: Long, limit: Int = 10): List<SongWithPlayCount> =
        scrobbleDao.getTopSongs(startTime, endTime, limit)

    suspend fun getTopArtists(startTime: Long, endTime: Long, limit: Int = 10): List<ArtistStats> =
        scrobbleDao.getTopArtists(startTime, endTime, limit)

    suspend fun getTopAlbums(startTime: Long, endTime: Long, limit: Int = 10): List<AlbumStats> =
        scrobbleDao.getTopAlbums(startTime, endTime, limit)
}
