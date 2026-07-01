package com.michu117.michivibes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.michu117.michivibes.MusicWrappedApp
import com.michu117.michivibes.ui.charts.ChartsScreen
import com.michu117.michivibes.ui.dashboard.DashboardScreen
import com.michu117.michivibes.ui.dashboard.DashboardViewModel
import com.michu117.michivibes.ui.import.ImportScreen
import com.michu117.michivibes.ui.import.ImportViewModel
import com.michu117.michivibes.ui.settings.SettingsScreen
import com.michu117.michivibes.ui.settings.SettingsViewModel
import com.michu117.michivibes.ui.topalbums.TopAlbumsScreen
import com.michu117.michivibes.ui.topartists.TopArtistsScreen
import com.michu117.michivibes.ui.topsongs.TopSongsScreen
import com.michu117.michivibes.ui.wrapped.WrappedScreen
import com.michu117.michivibes.ui.wrapped.WrappedViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val TOP_SONGS = "top_songs"
    const val TOP_ARTISTS = "top_artists"
    const val TOP_ALBUMS = "top_albums"
    const val CHARTS = "charts"
    const val IMPORT = "import"
    const val WRAPPED = "wrapped"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val app = MusicWrappedApp.instance

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(app.getStatisticsUseCase, app.dataStoreManager)
            )
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToTopSongs = { navController.navigate(Routes.TOP_SONGS) },
                onNavigateToTopArtists = { navController.navigate(Routes.TOP_ARTISTS) },
                onNavigateToTopAlbums = { navController.navigate(Routes.TOP_ALBUMS) },
                onNavigateToCharts = { navController.navigate(Routes.CHARTS) },
                onNavigateToImport = { navController.navigate(Routes.IMPORT) },
                onNavigateToWrapped = { navController.navigate(Routes.WRAPPED) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.TOP_SONGS) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(app.getStatisticsUseCase, app.dataStoreManager)
            )
            TopSongsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TOP_ARTISTS) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(app.getStatisticsUseCase, app.dataStoreManager)
            )
            TopArtistsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TOP_ALBUMS) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(app.getStatisticsUseCase, app.dataStoreManager)
            )
            TopAlbumsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CHARTS) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(app.getStatisticsUseCase, app.dataStoreManager)
            )
            ChartsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.IMPORT) {
            val viewModel: ImportViewModel = viewModel(
                factory = ImportViewModel.Factory()
            )
            ImportScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.WRAPPED) {
            val viewModel: WrappedViewModel = viewModel(
                factory = WrappedViewModel.Factory(app.getStatisticsUseCase, app.dataStoreManager)
            )
            WrappedScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.dataStoreManager, app.database)
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
