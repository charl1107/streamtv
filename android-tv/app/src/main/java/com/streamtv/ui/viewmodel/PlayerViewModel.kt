package com.streamtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.streamtv.domain.model.Stream
import com.streamtv.domain.usecase.GetStreamsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class PlayerUiState(
    val isLoading: Boolean = true,
    val streams: List<Stream> = emptyList(),
    val currentStreamIndex: Int = 0,
    val error: String? = null
)

class PlayerViewModel(
    private val getStreamsUseCase: GetStreamsUseCase,
    private val contentId: String,
    private val contentType: String,
    private val title: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val contentTitle: String get() = title

    init {
        loadStreams()
    }

    fun loadStreams() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val streams = getStreamsUseCase(contentType, contentId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streams = streams
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load streams"
                )
            }
        }
    }

    fun selectStream(index: Int) {
        _uiState.value = _uiState.value.copy(currentStreamIndex = index)
    }

    fun getCurrentStream(): Stream? {
        val state = _uiState.value
        return state.streams.getOrNull(state.currentStreamIndex)
    }
}
