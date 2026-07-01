package com.michu117.michivibes.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.michu117.michivibes.data.dao.ListeningSessionDao
import com.michu117.michivibes.data.dao.ScrobbleDao
import com.michu117.michivibes.data.dao.SongDao
import com.michu117.michivibes.data.models.ListeningSession
import com.michu117.michivibes.data.models.Scrobble
import com.michu117.michivibes.data.models.Song

@Database(
    entities = [Song::class, Scrobble::class, ListeningSession::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun scrobbleDao(): ScrobbleDao
    abstract fun listeningSessionDao(): ListeningSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        duration INTEGER NOT NULL,
                        playedAt INTEGER NOT NULL,
                        sourcePlayer TEXT NOT NULL,
                        albumArtUri TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_listening_sessions_playedAt ON listening_sessions(playedAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_listening_sessions_artist ON listening_sessions(artist)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_listening_sessions_title ON listening_sessions(title)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_wrapped_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
