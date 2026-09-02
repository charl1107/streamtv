package com.streamtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamtv.domain.model.Meta
import com.streamtv.domain.usecase.GetMetaUseCase
import com.streamtv.domain.usecase.GetStreamsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val meta: Meta? = null,
    val error: String? = null
)

/** One-shot event: tells the UI to play a video. */
data class PlayEvent(
    val streamUrl: String,
    val title: String
)

class DetailViewModel(
    private val getMetaUseCase: GetMetaUseCase,
    private val getStreamsUseCase: GetStreamsUseCase,
    private val contentId: String,
    private val contentType: String,
    private val addonUrl: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /** Emits once per play request — the UI collects this to launch the player. */
    private val _playEvents = MutableSharedFlow<PlayEvent>(extraBufferCapacity = 1)
    val playEvents: SharedFlow<PlayEvent> = _playEvents.asSharedFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val meta = getMetaUseCase(contentType, contentId)
                _uiState.value = DetailUiState(
                    isLoading = false,
                    meta = meta
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load details"
                )
            }
        }
    }

    /**
     * Fetch streams for a specific episode, then emit a PlayEvent
     * with the first available stream URL.
     */
    fun playEpisode(episodeId: String, episodeTitle: String) {
        viewModelScope.launch {
            try {
                val imdbId = _uiState.value.meta?.imdbId
                val streams = getStreamsUseCase(
                    type = contentType,
                    contentId = episodeId,
                    imdbId = imdbId
                )
                val streamUrl = streams.firstOrNull()?.url
                if (streamUrl != null) {
                    _playEvents.emit(PlayEvent(streamUrl, episodeTitle))
                }
            } catch (e: Exception) {
                // Silently fail — error state is not critical for playback
            }
        }
    }
}
