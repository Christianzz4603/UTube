package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.PlaylistEntity
import com.example.data.VideoEntity
import com.example.data.formatDuration
import com.example.ui.YouTubeViewModel
import com.example.ui.auth.signOutUTube
import com.example.ui.components.ChannelAvatarBadge
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.YouTubeRed
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouLibraryScreen(
    viewModel: YouTubeViewModel,
    onEnterCast: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val watchHistory by viewModel.watchHistory.collectAsState()
    val cloudWatchHistory by viewModel.cloudWatchHistory.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val likedVideos by viewModel.likedVideos.collectAsState()
    val watchLaterVideos by viewModel.watchLaterVideos.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val segmentsSkippedCount by viewModel.segmentsSkippedCount.collectAsState()
    val secondsSavedBySponsorBlock by viewModel.secondsSavedBySponsorBlock.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser = remember { runCatching { Firebase.auth.currentUser }.getOrNull() }
    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Christian Studio"
    val userEmail = currentUser?.email ?: "christianjaydelica3@gmail.com"
    val userHandle = "@${userEmail.substringBefore("@")}"

    val combinedHistoryVideos = remember(watchHistory, cloudWatchHistory, allVideos) {
        val cloudMapped = cloudWatchHistory.mapNotNull { doc ->
            allVideos.find { it.id == doc.videoId }?.copy(
                watchProgressFraction = doc.progressFraction.coerceIn(0.1f, 1f)
            )
        }
        (cloudMapped + watchHistory).distinctBy { it.id }
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showApkWorkflowSheet by remember { mutableStateOf(false) }
    var selectedLibrarySection by remember { mutableStateOf<String?>(null) } // "liked", "watch_later", "downloads", "your_videos"
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    val userUploadedVideos = remember(allVideos) { allVideos.filter { it.isUserUploaded } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("you_library_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Utility Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEnterCast) {
                    Icon(Icons.Outlined.Tv, contentDescription = "Cast")
                }
                IconButton(onClick = { viewModel.openNotifications() }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                }
                IconButton(onClick = { viewModel.openSearch() }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                }
                IconButton(
                    onClick = { viewModel.toggleDarkTheme() },
                    modifier = Modifier.testTag("toggle_theme_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle Dark/Light Theme"
                    )
                }
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }

        // User Profile Header with Google Account Sync Badge
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelAvatarBadge(
                    name = displayName,
                    colorLong = 0xFF00ACC1,
                    size = 68.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF00D400).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(1.dp, Color(0xFF00D400), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "UNLOCKED FREE",
                                color = Color(0xFF00D400),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "$userHandle • $userEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Google Account Synced • View channel >",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            selectedLibrarySection = "your_videos"
                        }
                    )
                }
            }
        }

        // SponsorBlock & Unlocked Stats Banner
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { viewModel.openSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00D400),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SponsorBlock Active • $segmentsSkippedCount segments skipped",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Time saved: ${secondsSavedBySponsorBlock / 60}m ${secondsSavedBySponsorBlock % 60}s • 4K60 & Background Play Free",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Account Action Pills Row (Switch Google Account, Incognito, Appearance, Upload)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = {
                            signOutUTube(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = coroutineScope
                            )
                        },
                        label = { Text("Switch Google Account") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("switch_google_account_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = { viewModel.toggleIncognito() },
                        label = { Text(if (isIncognito) "Turn off Incognito" else "Turn on Incognito") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("incognito_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = { viewModel.toggleDarkTheme() },
                        label = { Text(if (isDarkTheme) "Appearance: Dark" else "Appearance: Light") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                item {
                    AssistChip(
                        onClick = { showApkWorkflowSheet = true },
                        label = { Text("Build & Export APK") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Android,
                                contentDescription = null,
                                tint = Color(0xFF00D400),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("build_apk_workflow_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = { viewModel.openCreateSheet() },
                        label = { Text("UTube Studio Upload") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.VideoCall,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        // Watch History Carousel (Synced with Google Account)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Synced with your Google Account ($userEmail)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (combinedHistoryVideos.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearWatchHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text("Clear all")
                    }
                }
            }

            if (combinedHistoryVideos.isEmpty()) {
                Text(
                    text = if (isIncognito) "Incognito mode is active — watch history is paused."
                    else "Videos you watch on your Google Account will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(combinedHistoryVideos, key = { "hist_${it.id}" }) { video ->
                        HistoryMiniVideoCard(
                            video = video,
                            onClick = { viewModel.openVideo(video) }
                        )
                    }
                }
            }
        }

        // Playlists Carousel
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showCreatePlaylistDialog = true },
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Playlist")
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Liked Videos built-in playlist card
                item {
                    PlaylistCardItem(
                        title = "Liked videos",
                        subtitle = "${likedVideos.size} videos • Private",
                        previewVideo = likedVideos.firstOrNull(),
                        onClick = { selectedLibrarySection = "liked" }
                    )
                }
                // Watch Later built-in playlist card
                item {
                    PlaylistCardItem(
                        title = "Watch Later",
                        subtitle = "${watchLaterVideos.size} videos • Private",
                        previewVideo = watchLaterVideos.firstOrNull(),
                        onClick = { selectedLibrarySection = "watch_later" }
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    val ids = playlist.videoIdsCsv.split(",").filter { it.isNotBlank() }
                    val firstVideo = allVideos.find { it.id == ids.firstOrNull() }
                    PlaylistCardItem(
                        title = playlist.title,
                        subtitle = "${ids.size} videos • ${playlist.privacy}",
                        previewVideo = firstVideo,
                        onClick = { selectedPlaylist = playlist }
                    )
                }
            }
        }

        // Library Menu Rows
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            LibraryRowMenuItem(
                icon = Icons.Outlined.SmartDisplay,
                title = "Your videos",
                subtitle = "${userUploadedVideos.size} uploaded videos",
                onClick = { selectedLibrarySection = "your_videos" }
            )
            LibraryRowMenuItem(
                icon = Icons.Outlined.Download,
                title = "Downloads",
                subtitle = "${downloadedVideos.size} videos available offline",
                badgeIcon = Icons.Filled.CheckCircle,
                onClick = { selectedLibrarySection = "downloads" }
            )
            LibraryRowMenuItem(
                icon = Icons.Outlined.ThumbUp,
                title = "Liked videos",
                subtitle = "${likedVideos.size} videos",
                onClick = { selectedLibrarySection = "liked" }
            )
            LibraryRowMenuItem(
                icon = Icons.Outlined.Schedule,
                title = "Watch Later",
                subtitle = "${watchLaterVideos.size} videos",
                onClick = { selectedLibrarySection = "watch_later" }
            )
            LibraryRowMenuItem(
                icon = Icons.Outlined.Android,
                title = "Android APK Build Workflow",
                subtitle = "Export APK via AI Studio, GitHub Actions (.github/workflows/build-apk.yml), or Gradle",
                badgeIcon = Icons.Filled.Download,
                onClick = { showApkWorkflowSheet = true }
            )
        }
    }

    if (showApkWorkflowSheet) {
        ApkWorkflowBottomSheet(
            onDismiss = { showApkWorkflowSheet = false },
            onShowSnackbar = { viewModel.showSnackbar(it) }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var privacy by remember { mutableStateOf("Public") }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Public", "Unlisted", "Private").forEach { option ->
                            FilterChip(
                                selected = privacy == option,
                                onClick = { privacy = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.createPlaylist(newTitle, newDesc, privacy)
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Section Detail Sheet (Liked, Watch Later, Downloads, Your Videos)
    selectedLibrarySection?.let { section ->
        val (sheetTitle, sectionVideos) = when (section) {
            "liked" -> "Liked videos" to likedVideos
            "watch_later" -> "Watch Later" to watchLaterVideos
            "downloads" -> "Downloads" to downloadedVideos
            else -> "Your uploaded videos" to userUploadedVideos
        }
        ModalBottomSheet(
            onDismissRequest = { selectedLibrarySection = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = sheetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (sectionVideos.isEmpty()) {
                    Text(
                        text = "No videos in $sheetTitle yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(sectionVideos, key = { it.id }) { video ->
                            CompactVideoRow(
                                video = video,
                                onClick = {
                                    selectedLibrarySection = null
                                    viewModel.openVideo(video)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Playlist Detail Sheet
    selectedPlaylist?.let { playlist ->
        val ids = playlist.videoIdsCsv.split(",").filter { it.isNotBlank() }
        val playlistVideos = allVideos.filter { it.id in ids }
        ModalBottomSheet(
            onDismissRequest = { selectedPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (playlist.description.isNotBlank()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.deletePlaylist(playlist.id)
                            selectedPlaylist = null
                        }
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete playlist")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(playlistVideos, key = { it.id }) { video ->
                        CompactVideoRow(
                            video = video,
                            onClick = {
                                selectedPlaylist = null
                                viewModel.openVideo(video)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMiniVideoCard(
    video: VideoEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(156.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            VideoThumbnailImage(
                video = video,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = formatDuration(video.durationSeconds),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            LinearProgressIndicator(
                progress = { video.watchProgressFraction.coerceIn(0.1f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = YouTubeRed
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = video.channelName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistCardItem(
    title: String,
    subtitle: String,
    previewVideo: VideoEntity?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(156.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (previewVideo != null) {
                VideoThumbnailImage(
                    video = previewVideo,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibraryRowMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (badgeIcon != null) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactVideoRow(
    video: VideoEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            VideoThumbnailImage(
                video = video,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = formatDuration(video.durationSeconds),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${video.channelName} • ${video.publishedTimeText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkWorkflowBottomSheet(
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = null,
                    tint = Color(0xFF00D400),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "UTube Android APK Build Workflow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Package ID: com.aistudio.youtube.vstrxm • v1.0 Unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 1: AI Studio Direct APK Download
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. Direct Download in AI Studio (Fastest)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF00D400)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Open the Settings / Export menu in the top-right bar of Google AI Studio and select 'Download APK' (or 'Generate APK/AAB') to download the signed UTube APK directly to your phone or PC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: GitHub Actions Automated CI/CD Workflow
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. GitHub Actions CI/CD Workflow (Included)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        TextButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(".github/workflows/build-apk.yml"))
                                onShowSnackbar("Copied workflow path: .github/workflows/build-apk.yml")
                            }
                        ) {
                            Text("Copy Path")
                        }
                    }
                    Text(
                        text = "File: .github/workflows/build-apk.yml\nPush to GitHub from AI Studio and GitHub Actions will automatically compile and upload 'UTube-v1.0-Unlocked-debug.apk' under the Actions -> Artifacts tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 3: Local / CLI Build Script
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Command-Line APK Script (build-apk.sh)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("bash build-apk.sh"))
                                onShowSnackbar("Copied command: bash build-apk.sh")
                            }
                        ) {
                            Text("Copy Cmd")
                        }
                    }
                    Text(
                        text = "Run 'bash build-apk.sh' or 'gradle :app:assembleDebug' to produce:\napp/build/outputs/apk/debug/app-debug.apk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
