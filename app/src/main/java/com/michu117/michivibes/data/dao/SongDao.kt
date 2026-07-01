package com.michu117.michivibes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.michu117.michivibes.data.models.Song

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Query("SELECT * FROM songs ORDER BY title ASC")
    suspend fun getAllSongs(): List<Song>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): Song?

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT COUNT(DISTINCT artist) FROM songs")
    suspend fun getArtistCount(): Int

    @Query("SELECT COUNT(DISTINCT album) FROM songs")
    suspend fun getAlbumCount(): Int

    @Query("SELECT * FROM songs WHERE artist = :artistName")
    suspend fun getSongsByArtist(artistName: String): List<Song>

    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}
