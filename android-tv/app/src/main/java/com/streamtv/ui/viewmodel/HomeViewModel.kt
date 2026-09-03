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

private const val TAG = "HomeViewModel"

data class HomeUiState(
    val isLoading: Boolean = true,
    val catalogGroups: List<CatalogGroup> = emptyList(),
    val error: String? = null
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val groups = getCatalogUseCase()
                Log.d(TAG, "Loaded ${groups.size} catalog groups")
                _uiState.value = HomeUiState(
                    isLoading = false,
                    catalogGroups = groups
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load catalogs", e)
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load catalogs"
                )
            }
        }
    }
}
