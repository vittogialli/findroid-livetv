package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.time.LocalDateTime
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ChannelType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.PlayAccess

data class FindroidChannel(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String? = null,
    override val overview: String = "",
    override val sources: List<FindroidSource> = emptyList(),
    override val played: Boolean = false,
    override val favorite: Boolean = false,
    override val canPlay: Boolean = false,
    override val canDownload: Boolean = false,
    override val runtimeTicks: Long = 0,
    override val playbackPositionTicks: Long = 0,
    override val unplayedItemCount: Int? = null,
    override val images: FindroidImages = FindroidImages(),
    override val chapters: List<FindroidChapter> = emptyList(),
    val number: String?,
    val channelType: ChannelType?,
    val currentProgramName: String?,
    val currentProgramEndDate: LocalDateTime?,
) : FindroidItem

suspend fun BaseItemDto.toFindroidChannel(
    jellyfinRepository: JellyfinRepository,
): FindroidChannel {
    return toFindroidChannel(jellyfinRepository.getBaseUrl())
}

fun BaseItemDto.toFindroidChannel(
    baseUrl: String,
): FindroidChannel {
    val baseUri = Uri.parse(baseUrl)
    val primaryImage = imageTags?.get(ImageType.PRIMARY)?.let { tag ->
        baseUri
            .buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("tag", tag)
            .build()
    }
    return FindroidChannel(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        runtimeTicks = runTimeTicks ?: 0,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0,
        favorite = userData?.isFavorite == true,
        played = userData?.played == true,
        images = FindroidImages(primary = primaryImage),
        number = channelNumber,
        channelType = channelType,
        currentProgramName = currentProgram?.name,
        currentProgramEndDate = currentProgram?.endDate,
    )
}
