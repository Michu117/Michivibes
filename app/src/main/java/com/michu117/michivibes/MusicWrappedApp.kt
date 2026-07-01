package com.michu117.michivibes

import android.app.Application
import com.michu117.michivibes.data.database.AppDatabase
import com.michu117.michivibes.data.repository.ListeningSessionRepository
import com.michu117.michivibes.data.repository.ScrobbleRepository
import com.michu117.michivibes.data.repository.SongRepository
import com.michu117.michivibes.domain.usecases.GetStatisticsUseCase
import com.michu117.michivibes.service.MediaSessionMonitorService
import com.michu117.michivibes.service.NowPlayingHolder
import com.michu117.michivibes.service.NowPlayingInfo
import com.michu117.michivibes.utils.CsvImporter
import com.michu117.michivibes.utils.DataStoreManager
import com.michu117.michivibes.utils.JsonImporter
import kotlinx.coroutines.flow.StateFlow

class MusicWrappedApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var songRepository: SongRepository
        private set
    lateinit var scrobbleRepository: ScrobbleRepository
        private set
    lateinit var listeningSessionRepository: ListeningSessionRepository
        private set
    lateinit var getStatisticsUseCase: GetStatisticsUseCase
        private set
    lateinit var dataStoreManager: DataStoreManager
        private set
    val csvImporter: CsvImporter by lazy { CsvImporter() }
    val jsonImporter: JsonImporter by lazy { JsonImporter() }

    val nowPlayingFlow: StateFlow<NowPlayingInfo> = NowPlayingHolder.nowPlaying

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        songRepository = SongRepository(database.songDao())
        scrobbleRepository = ScrobbleRepository(database.scrobbleDao())
        listeningSessionRepository = ListeningSessionRepository(database.listeningSessionDao())
        getStatisticsUseCase = GetStatisticsUseCase(
            songRepository, scrobbleRepository, listeningSessionRepository
        )
        dataStoreManager = DataStoreManager(this)
        startMediaMonitoring()
    }

    fun startMediaMonitoring() {
        MediaSessionMonitorService.start(this)
    }

    fun stopMediaMonitoring() {
        MediaSessionMonitorService.stop(this)
    }

    companion object {
        lateinit var instance: MusicWrappedApp
            private set
    }
}
