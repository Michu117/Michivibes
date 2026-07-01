package com.michu117.michivibes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.michu117.michivibes.data.models.ListeningSession

@Dao
interface ListeningSessionDao {
    @Insert
    suspend fun insert(session: ListeningSession)

    @Insert
    suspend fun insertAll(sessions: List<ListeningSession>)

    @Query("SELECT COUNT(*) FROM listening_sessions")
    suspend fun getSessionCount(): Long

    @Query("SELECT SUM(duration) FROM listening_sessions")
    suspend fun getTotalListeningTime(): Long?

    @Query("SELECT COUNT(DISTINCT title || artist) FROM listening_sessions")
    suspend fun getUniqueSongCount(): Int

    @Query("SELECT COUNT(DISTINCT artist) FROM listening_sessions")
    suspend fun getUniqueArtistCount(): Int

    @Query("SELECT COUNT(DISTINCT album) FROM listening_sessions")
    suspend fun getUniqueAlbumCount(): Int

    @Query("SELECT * FROM listening_sessions WHERE playedAt >= :startTime AND playedAt <= :endTime ORDER BY playedAt DESC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<ListeningSession>

    @Query("SELECT * FROM listening_sessions ORDER BY playedAt DESC")
    suspend fun getAllSessions(): List<ListeningSession>

    @Query("""
        SELECT title as songTitle, artist, album, duration, 
        COUNT(*) as playCount, MAX(playedAt) as lastPlayedAt
        FROM listening_sessions 
        WHERE playedAt >= :startTime AND playedAt <= :endTime
        GROUP BY title, artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopSongs(startTime: Long, endTime: Long, limit: Int): List<ListeningSessionStat>

    @Query("""
        SELECT artist, COUNT(*) as playCount, SUM(duration) as totalDuration
        FROM listening_sessions 
        WHERE playedAt >= :startTime AND playedAt <= :endTime
        GROUP BY artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopArtists(startTime: Long, endTime: Long, limit: Int): List<ArtistSessionStat>

    @Query("""
        SELECT album, artist, COUNT(*) as playCount
        FROM listening_sessions 
        WHERE playedAt >= :startTime AND playedAt <= :endTime
        GROUP BY album 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopAlbums(startTime: Long, endTime: Long, limit: Int): List<AlbumSessionStat>

    @Query("""
        SELECT title as songTitle, artist, album, duration, 
        COUNT(*) as playCount, MAX(playedAt) as lastPlayedAt
        FROM listening_sessions 
        WHERE artist = :artistName AND playedAt >= :startTime AND playedAt <= :endTime
        GROUP BY title, artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    suspend fun getTopSongsByArtist(artistName: String, startTime: Long, endTime: Long, limit: Int = 10): List<ListeningSessionStat>

    @Query("SELECT * FROM listening_sessions ORDER BY id DESC LIMIT 1")
    suspend fun getLastSession(): ListeningSession?

    @Query("DELETE FROM listening_sessions")
    suspend fun deleteAll()
}

data class ListeningSessionStat(
    val songTitle: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int,
    val lastPlayedAt: Long
)

data class ArtistSessionStat(
    val artist: String,
    val playCount: Int,
    val totalDuration: Long
)

data class AlbumSessionStat(
    val album: String,
    val artist: String,
    val playCount: Int
)
