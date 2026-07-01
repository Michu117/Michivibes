package com.michu117.michivibes.domain.usecases

import com.michu117.michivibes.data.dao.AlbumSessionStat
import com.michu117.michivibes.data.dao.ArtistSessionStat
import com.michu117.michivibes.data.dao.ListeningSessionStat
import com.michu117.michivibes.data.repository.ListeningSessionRepository
import com.michu117.michivibes.data.repository.ScrobbleRepository
import com.michu117.michivibes.data.repository.SongRepository
import com.michu117.michivibes.domain.model.MonthStats
import com.michu117.michivibes.domain.model.MusicStats
import com.michu117.michivibes.domain.model.WeekStats
import com.michu117.michivibes.domain.model.WrappedData
import java.util.Calendar

class GetStatisticsUseCase(
    private val songRepository: SongRepository,
    private val scrobbleRepository: ScrobbleRepository,
    private val listeningSessionRepository: ListeningSessionRepository
) {
    suspend fun getGeneralStats(year: Int): MusicStats {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        val startOfYear = calendar.timeInMillis
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        val endOfYear = calendar.timeInMillis

        val sessionCount = listeningSessionRepository.getSessionCount()

        return if (sessionCount > 0) {
            MusicStats(
                totalScrobbles = sessionCount,
                totalListeningTimeMs = listeningSessionRepository.getTotalListeningTime(),
                totalSongs = listeningSessionRepository.getUniqueSongCount(),
                totalArtists = listeningSessionRepository.getUniqueArtistCount(),
                totalAlbums = listeningSessionRepository.getUniqueAlbumCount(),
                topSongs = listeningSessionRepository.getTopSongs(startOfYear, endOfYear, 10)
                    .map { it.toSongWithPlayCount() },
                topArtists = listeningSessionRepository.getTopArtists(startOfYear, endOfYear, 10)
                    .map { it.toArtistStats() },
                topAlbums = listeningSessionRepository.getTopAlbums(startOfYear, endOfYear, 10)
                    .map { it.toAlbumStats() },
                monthlyStats = getMonthlyStatsFromSessions(year),
                weeklyStats = getWeeklyStatsFromSessions(year)
            )
        } else {
            legacyStats(year)
        }
    }

    suspend fun getDashboardStats(): MusicStats {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return getGeneralStats(currentYear)
    }

    suspend fun getWrappedData(year: Int): WrappedData? {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        val startOfYear = calendar.timeInMillis
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        val endOfYear = calendar.timeInMillis

        val sessionCount = listeningSessionRepository.getSessionCount()

        return if (sessionCount > 0) {
            buildWrappedFromSessions(year, startOfYear, endOfYear)
        } else {
            buildLegacyWrapped(year)
        }
    }

    private suspend fun buildWrappedFromSessions(year: Int, startOfYear: Long, endOfYear: Long): WrappedData? {
        val totalScrobbles = listeningSessionRepository.getSessionCount()
        if (totalScrobbles == 0L) return null

        val topArtists = listeningSessionRepository.getTopArtists(startOfYear, endOfYear, 5)
        val topSongs = listeningSessionRepository.getTopSongs(startOfYear, endOfYear, 10)

        if (topArtists.isEmpty() || topSongs.isEmpty()) return null

        val monthlyStats = getMonthlyStatsFromSessions(year)
        val mostActiveMonth = monthlyStats.maxByOrNull { it.totalScrobbles }

        return WrappedData(
            year = year,
            totalListeningTimeMs = listeningSessionRepository.getTotalListeningTime(),
            totalScrobbles = totalScrobbles,
            totalSongs = listeningSessionRepository.getUniqueSongCount(),
            totalArtists = listeningSessionRepository.getUniqueArtistCount(),
            topArtist = topArtists.first().toArtistStats(),
            topSong = topSongs.first().toSongWithPlayCount(),
            mostActiveMonth = mostActiveMonth?.let {
                java.text.DateFormatSymbols().months[it.month - 1]
            } ?: "N/A",
            topArtists = topArtists.map { it.toArtistStats() },
            topSongs = topSongs.map { it.toSongWithPlayCount() },
            totalAlbums = listeningSessionRepository.getUniqueAlbumCount()
        )
    }

    private suspend fun buildLegacyWrapped(year: Int): WrappedData? {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        val startOfYear = calendar.timeInMillis
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        val endOfYear = calendar.timeInMillis

        val totalScrobbles = scrobbleRepository.getScrobbleCount()
        if (totalScrobbles == 0L) return null

        val topArtists = scrobbleRepository.getTopArtists(startOfYear, endOfYear, 5)
        val topSongs = scrobbleRepository.getTopSongs(startOfYear, endOfYear, 10)

        if (topArtists.isEmpty() || topSongs.isEmpty()) return null

        val monthlyStats = getMonthlyStats(year)
        val mostActiveMonth = monthlyStats.maxByOrNull { it.totalScrobbles }

        return WrappedData(
            year = year,
            totalListeningTimeMs = scrobbleRepository.getTotalListeningTime(),
            totalScrobbles = totalScrobbles,
            totalSongs = songRepository.getSongCount(),
            totalArtists = songRepository.getArtistCount(),
            topArtist = topArtists.first(),
            topSong = topSongs.first(),
            mostActiveMonth = mostActiveMonth?.let {
                java.text.DateFormatSymbols().months[it.month - 1]
            } ?: "N/A",
            topArtists = topArtists,
            topSongs = topSongs,
            totalAlbums = songRepository.getAlbumCount()
        )
    }

    private suspend fun legacyStats(year: Int): MusicStats {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        val startOfYear = calendar.timeInMillis
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        val endOfYear = calendar.timeInMillis

        return MusicStats(
            totalScrobbles = scrobbleRepository.getScrobbleCount(),
            totalListeningTimeMs = scrobbleRepository.getTotalListeningTime(),
            totalSongs = songRepository.getSongCount(),
            totalArtists = songRepository.getArtistCount(),
            totalAlbums = songRepository.getAlbumCount(),
            topSongs = scrobbleRepository.getTopSongs(startOfYear, endOfYear, 10),
            topArtists = scrobbleRepository.getTopArtists(startOfYear, endOfYear, 10),
            topAlbums = scrobbleRepository.getTopAlbums(startOfYear, endOfYear, 10),
            monthlyStats = getMonthlyStats(year),
            weeklyStats = getWeeklyStats(year)
        )
    }

    private suspend fun getMonthlyStatsFromSessions(year: Int): List<MonthStats> {
        val stats = mutableListOf<MonthStats>()
        for (month in 1..12) {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            val startOfMonth = calendar.timeInMillis
            calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val endOfMonth = calendar.timeInMillis

            val sessions = listeningSessionRepository.getSessionsBetween(startOfMonth, endOfMonth)
            if (sessions.isNotEmpty()) {
                val topSong = listeningSessionRepository.getTopSongs(startOfMonth, endOfMonth, 1)
                val topArtist = listeningSessionRepository.getTopArtists(startOfMonth, endOfMonth, 1)
                val totalTime = sessions.sumOf { it.duration }

                stats.add(
                    MonthStats(
                        month = month,
                        year = year,
                        totalScrobbles = sessions.size,
                        totalListeningTimeMs = totalTime,
                        topSong = topSong.firstOrNull()?.songTitle,
                        topArtist = topArtist.firstOrNull()?.artist
                    )
                )
            }
        }
        return stats
    }

    private suspend fun getWeeklyStatsFromSessions(year: Int): List<WeekStats> {
        val stats = mutableListOf<WeekStats>()
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)

        while (calendar.get(Calendar.YEAR) == year) {
            val weekStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val weekEnd = calendar.timeInMillis

            val sessions = listeningSessionRepository.getSessionsBetween(weekStart, weekEnd)
            if (sessions.isNotEmpty()) {
                stats.add(
                    WeekStats(
                        weekStart = weekStart,
                        totalScrobbles = sessions.size,
                        totalListeningTimeMs = sessions.sumOf { it.duration }
                    )
                )
            }
        }
        return stats
    }

    private suspend fun getMonthlyStats(year: Int): List<MonthStats> {
        val stats = mutableListOf<MonthStats>()
        for (month in 1..12) {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            val startOfMonth = calendar.timeInMillis
            calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val endOfMonth = calendar.timeInMillis

            val scrobbles = scrobbleRepository.getScrobblesBetween(startOfMonth, endOfMonth)
            if (scrobbles.isNotEmpty()) {
                val topSong = scrobbleRepository.getTopSongs(startOfMonth, endOfMonth, 1)
                val topArtist = scrobbleRepository.getTopArtists(startOfMonth, endOfMonth, 1)

                stats.add(
                    MonthStats(
                        month = month,
                        year = year,
                        totalScrobbles = scrobbles.size,
                        totalListeningTimeMs = scrobbles.size * 180000L,
                        topSong = topSong.firstOrNull()?.title,
                        topArtist = topArtist.firstOrNull()?.artist
                    )
                )
            }
        }
        return stats
    }

    private suspend fun getWeeklyStats(year: Int): List<WeekStats> {
        val stats = mutableListOf<WeekStats>()
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)

        while (calendar.get(Calendar.YEAR) == year) {
            val weekStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val weekEnd = calendar.timeInMillis

            val scrobbles = scrobbleRepository.getScrobblesBetween(weekStart, weekEnd)
            if (scrobbles.isNotEmpty()) {
                stats.add(
                    WeekStats(
                        weekStart = weekStart,
                        totalScrobbles = scrobbles.size,
                        totalListeningTimeMs = scrobbles.size * 180000L
                    )
                )
            }
        }
        return stats
    }
}

// Extension functions to convert new session types to legacy types for UI compatibility
fun com.michu117.michivibes.data.dao.ListeningSessionStat.toSongWithPlayCount(): com.michu117.michivibes.data.models.SongWithPlayCount =
    com.michu117.michivibes.data.models.SongWithPlayCount(
        id = "$artist - $songTitle",
        title = songTitle, artist = artist, album = album,
        duration = duration, playCount = playCount
    )

fun com.michu117.michivibes.data.dao.ArtistSessionStat.toArtistStats(): com.michu117.michivibes.data.models.ArtistStats =
    com.michu117.michivibes.data.models.ArtistStats(
        artist = artist, playCount = playCount, totalDuration = totalDuration
    )

fun com.michu117.michivibes.data.dao.AlbumSessionStat.toAlbumStats(): com.michu117.michivibes.data.models.AlbumStats =
    com.michu117.michivibes.data.models.AlbumStats(
        album = album, artist = artist, playCount = playCount
    )
