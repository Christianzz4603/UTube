package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.YouTubeTab
import com.example.ui.YouTubeViewModel
import com.example.ui.components.*

@Composable
fun HomeScreen(
    viewModel: YouTubeViewModel,
    onEnterCast: () -> Unit
) {
    val homeVideos by viewModel.filteredHomeVideos.collectAsState()
    val shortsVideos by viewModel.shortsVideos.collectAsState()
    val communityPosts by viewModel.communityPosts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val isInlineMutedPreview by viewModel.isInlineMutedPreview.collectAsState()

    val unreadCount = notifications.count { !it.isRead }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        YouTubeTopBar(
            unreadNotificationsCount = unreadCount,
            isIncognito = isIncognito,
            onCastClick = onEnterCast,
            onNotificationsClick = { viewModel.openNotifications() },
            onSearchClick = { viewModel.openSearch() },
            onLogoClick = { viewModel.selectCategory("All") }
        )

        CategoryFilterChipsRow(
            categories = viewModel.categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { viewModel.selectCategory(it) },
            onExploreShortsClick = { viewModel.openExploreTrending() }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        if (homeVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No videos in \"$selectedCategory\" yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try selecting another filter chip or upload your own video with the + button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.selectCategory("All") }) {
                        Text("Show All Videos")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_video_feed"),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                itemsIndexed(homeVideos, key = { _, item -> item.id }) { index, video ->
                    YouTubeVideoCard(
                        video = video,
                        onVideoClick = { viewModel.openVideo(video) },
                        onChannelClick = { viewModel.openChannelProfile(video.channelId) },
                        onSaveToWatchLater = { viewModel.toggleWatchLater(video) },
                        onSaveToPlaylist = { viewModel.openSaveToPlaylistDialog(video.id) },
                        onDownloadVideo = { viewModel.toggleDownload(video) },
                        onPlayNextInQueue = { viewModel.playNextInQueue(video) },
                        onNotInterested = { viewModel.markNotInterested(video.id) },
                        showInlineMutedBadge = isInlineMutedPreview && index == 0
                    )

                    // Insert authentic Shorts Shelf after 2nd video when viewing "All" or "New to you"
                    if (index == 1 && (selectedCategory == "All" || selectedCategory == "New to you")) {
                        ShortsShelfSection(
                            shorts = shortsVideos,
                            onShortClick = { viewModel.selectTab(YouTubeTab.SHORTS) }
                        )
                    }

                    // Insert Community Post card after 4th video
                    if (index == 3 && communityPosts.isNotEmpty() && selectedCategory == "All") {
                        val firstPost = communityPosts.first()
                        CommunityPostCard(
                            post = firstPost,
                            onVote = { optionIdx -> viewModel.voteOnPoll(firstPost, optionIdx) },
                            onLike = { viewModel.togglePostLike(firstPost) },
                            onChannelClick = { viewModel.openChannelProfile(firstPost.channelId) }
                        )
                    }
                }
            }
        }
    }
}
