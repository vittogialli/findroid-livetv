package dev.jdtech.jellyfin.film.presentation.livetv

import dev.jdtech.jellyfin.models.FindroidChannel

data class LiveTvState(
    val channels: List<FindroidChannel> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
