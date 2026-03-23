package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.FindroidChannel
import dev.jdtech.jellyfin.models.FindroidSource
import java.util.UUID

interface LiveTvRepository {
    suspend fun getChannels(): List<FindroidChannel>
    suspend fun getChannel(channelId: UUID): FindroidChannel
    suspend fun getLiveMediaSources(channelId: UUID): List<FindroidSource>
    suspend fun closeLiveStream(liveStreamId: String)
}
