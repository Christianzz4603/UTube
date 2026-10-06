package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChannelEntity
import com.example.data.formatCompactCount
import com.example.ui.SubFilter
import com.example.ui.YouTubeTab
import com.example.ui.YouTubeViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: YouTubeViewModel,
    onEnterCast: () -> Unit
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val subscribedChannels by viewModel.subscribedChannels.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val communityPosts by viewModel.communityPosts.collectAsState()
    val selectedChannelId by viewModel.selectedSubChannelId.collectAsState()
    val selectedFilter by viewModel.selectedSubFilter.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()

    var showManageAllSheet by remember { mutableStateOf(false) }

    val unreadCount = notifications.count { !it.isRead }
    val subIds = subscribedChannels.map { it.id }.toSet()

    val subVideos = remember(allVideos, subIds, selectedChannelId, selectedFilter) {
        allVideos.filter { video ->
            val matchesChannel = if (selectedChannelId != null) {
                video.channelId == selectedChannelId
            } else {
                video.channelId in subIds
            }
            val matchesFilter = when (selectedFilter) {
                SubFilter.ALL -> !video.isShort
                SubFilter.TODAY -> video.publishedTimeText.contains("hour") || video.publishedTimeText.contains("day")
                SubFilter.VIDEOS -> !video.isShort && !video.isLive
                SubFilter.SHORTS -> video.isShort
                SubFilter.LIVE -> video.isLive
                SubFilter.POSTS -> false
            }
            matchesChannel && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subscriptions_screen")
    ) {
        YouTubeTopBar(
            unreadNotificationsCount = unreadCount,
            isIncognito = isIncognito,
            onCastClick = onEnterCast,
            onNotificationsClick = { viewModel.openNotifications() },
            onSearchClick = { viewModel.openSearch() },
            onLogoClick = { viewModel.selectTab(YouTubeTab.HOME) }
        )

        // Subscribed Channels Avatar Row + "All" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(subscribedChannels, key = { it.id }) { channel ->
                    val isSelected = selectedChannelId == channel.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .clickable { viewModel.selectSubChannel(channel.id) }
                            .testTag("sub_avatar_${channel.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ChannelAvatarBadge(
                                name = channel.name,
                                colorLong = channel.avatarColor,
                                size = 48.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            TextButton(
                onClick = { showManageAllSheet = true },
                modifier = Modifier.testTag("manage_all_subs_button")
            ) {
                Text(
                    text = "All",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sub-filter Chips: All, Today, Videos, Shorts, Live, Posts
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            val filters = listOf(
                SubFilter.ALL to "All",
                SubFilter.TODAY to "Today",
                SubFilter.VIDEOS to "Videos",
                SubFilter.SHORTS to "Shorts",
                SubFilter.LIVE to "Live",
                SubFilter.POSTS to "Posts"
            )
            items(filters) { (filter, label) ->
                val isSelected = selectedFilter == filter
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { viewModel.selectSubFilter(filter) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (selectedFilter == SubFilter.POSTS) {
                items(communityPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        onVote = { idx -> viewModel.voteOnPoll(post, idx) },
                        onLike = { viewModel.togglePostLike(post) },
                        onChannelClick = { viewModel.openChannelProfile(post.channelId) }
                    )
                }
            } else {
                items(subVideos, key = { it.id }) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onVideoClick = { viewModel.openVideo(video) },
                        onChannelClick = { viewModel.openChannelProfile(video.channelId) },
                        onSaveToWatchLater = { viewModel.toggleWatchLater(video) },
                        onSaveToPlaylist = { viewModel.openSaveToPlaylistDialog(video.id) },
                        onDownloadVideo = { viewModel.toggleDownload(video) }
                    )
                }

                if (selectedFilter == SubFilter.ALL && communityPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Latest Community Posts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(communityPosts, key = { "post_${it.id}" }) { post ->
                        CommunityPostCard(
                            post = post,
                            onVote = { idx -> viewModel.voteOnPoll(post, idx) },
                            onLike = { viewModel.togglePostLike(post) },
                            onChannelClick = { viewModel.openChannelProfile(post.channelId) }
                        )
                    }
                }
            }
        }
    }

    if (showManageAllSheet) {
        ModalBottomSheet(
            onDismissRequest = { showManageAllSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "All Channels & Subscriptions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(allChannels, key = { it.id }) { channel ->
                        ManageChannelRow(
                            channel = channel,
                            onToggleSub = { viewModel.toggleSubscription(channel) },
                            onCycleBell = { viewModel.cycleBellMode(channel) },
                            onOpenChannel = {
                                showManageAllSheet = false
                                viewModel.openChannelProfile(channel.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageChannelRow(
    channel: ChannelEntity,
    onToggleSub: () -> Unit,
    onCycleBell: () -> Unit,
    onOpenChannel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenChannel() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelAvatarBadge(
            name = channel.name,
            colorLong = channel.avatarColor,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${channel.handle} • ${formatCompactCount(channel.subscriberCount)} subscribers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (channel.isSubscribed) {
            IconButton(onClick = onCycleBell) {
                Icon(
                    imageVector = when (channel.notificationBellMode) {
                        "ALL" -> Icons.Filled.NotificationsActive
                        "NONE" -> Icons.Outlined.NotificationsOff
                        else -> Icons.Outlined.Notifications
                    },
                    contentDescription = "Notification Bell",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Surface(
            color = if (channel.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onBackground,
            contentColor = if (channel.isSubscribed) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .height(34.dp)
                .clickable { onToggleSub() }
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (channel.isSubscribed) "Subscribed" else "Subscribe",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
