package com.michu117.michivibes.data.repository

import com.michu117.michivibes.data.dao.SongDao
import com.michu117.michivibes.data.models.Song

class SongRepository(private val songDao: SongDao) {
    suspend fun insertAll(songs: List<Song>) = songDao.insertAll(songs)

    suspend fun insert(song: Song) = songDao.insert(song)

    suspend fun getAllSongs(): List<Song> = songDao.getAllSongs()

    suspend fun getSongById(id: String): Song? = songDao.getSongById(id)

    suspend fun getSongCount(): Int = songDao.getSongCount()

    suspend fun getArtistCount(): Int = songDao.getArtistCount()

    suspend fun getAlbumCount(): Int = songDao.getAlbumCount()

    suspend fun getSongsByArtist(artistName: String): List<Song> = songDao.getSongsByArtist(artistName)

    suspend fun deleteAll() = songDao.deleteAll()
}
