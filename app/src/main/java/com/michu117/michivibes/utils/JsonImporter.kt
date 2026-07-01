package com.michu117.michivibes.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.michu117.michivibes.data.models.Scrobble
import com.michu117.michivibes.data.models.Song

data class JsonScrobble(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val duration: Long? = null,
    val playedAt: String? = null,
    val song: String? = null,
    val artistText: String? = null,
    val albumText: String? = null
)

class JsonImporter {
    private val gson = Gson()

    fun import(context: Context, uri: Uri): ImportResult {
        val songs = mutableListOf<Song>()
        val scrobbles = mutableListOf<Scrobble>()
        val errors = mutableListOf<String>()
        var validCount = 0
        var errorCount = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.readText() ?: return ImportResult(
                emptyList(), emptyList(), 0, 1, listOf("No se pudo abrir el archivo")
            )

            val listType = object : TypeToken<List<JsonScrobble>>() {}.type
            val jsonScrobbles: List<JsonScrobble> = try {
                gson.fromJson(jsonString, listType)
            } catch (e: Exception) {
                val singleType = object : TypeToken<Map<String, JsonScrobble>>() {}.type
                val map: Map<String, JsonScrobble>? = try {
                    gson.fromJson(jsonString, singleType)
                } catch (e2: Exception) {
                    null
                }
                map?.values?.toList() ?: emptyList()
            }

            for ((index, jsonScrobble) in jsonScrobbles.withIndex()) {
                try {
                    val title = jsonScrobble.title ?: jsonScrobble.song
                        ?: throw IllegalArgumentException("Falta título")
                    val artist = jsonScrobble.artist ?: jsonScrobble.artistText
                        ?: throw IllegalArgumentException("Falta artista")
                    val album = jsonScrobble.album ?: jsonScrobble.albumText ?: ""

                    val songId = "${artist} - ${title}".hashCode().toLong().toString() + album.hashCode()

                    val song = Song(
                        id = songId,
                        title = title.trim(),
                        artist = artist.trim(),
                        album = album.trim(),
                        duration = jsonScrobble.duration ?: 0L
                    )

                    if (!songs.any { it.id == songId }) {
                        songs.add(song)
                    }

                    val timestamp = jsonScrobble.timestamp
                        ?: DateUtils.parseDateFlexible(jsonScrobble.date ?: jsonScrobble.playedAt ?: "")
                        ?: System.currentTimeMillis()

                    scrobbles.add(Scrobble(songId = songId, playedAt = timestamp))
                    validCount++
                } catch (e: Exception) {
                    errorCount++
                    errors.add("Registro ${index + 1}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            return ImportResult(emptyList(), emptyList(), 0, 1, listOf("Error: ${e.message}"))
        }

        return ImportResult(songs, scrobbles, validCount, errorCount, errors)
    }
}
