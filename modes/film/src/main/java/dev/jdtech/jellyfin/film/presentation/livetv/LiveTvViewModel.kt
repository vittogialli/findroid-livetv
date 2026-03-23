package dev.jdtech.jellyfin.film.presentation.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.LiveTvRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class LiveTvViewModel
@Inject
constructor(
    private val liveTvRepository: LiveTvRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LiveTvState())
    val state = _state.asStateFlow()

    fun onAction(action: LiveTvAction) {
        when (action) {
            is LiveTvAction.OnRetryClick -> loadData()
        }
    }

    fun loadData() {
        Timber.i("Loading live TV data")
        viewModelScope.launch(Dispatchers.Default) {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                val channels = liveTvRepository.getChannels()
                _state.emit(_state.value.copy(channels = channels, isLoading = false))
            } catch (e: Exception) {
                Timber.e(e)
                _state.emit(_state.value.copy(error = e, isLoading = false))
            }
        }
    }
}
