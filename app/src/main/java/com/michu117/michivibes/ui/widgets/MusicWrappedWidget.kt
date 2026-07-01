package com.michu117.michivibes.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.michu117.michivibes.MusicWrappedApp
import com.michu117.michivibes.R
import com.michu117.michivibes.utils.DateUtils
import kotlinx.coroutines.runBlocking

abstract class BaseMusicWidget : AppWidgetProvider() {
    protected fun getStats(context: Context) {
        val app = context.applicationContext as MusicWrappedApp
        runBlocking {
            try {
                val stats = app.getStatisticsUseCase.getDashboardStats()
                updateWidgetWithStats(context, stats)
            } catch (_: Exception) {
                updateWidgetWithError(context)
            }
        }
    }

    abstract fun updateWidgetWithStats(context: Context, stats: Any)
    abstract fun updateWidgetWithError(context: Context)
}

class MusicWrappedWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as MusicWrappedApp

        runBlocking {
            try {
                val stats = app.getStatisticsUseCase.getDashboardStats()
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(
                        context.packageName,
                        R.layout.widget_music_wrapped
                    )

                    views.setTextViewText(
                        R.id.widget_title,
                        context.getString(R.string.widget_today_title)
                    )
                    views.setTextViewText(
                        R.id.widget_plays,
                        "${stats.totalScrobbles} ${context.getString(R.string.widget_plays)}"
                    )
                    views.setTextViewText(
                        R.id.widget_time,
                        DateUtils.formatDuration(stats.totalListeningTimeMs)
                    )

                    val intent = context.packageManager.getLaunchIntentForPackage(
                        context.packageName
                    )
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(
                        context.packageName,
                        R.layout.widget_music_wrapped
                    )
                    views.setTextViewText(
                        R.id.widget_title,
                        context.getString(R.string.widget_no_data)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}

class TopSongWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as MusicWrappedApp

        runBlocking {
            try {
                val calendar = java.util.Calendar.getInstance()
                val year = calendar.get(java.util.Calendar.YEAR)
                calendar.set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
                val startOfYear = calendar.timeInMillis
                calendar.set(year, java.util.Calendar.DECEMBER, 31, 23, 59, 59)
                val endOfYear = calendar.timeInMillis

                val topSongs = app.scrobbleRepository.getTopSongs(startOfYear, endOfYear, 1)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(
                        context.packageName,
                        R.layout.widget_top_song
                    )

                    if (topSongs.isNotEmpty()) {
                        val song = topSongs.first()
                        views.setTextViewText(
                            R.id.widget_song_title,
                            song.title
                        )
                        views.setTextViewText(
                            R.id.widget_song_plays,
                            "${song.playCount} ${context.getString(R.string.widget_times)}"
                        )
                    } else {
                        views.setTextViewText(
                            R.id.widget_song_title,
                            context.getString(R.string.widget_no_data)
                        )
                    }

                    val intent = context.packageManager.getLaunchIntentForPackage(
                        context.packageName
                    )
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(
                        context.packageName,
                        R.layout.widget_top_song
                    )
                    views.setTextViewText(
                        R.id.widget_song_title,
                        context.getString(R.string.widget_no_data)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}

class TopArtistWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as MusicWrappedApp

        runBlocking {
            try {
                val calendar = java.util.Calendar.getInstance()
                val year = calendar.get(java.util.Calendar.YEAR)
                calendar.set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
                val startOfYear = calendar.timeInMillis
                calendar.set(year, java.util.Calendar.DECEMBER, 31, 23, 59, 59)
                val endOfYear = calendar.timeInMillis

                val topArtists = app.scrobbleRepository.getTopArtists(startOfYear, endOfYear, 1)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(
                        context.packageName,
                        R.layout.widget_top_artist
                    )

                    if (topArtists.isNotEmpty()) {
                        val artist = topArtists.first()
                        views.setTextViewText(
                            R.id.widget_artist_name,
                            artist.artist
                        )
                        views.setTextViewText(
                            R.id.widget_artist_plays,
                            "${artist.playCount} ${context.getString(R.string.widget_plays)}"
                        )
                    } else {
                        views.setTextViewText(
                            R.id.widget_artist_name,
                            context.getString(R.string.widget_no_data)
                        )
                    }

                    val intent = context.packageManager.getLaunchIntentForPackage(
                        context.packageName
                    )
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(
                        context.packageName,
                        R.layout.widget_top_artist
                    )
                    views.setTextViewText(
                        R.id.widget_artist_name,
                        context.getString(R.string.widget_no_data)
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
