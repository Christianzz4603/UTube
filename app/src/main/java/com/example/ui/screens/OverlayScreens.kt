package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.formatCompactCount
import com.example.ui.YouTubeViewModel
import com.example.ui.components.ChannelAvatarBadge
import com.example.ui.components.CommunityPostCard
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.theme.YouTubeRed

@Composable
fun SearchOverlayScreen(viewModel: YouTubeViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val durationFilter by viewModel.searchDurationFilter.collectAsState()

    BackHandler { viewModel.closeSearch() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().testTag("search_overlay_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.closeSearch() },
                    modifier = Modifier.testTag("search_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search YouTube") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("search_input_field"),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Any", "Under 4 mins", "4-20 mins", "Shorts")) { filter ->
                    FilterChip(
                        selected = durationFilter == filter,
                        onClick = { viewModel.setSearchDurationFilter(filter) },
                        label = { Text(filter) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                    items(searchHistory, key = { it.query }) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.submitSearch(historyItem.query) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = historyItem.query,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.deleteSearchHistoryItem(historyItem.query) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove from history",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                items(searchResults, key = { "res_${it.id}" }) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onVideoClick = {
                            viewModel.submitSearch(searchQuery.ifBlank { video.title })
                            viewModel.closeSearch()
                            viewModel.openVideo(video)
                        },
                        onChannelClick = {
                            viewModel.closeSearch()
                            viewModel.openChannelProfile(video.channelId)
                        },
                        onSaveToWatchLater = { viewModel.toggleWatchLater(video) },
                        onSaveToPlaylist = { viewModel.openSaveToPlaylistDialog(video.id) },
                        onDownloadVideo = { viewModel.toggleDownload(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsOverlayScreen(viewModel: YouTubeViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()

    BackHandler { viewModel.closeNotifications() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().testTag("notifications_overlay_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.closeNotifications() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                    Text("Mark all read")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    val targetVideo = allVideos.find { it.id == item.videoId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (!item.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.markNotificationRead(item.id)
                                viewModel.closeNotifications()
                                if (targetVideo != null) {
                                    viewModel.openVideo(targetVideo)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!item.isRead) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 14.dp, end = 8.dp)
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                        ChannelAvatarBadge(
                            name = item.channelName,
                            colorLong = item.channelAvatarColor,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.timeAgoText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (targetVideo != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                VideoThumbnailImage(
                                    video = targetVideo,
                                    modifier = Modifier.fillMaxSize()
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
fun ChannelProfileOverlayScreen(channelId: String, viewModel: YouTubeViewModel) {
    val allChannels by viewModel.allChannels.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val communityPosts by viewModel.communityPosts.collectAsState()

    val channel = allChannels.find { it.id == channelId }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    BackHandler { viewModel.closeChannelProfile() }

    if (channel == null) {
        viewModel.closeChannelProfile()
        return
    }

    val channelVideos = allVideos.filter { it.channelId == channelId && !it.isShort }
    val channelShorts = allVideos.filter { it.channelId == channelId && it.isShort }
    val channelPosts = communityPosts.filter { it.channelId == channelId }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().testTag("channel_profile_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(channel.bannerColor), Color(channel.avatarColor))
                            )
                        )
                ) {
                    IconButton(
                        onClick = { viewModel.closeChannelProfile() },
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatarBadge(name = channel.name, colorLong = channel.avatarColor, size = 68.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (channel.isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "${channel.handle} • ${formatCompactCount(channel.subscriberCount)} subscribers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = channel.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.toggleSubscription(channel) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (channel.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onBackground,
                            contentColor = if (channel.isSubscribed) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (channel.isSubscribed) "Subscribed" else "Subscribe",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    listOf("Videos", "Shorts", "Community").forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTabIndex == idx,
                            onClick = { selectedTabIndex = idx },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (selectedTabIndex) {
                0 -> items(channelVideos, key = { it.id }) { video ->
                    YouTubeVideoCard(
                        video = video,
                        onVideoClick = {
                            viewModel.closeChannelProfile()
                            viewModel.openVideo(video)
                        },
                        onChannelClick = {},
                        onSaveToWatchLater = { viewModel.toggleWatchLater(video) },
                        onSaveToPlaylist = { viewModel.openSaveToPlaylistDialog(video.id) },
                        onDownloadVideo = { viewModel.toggleDownload(video) }
                    )
                }
                1 -> items(channelShorts, key = { it.id }) { shortVid ->
                    CompactVideoRow(
                        video = shortVid,
                        onClick = {
                            viewModel.closeChannelProfile()
                            viewModel.openVideo(shortVid)
                        }
                    )
                }
                2 -> items(channelPosts, key = { it.id }) { post ->
                    CommunityPostCard(
                        post = post,
                        onVote = { idx -> viewModel.voteOnPoll(post, idx) },
                        onLike = { viewModel.togglePostLike(post) },
                        onChannelClick = {}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStudioBottomSheet(viewModel: YouTubeViewModel) {
    var mode by remember { mutableStateOf("VIDEO") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tech") }
    var customThumbnailUri by remember { mutableStateOf("") }
    var thumbnailPreset by remember { mutableStateOf("img_thumb_tech_1791326396393") }
    var postContent by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) customThumbnailUri = uri.toString()
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeCreateSheet() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("create_studio_sheet")
        ) {
            Text("Create on YouTube", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == "VIDEO",
                    onClick = { mode = "VIDEO" },
                    label = { Text("Upload video") }
                )
                FilterChip(
                    selected = mode == "SHORT",
                    onClick = { mode = "SHORT" },
                    label = { Text("Create Short") }
                )
                FilterChip(
                    selected = mode == "POST",
                    onClick = { mode = "POST" },
                    label = { Text("Create post") }
                )
                AssistChip(
                    onClick = { viewModel.openGoLive() },
                    label = { Text("Go Live") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Sensors,
                            contentDescription = null,
                            tint = YouTubeRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (mode == "POST") {
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { postContent = it },
                    label = { Text("Share an update or poll with subscribers...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pollOption1,
                    onValueChange = { pollOption1 = it },
                    label = { Text("Poll Option 1 (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pollOption2,
                    onValueChange = { pollOption2 = it },
                    label = { Text("Poll Option 2 (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (postContent.isNotBlank()) {
                            viewModel.createCommunityPost(postContent, listOf(pollOption1, pollOption2))
                            viewModel.closeCreateSheet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publish Post", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (mode == "SHORT") "Short caption & #Shorts" else "Video title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("upload_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & chapters") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Tech", "Gaming", "Cooking", "Nature", "Music", "Movies")) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                thumbnailPreset = when (cat) {
                                    "Gaming" -> "img_thumb_gaming_1791326409204"
                                    "Cooking" -> "img_thumb_cooking_1791326420737"
                                    "Nature" -> "img_thumb_nature_1791326431475"
                                    else -> "img_thumb_tech_1791326396393"
                                }
                            },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (customThumbnailUri.isNotBlank()) "Custom Thumbnail Selected ✓" else "Choose Thumbnail Photo")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val finalTitle = title.ifBlank {
                            if (mode == "SHORT") "My New Studio Short 🔥 #Shorts" else "My Studio 4K Video Showcase"
                        }
                        viewModel.uploadUserVideo(
                            title = finalTitle,
                            description = description,
                            category = category,
                            isShort = mode == "SHORT",
                            customThumbnailUri = customThumbnailUri,
                            thumbnailPreset = thumbnailPreset
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    modifier = Modifier.fillMaxWidth().testTag("publish_upload_button")
                ) {
                    Text(
                        text = if (mode == "SHORT") "Upload Short" else "Publish Video",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SaveToPlaylistDialog(videoId: String, viewModel: YouTubeViewModel) {
    val playlists by viewModel.allPlaylists.collectAsState()
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.closeSaveToPlaylistDialog() },
        title = { Text("Save video to...") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                playlists.forEach { playlist ->
                    val ids = playlist.videoIdsCsv.split(",").map { it.trim() }
                    val isChecked = videoId in ids
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleVideoInPlaylist(playlist, videoId) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggleVideoInPlaylist(playlist, videoId) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(playlist.title, fontWeight = FontWeight.Medium)
                            Text(
                                playlist.privacy,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Or create new playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName, "", "Public", videoId)
                    }
                    viewModel.closeSaveToPlaylistDialog()
                }
            ) {
                Text("Done")
            }
        }
    )
}
