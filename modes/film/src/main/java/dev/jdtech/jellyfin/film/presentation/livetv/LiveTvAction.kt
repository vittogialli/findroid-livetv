package dev.jdtech.jellyfin.film.presentation.livetv

sealed interface LiveTvAction {
    data object OnRetryClick : LiveTvAction
}
