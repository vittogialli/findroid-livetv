package dev.jdtech.jellyfin.presentation.film

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.livetv.LiveTvAction
import dev.jdtech.jellyfin.film.presentation.livetv.LiveTvState
import dev.jdtech.jellyfin.film.presentation.livetv.LiveTvViewModel
import dev.jdtech.jellyfin.models.FindroidChannel
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.time.Duration
import java.time.LocalDateTime
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadData() }

    LiveTvScreenLayout(
        state = state,
        onAction = { action -> viewModel.onAction(action) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveTvScreenLayout(
    state: LiveTvState,
    onAction: (LiveTvAction) -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(CoreR.string.live_tv)) },
                windowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading && state.channels.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && state.channels.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(CoreR.string.error_loading_data),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                        TextButton(onClick = { onAction(LiveTvAction.OnRetryClick) }) {
                            Text(text = stringResource(CoreR.string.retry))
                        }
                    }
                }
                state.channels.isEmpty() -> {
                    Text(
                        text = stringResource(CoreR.string.no_channels),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isLoading,
                        onRefresh = { onAction(LiveTvAction.OnRetryClick) },
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                horizontal = MaterialTheme.spacings.medium,
                                vertical = MaterialTheme.spacings.small,
                            ),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                        ) {
                            items(state.channels, key = { it.id.toString() }) { channel ->
                                ChannelListItem(
                                    channel = channel,
                                    onClick = {
                                        val intent = Intent(context, PlayerActivity::class.java)
                                        intent.putExtra("itemId", channel.id.toString())
                                        intent.putExtra("itemKind", BaseItemKind.LIVE_TV_CHANNEL.serialName)
                                        intent.putExtra("startFromBeginning", false)
                                        context.startActivity(intent)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelListItem(
    channel: FindroidChannel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(MaterialTheme.spacings.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp),
        ) {
            if (channel.images.primary != null) {
                AsyncImage(
                    model = channel.images.primary,
                    contentDescription = channel.name,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = channel.number ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val number = channel.number
                if (number != null) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val programName = channel.currentProgramName
            if (programName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = programName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val endDate = channel.currentProgramEndDate
                if (endDate != null) {
                    val remaining = Duration.between(LocalDateTime.now(), endDate)
                    if (!remaining.isNegative) {
                        val minutes = remaining.toMinutes()
                        Text(
                            text = stringResource(CoreR.string.runtime_minutes_left, minutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(CoreR.string.no_program_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
