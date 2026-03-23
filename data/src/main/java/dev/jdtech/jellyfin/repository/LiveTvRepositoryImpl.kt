package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.FindroidChannel
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.toFindroidChannel
import dev.jdtech.jellyfin.models.toFindroidMediaStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaStreamProtocol
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.SubtitleProfile
import org.jellyfin.sdk.model.api.TranscodingProfile
import timber.log.Timber

class LiveTvRepositoryImpl(
    private val jellyfinApi: JellyfinApi,
) : LiveTvRepository {

    private fun getBaseUrl(): String = jellyfinApi.api.baseUrl.orEmpty()

    override suspend fun getChannels(): List<FindroidChannel> =
        withContext(Dispatchers.IO) {
            jellyfinApi.liveTvApi
                .getLiveTvChannels(
                    userId = jellyfinApi.userId!!,
                    enableImages = true,
                    enableUserData = true,
                    sortOrder = SortOrder.ASCENDING,
                )
                .content
                .items
                .map { it.toFindroidChannel(getBaseUrl()) }
        }

    override suspend fun getChannel(channelId: UUID): FindroidChannel =
        withContext(Dispatchers.IO) {
            jellyfinApi.liveTvApi
                .getChannel(channelId, userId = jellyfinApi.userId!!)
                .content
                .toFindroidChannel(getBaseUrl())
        }

    override suspend fun getLiveMediaSources(channelId: UUID): List<FindroidSource> =
        withContext(Dispatchers.IO) {
            val baseUrl = getBaseUrl()
            val response =
                jellyfinApi.mediaInfoApi
                    .getPostedPlaybackInfo(
                        channelId,
                        PlaybackInfoDto(
                            userId = jellyfinApi.userId!!,
                            deviceProfile = buildLiveTvDeviceProfile(),
                            autoOpenLiveStream = true,
                            maxStreamingBitrate = MAX_BITRATE,
                        ),
                    )
                    .content

            response.mediaSources.map { mediaSourceInfo ->
                val path = when {
                    mediaSourceInfo.supportsTranscoding == true &&
                        mediaSourceInfo.transcodingUrl != null -> {
                        jellyfinApi.api.createUrl(mediaSourceInfo.transcodingUrl!!)
                    }
                    mediaSourceInfo.supportsDirectPlay == true &&
                        mediaSourceInfo.protocol == MediaProtocol.HTTP -> {
                        mediaSourceInfo.path.orEmpty()
                    }
                    mediaSourceInfo.protocol == MediaProtocol.FILE -> {
                        try {
                            jellyfinApi.videosApi.getVideoStreamUrl(
                                channelId,
                                static = false,
                                mediaSourceId = mediaSourceInfo.id,
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to get video stream URL for channel $channelId")
                            ""
                        }
                    }
                    else -> mediaSourceInfo.path.orEmpty()
                }

                FindroidSource(
                    id = mediaSourceInfo.id.orEmpty(),
                    name = mediaSourceInfo.name.orEmpty(),
                    type = FindroidSourceType.REMOTE,
                    path = path,
                    size = mediaSourceInfo.size ?: 0,
                    mediaStreams =
                        mediaSourceInfo.mediaStreams?.map {
                            it.toFindroidMediaStream(baseUrl)
                        } ?: emptyList(),
                    liveStreamId = mediaSourceInfo.liveStreamId,
                    playSessionId = response.playSessionId,
                )
            }
        }

    override suspend fun closeLiveStream(liveStreamId: String) {
        withContext(Dispatchers.IO) {
            try {
                jellyfinApi.mediaInfoApi.closeLiveStream(liveStreamId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to close live stream: $liveStreamId")
            }
        }
    }

    companion object {
        private const val MAX_BITRATE = 120_000_000

        fun buildLiveTvDeviceProfile(): DeviceProfile {
            return DeviceProfile(
                name = "Findroid Live TV",
                maxStreamingBitrate = MAX_BITRATE,
                maxStaticBitrate = 100_000_000,
                codecProfiles = emptyList(),
                containerProfiles = emptyList(),
                directPlayProfiles = listOf(
                    DirectPlayProfile(
                        type = DlnaProfileType.VIDEO,
                        container = "ts,mpegts",
                        videoCodec = "h264,hevc,mpeg2video",
                        audioCodec = "aac,ac3,eac3,mp3,mp2",
                    ),
                    DirectPlayProfile(
                        type = DlnaProfileType.VIDEO,
                        container = "mp4,mkv,fmp4",
                        videoCodec = "h264,hevc",
                        audioCodec = "aac,ac3,eac3,mp3",
                    ),
                    DirectPlayProfile(
                        type = DlnaProfileType.AUDIO,
                        container = "aac,mp3",
                    ),
                ),
                transcodingProfiles = listOf(
                    TranscodingProfile(
                        type = DlnaProfileType.VIDEO,
                        container = "ts",
                        videoCodec = "h264",
                        audioCodec = "aac",
                        protocol = MediaStreamProtocol.HLS,
                        conditions = emptyList(),
                    ),
                    TranscodingProfile(
                        type = DlnaProfileType.VIDEO,
                        container = "fmp4",
                        videoCodec = "h264",
                        audioCodec = "aac",
                        protocol = MediaStreamProtocol.HLS,
                        conditions = emptyList(),
                    ),
                ),
                subtitleProfiles = listOf(
                    SubtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL),
                    SubtitleProfile("ass", SubtitleDeliveryMethod.EXTERNAL),
                    SubtitleProfile("subrip", SubtitleDeliveryMethod.EXTERNAL),
                ),
            )
        }
    }
}
