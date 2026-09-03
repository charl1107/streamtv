package com.streamtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import com.streamtv.domain.model.CatalogGroup
import com.streamtv.ui.components.ErrorMessage
import com.streamtv.ui.components.LoadingShimmer
import com.streamtv.ui.components.PosterCard
import com.streamtv.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onContentClick: (contentId: String, contentType: String, addonUrl: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            text = "Stream TV",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Always show debug info
        Text(
            text = uiState.debugInfo,
            color = Color(0xFFE94560),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            uiState.isLoading -> {
                LoadingShimmer()
            }
            uiState.error != null -> {
                ErrorMessage(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadCatalogs() }
                )
            }
            else -> {
                HomeContent(
                    catalogGroups = uiState.catalogGroups,
                    onContentClick = onContentClick
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    catalogGroups: List<CatalogGroup>,
    onContentClick: (contentId: String, contentType: String, addonUrl: String) -> Unit
) {
    TvLazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(catalogGroups) { group ->
            CatalogRow(
                group = group,
                onContentClick = onContentClick
            )
        }
    }
}

@Composable
private fun CatalogRow(
    group: CatalogGroup,
    onContentClick: (contentId: String, contentType: String, addonUrl: String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "${group.catalogName} (${group.addonName})",
            color = Color.Gray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TvLazyRow(
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            items(group.metas) { item ->
                PosterCard(
                    title = item.name,
                    posterUrl = item.poster,
                    onClick = {
                        onContentClick(item.id, item.type, group.addonUrl)
                    }
                )
            }
        }
    }
}
