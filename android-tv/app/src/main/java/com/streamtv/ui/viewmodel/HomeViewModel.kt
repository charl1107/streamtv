package com.streamtv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamtv.domain.model.CatalogGroup
import com.streamtv.domain.usecase.GetCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "HomeVM"

data class HomeUiState(
    val isLoading: Boolean = true,
    val catalogGroups: List<CatalogGroup> = emptyList(),
    val error: String? = null,
    val debugInfo: String = "Starting..."
)

class HomeViewModel(
    private val getCatalogUseCase: GetCatalogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalogs()
    }

    fun loadCatalogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                debugInfo = "Loading catalogs..."
            )
            Log.d(TAG, "loadCatalogs() called")
            try {
                val groups = getCatalogUseCase()
                Log.d(TAG, "Loaded ${groups.size} catalog groups, ${groups.sumOf { it.metas.size }} total items")
                _uiState.value = HomeUiState(
                    isLoading = false,
                    catalogGroups = groups,
                    debugInfo = "Loaded ${groups.size} groups, ${groups.sumOf { it.metas.size }} items"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load catalogs: ${e.message}", e)
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load catalogs",
                    debugInfo = "ERROR: ${e.message}"
                )
            }
        }
    }
}
