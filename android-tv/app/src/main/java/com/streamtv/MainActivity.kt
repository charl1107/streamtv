package com.streamtv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamtv.ui.screens.DetailScreen
import com.streamtv.ui.screens.HomeScreen
import com.streamtv.ui.screens.PlayerActivity
import com.streamtv.ui.theme.StreamTvTheme
import com.streamtv.ui.viewmodel.DetailViewModel
import com.streamtv.ui.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StreamTvTheme {
                StreamTvNavHost(
                    homeViewModel = homeViewModel,
                    onPlayVideo = { url, title ->
                        val intent = Intent(this, PlayerActivity::class.java).apply {
                            putExtra(PlayerActivity.EXTRA_VIDEO_URL, url)
                            putExtra(PlayerActivity.EXTRA_TITLE, title)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun StreamTvNavHost(
    homeViewModel: HomeViewModel,
    onPlayVideo: (url: String, title: String) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onContentClick = { contentId, contentType, addonUrl ->
                    navController.navigate("detail/$contentId/$contentType/${Uri.encode(addonUrl)}")
                }
            )
        }

        composable(
            route = "detail/{contentId}/{contentType}/{addonUrl}",
            arguments = listOf(
                navArgument("contentId") { type = NavType.StringType },
                navArgument("contentType") { type = NavType.StringType },
                navArgument("addonUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId") ?: return@composable
            val contentType = backStackEntry.arguments?.getString("contentType") ?: return@composable
            val addonUrl = backStackEntry.arguments?.getString("addonUrl") ?: return@composable

            val detailViewModel: DetailViewModel = org.koin.java.KoinJavaComponent.get(
                clazz = DetailViewModel::class.java,
                parameters = { parametersOf(contentId, contentType, addonUrl) }
            )

            // Collect one-shot play events from the ViewModel
            LaunchedEffect(Unit) {
                detailViewModel.playEvents.collect { event ->
                    onPlayVideo(event.streamUrl, event.title)
                }
            }

            DetailScreen(
                viewModel = detailViewModel,
                onEpisodeClick = { episodeId, episodeTitle ->
                    detailViewModel.playEpisode(episodeId, episodeTitle)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
