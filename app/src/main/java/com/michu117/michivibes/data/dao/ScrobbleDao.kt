package com.michu117.michivibes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.michu117.michivibes.data.models.AlbumStats
import com.michu117.michivibes.data.models.ArtistStats
import com.michu117.michivibes.data.models.Scrobble
import com.michu117.michivibes.data.models.SongWithPlayCount

@Dao
interface ScrobbleDao {
    @Insert
    suspend fun insertAll(scrobbles: List<Scrobble>)

    @Query("SELECT COUNT(*) FROM scrobbles")
    suspend fun getScrobbleCount(): Long

    @Query("SELECT * FROM scrobbles ORDER BY playedAt DESC")
    suspend fun getAllScrobbles(): List<Scrobble>

    @Query("SELECT * FROM scrobbles WHERE playedAt >= :startTime AND playedAt <= :endTime ORDER BY playedAt ASC")
    suspend fun getScrobblesBetween(startTime: Long, endTime: Long): List<Scrobble>

    @Query("SELECT * FROM scrobbles WHERE songId = :songId ORDER BY playedAt DESC")
    suspend fun getScrobblesForSong(songId: String): List<Scrobble>

    @Query("SELECT COUNT(*) FROM scrobbles WHERE songId = :songId")
    suspend fun getPlayCountForSong(songId: String): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM scrobbles")
    suspend fun getUniqueSongCount(): Int

    @Query("DELETE FROM scrobbles")
    suspend fun deleteAll()

    @Query("SELECT SUM(s.duration) FROM scrobbles sc INNER JOIN songs s ON sc.songId = s.id")
    suspend fun getTotalListeningTime(): Long?

    @Query("""
        SELECT s.*, COUNT(sc.id) as playCount 
        FROM scrobbles sc 
        INNER JOIN songs s ON sc.songId = s.id 
        WHERE sc.playedAt >= :startTime AND sc.playedAt <= :endTime
        GROUP BY s.id 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopSongs(startTime: Long, endTime: Long, limit: Int): List<SongWithPlayCount>

    @Query("""
        SELECT s.artist, COUNT(sc.id) as playCount, SUM(s.duration) as totalDuration
        FROM scrobbles sc 
        INNER JOIN songs s ON sc.songId = s.id 
        WHERE sc.playedAt >= :startTime AND sc.playedAt <= :endTime
        GROUP BY s.artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopArtists(startTime: Long, endTime: Long, limit: Int): List<ArtistStats>

    @Query("""
        SELECT s.album, s.artist, COUNT(sc.id) as playCount
        FROM scrobbles sc 
        INNER JOIN songs s ON sc.songId = s.id 
        WHERE sc.playedAt >= :startTime AND sc.playedAt <= :endTime
        GROUP BY s.album 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopAlbums(startTime: Long, endTime: Long, limit: Int): List<AlbumStats>
}
