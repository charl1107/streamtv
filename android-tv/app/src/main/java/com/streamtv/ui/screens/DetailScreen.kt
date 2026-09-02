package com.streamtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamtv.domain.model.Meta
import com.streamtv.ui.components.EpisodeRow
import com.streamtv.ui.components.ErrorMessage
import com.streamtv.ui.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onEpisodeClick: (episodeId: String, episodeTitle: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = Color.Gray, fontSize = 20.sp)
            }
        }
        uiState.error != null -> {
            ErrorMessage(
                message = uiState.error ?: "Unknown error",
                onRetry = { viewModel.loadDetails() }
            )
        }
        uiState.meta != null -> {
            DetailContent(
                meta = uiState.meta!!,
                onEpisodeClick = onEpisodeClick,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun DetailContent(
    meta: Meta,
    onEpisodeClick: (episodeId: String, episodeTitle: String) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Hero background
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(meta.background ?: meta.poster)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.3f
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
        )

        // Content
        TvLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Back button
            item {
                Text(
                    text = "← Back",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onBack() }
                        .focusable()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Poster
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(meta.poster)
                            .crossfade(true)
                            .build(),
                        contentDescription = meta.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(200.dp)
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    // Info
                    Column(
                        modifier = Modifier.padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = meta.name,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (meta.year != null) {
                            Text(
                                text = meta.year,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }

                        if (meta.genres.isNotEmpty()) {
                            Text(
                                text = meta.genres.joinToString(" · "),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        if (meta.runtime != null) {
                            Text(
                                text = meta.runtime,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = meta.description ?: "",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Episodes section
            if (meta.videos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Episodes (${meta.videos.size})",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(meta.videos) { video ->
                    EpisodeRow(
                        episodeNumber = video.episode,
                        title = video.title,
                        onClick = {
                            onEpisodeClick(video.id, video.title)
                        }
                    )
                }
            }
        }
    }
}
