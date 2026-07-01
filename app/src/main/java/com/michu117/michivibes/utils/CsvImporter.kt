package com.michu117.michivibes.utils

import android.content.Context
import android.net.Uri
import com.michu117.michivibes.data.models.Scrobble
import com.michu117.michivibes.data.models.Song

data class ImportResult(
    val songs: List<Song>,
    val scrobbles: List<Scrobble>,
    val validCount: Int,
    val errorCount: Int,
    val errors: List<String>
)

class CsvImporter {
    fun import(context: Context, uri: Uri): ImportResult {
        val songs = mutableListOf<Song>()
        val scrobbles = mutableListOf<Scrobble>()
        val errors = mutableListOf<String>()
        var validCount = 0
        var errorCount = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = inputStream?.bufferedReader() ?: return ImportResult(
                emptyList(), emptyList(), 0, 1, listOf("No se pudo abrir el archivo")
            )

            val lines = reader.readLines()
            if (lines.isEmpty()) {
                return ImportResult(emptyList(), emptyList(), 0, 1, listOf("Archivo vacío"))
            }

            val headerLine = lines.first().trim()
            val columns = parseCsvLine(headerLine)
            val columnMap = mapColumns(columns)

            for ((index, line) in lines.drop(1).withIndex()) {
                try {
                    val values = parseCsvLine(line)
                    if (values.size < 4) {
                        errorCount++
                        continue
                    }

                    val title = getValue(values, columnMap, "title", "song", "name", "track")
                        ?: throw IllegalArgumentException("Falta título")
                    val artist = getValue(values, columnMap, "artist", "singer", "performer")
                        ?: throw IllegalArgumentException("Falta artista")
                    val album = getValue(values, columnMap, "album", "release") ?: ""
                    val dateStr = getValue(values, columnMap, "date", "played_at", "timestamp", "time", "datetime")
                    val durationStr = getValue(values, columnMap, "duration", "length", "ms")
                    val playCountStr = getValue(values, columnMap, "count", "plays", "playcount")

                    val songId = "${artist} - ${title}".hashCode().toLong().toString() + album.hashCode()
                    val duration = durationStr?.toLongOrNull() ?: 0L
                    val playCount = playCountStr?.toIntOrNull() ?: 1

                    val song = Song(
                        id = songId,
                        title = title.trim(),
                        artist = artist.trim(),
                        album = album.trim(),
                        duration = duration
                    )

                    if (!songs.any { it.id == songId }) {
                        songs.add(song)
                    }

                    if (dateStr != null) {
                        val timestamp = DateUtils.parseDateFlexible(dateStr.trim())
                        if (timestamp != null) {
                            repeat(playCount) {
                                scrobbles.add(
                                    Scrobble(songId = songId, playedAt = timestamp)
                                )
                            }
                        } else {
                            scrobbles.add(
                                Scrobble(songId = songId, playedAt = System.currentTimeMillis())
                            )
                        }
                    }

                    validCount++
                } catch (e: Exception) {
                    errorCount++
                    errors.add("Línea ${index + 2}: ${e.message}")
                }
            }

            reader.close()
        } catch (e: Exception) {
            return ImportResult(emptyList(), emptyList(), 0, 1, listOf("Error: ${e.message}"))
        }

        return ImportResult(songs, scrobbles, validCount, errorCount, errors)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun mapColumns(columns: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        columns.forEachIndexed { index, col ->
            map[col.lowercase().trim()] = index
        }
        return map
    }

    private fun getValue(values: List<String>, columnMap: Map<String, Int>, vararg keys: String): String? {
        for (key in keys) {
            val index = columnMap[key.lowercase()]
            if (index != null && index < values.size) {
                val value = values[index].trim()
                if (value.isNotEmpty() && value != "\"\"") {
                    return value.removeSurrounding("\"")
                }
            }
        }
        return null
    }
}
