package com.michu117.michivibes.data.repository

import com.michu117.michivibes.data.dao.AlbumSessionStat
import com.michu117.michivibes.data.dao.ArtistSessionStat
import com.michu117.michivibes.data.dao.ListeningSessionDao
import com.michu117.michivibes.data.dao.ListeningSessionStat
import com.michu117.michivibes.data.models.ListeningSession

class ListeningSessionRepository(private val dao: ListeningSessionDao) {
    suspend fun insert(session: ListeningSession) = dao.insert(session)

    suspend fun insertAll(sessions: List<ListeningSession>) = dao.insertAll(sessions)

    suspend fun getSessionCount(): Long = dao.getSessionCount()

    suspend fun getTotalListeningTime(): Long = dao.getTotalListeningTime() ?: 0L

    suspend fun getUniqueSongCount(): Int = dao.getUniqueSongCount()

    suspend fun getUniqueArtistCount(): Int = dao.getUniqueArtistCount()

    suspend fun getUniqueAlbumCount(): Int = dao.getUniqueAlbumCount()

    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<ListeningSession> =
        dao.getSessionsBetween(startTime, endTime)

    suspend fun getAllSessions(): List<ListeningSession> = dao.getAllSessions()

    suspend fun getTopSongs(startTime: Long, endTime: Long, limit: Int = 10): List<ListeningSessionStat> =
        dao.getTopSongs(startTime, endTime, limit)

    suspend fun getTopArtists(startTime: Long, endTime: Long, limit: Int = 10): List<ArtistSessionStat> =
        dao.getTopArtists(startTime, endTime, limit)

    suspend fun getTopAlbums(startTime: Long, endTime: Long, limit: Int = 10): List<AlbumSessionStat> =
        dao.getTopAlbums(startTime, endTime, limit)

    suspend fun getTopSongsByArtist(artistName: String, startTime: Long, endTime: Long, limit: Int = 10) =
        dao.getTopSongsByArtist(artistName, startTime, endTime, limit)

    suspend fun getLastSession(): ListeningSession? = dao.getLastSession()

    suspend fun deleteAll() = dao.deleteAll()
}
