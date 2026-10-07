package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.SponsorCategory
import com.example.data.SponsorSkipBehavior
import com.example.ui.YouTubeViewModel
import com.example.ui.auth.signOutUTube
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.theme.YouTubeRed

@Composable
fun YouTubeSettingsScreen(
    viewModel: YouTubeViewModel,
    onSignOut: () -> Unit = {}
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isAmbientMode by viewModel.isAmbientMode.collectAsState()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsState()
    val isCaptionsEnabled by viewModel.isCaptionsEnabled.collectAsState()
    val isStableVolume by viewModel.isStableVolume.collectAsState()
    val isStatsForNerds by viewModel.isStatsForNerds.collectAsState()
    val isRestrictedMode by viewModel.isRestrictedMode.collectAsState()
    val isInlineMutedPreview by viewModel.isInlineMutedPreview.collectAsState()
    val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()
    val isSponsorBlockEnabled by viewModel.isSponsorBlockEnabled.collectAsState()
    val sponsorSkipBehavior by viewModel.sponsorSkipBehavior.collectAsState()
    val segmentsSkippedCount by viewModel.segmentsSkippedCount.collectAsState()
    val secondsSavedBySponsorBlock by viewModel.secondsSavedBySponsorBlock.collectAsState()
    val isReturnDislikeEnabled by viewModel.isReturnDislikeEnabled.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showApkWorkflowSheet by remember { mutableStateOf(false) }

    BackHandler { viewModel.closeSettings() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("youtube_settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeSettings() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item {
                    SettingsSectionHeader("SponsorBlock & Return Dislike (100% Free)")
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Shield,
                        title = "Enable SponsorBlock",
                        subtitle = "Skipped $segmentsSkippedCount segments (${secondsSavedBySponsorBlock / 60}m ${secondsSavedBySponsorBlock % 60}s saved)",
                        checked = isSponsorBlockEnabled,
                        onCheckedChange = { viewModel.toggleSponsorBlock() }
                    )
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.FastForward,
                        title = "SponsorBlock skip behavior",
                        subtitle = "Current mode: ${sponsorSkipBehavior.label} (Tap to cycle Auto-Skip / Skip Button / Seekbar)",
                        onClick = {
                            val next = when (sponsorSkipBehavior) {
                                SponsorSkipBehavior.AUTO_SKIP -> SponsorSkipBehavior.SHOW_SKIP_BUTTON
                                SponsorSkipBehavior.SHOW_SKIP_BUTTON -> SponsorSkipBehavior.SHOW_IN_SEEKBAR_ONLY
                                SponsorSkipBehavior.SHOW_IN_SEEKBAR_ONLY -> SponsorSkipBehavior.AUTO_SKIP
                                SponsorSkipBehavior.DISABLED -> SponsorSkipBehavior.AUTO_SKIP
                            }
                            viewModel.setSponsorSkipBehavior(next)
                        }
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            text = "Active SponsorBlock Categories",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SponsorCategory.entries.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(cat.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(
                                    text = sponsorSkipBehavior.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF00D400),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.ThumbDown,
                        title = "Return YouTube Dislike",
                        subtitle = "Show exact community dislike counts on all videos & Shorts",
                        checked = isReturnDislikeEnabled,
                        onCheckedChange = { viewModel.toggleReturnDislike() }
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsSectionHeader("Account & General")
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Appearance: Dark theme",
                        subtitle = "Reduce glare and improve night viewing",
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleDarkTheme() }
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.PlayCircleOutline,
                        title = "Playback in feeds",
                        subtitle = "Show inline muted previews while scrolling Home",
                        checked = isInlineMutedPreview,
                        onCheckedChange = { viewModel.toggleInlineMutedPreview() }
                    )
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Forward10,
                        title = "Double-tap to seek",
                        subtitle = "$doubleTapSeekSeconds seconds (Tap to cycle 5s / 10s / 15s / 30s)",
                        onClick = { viewModel.cycleDoubleTapSeekSeconds() }
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Shield,
                        title = "Restricted Mode",
                        subtitle = "Helps hide potentially mature videos",
                        checked = isRestrictedMode,
                        onCheckedChange = { viewModel.toggleRestrictedMode() }
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsSectionHeader("Video quality & Playback")
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.AutoMode,
                        title = "Autoplay next video",
                        subtitle = "When a video finishes, play another automatically",
                        checked = isAutoplayEnabled,
                        onCheckedChange = { viewModel.toggleAutoplay() }
                    )
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Hd,
                        title = "Video quality preferences",
                        subtitle = "Current default: $videoQuality (Higher picture quality)",
                        onClick = {
                            val nextQ = if (videoQuality.contains("1080p")) "720p60 Data Saver" else "1080p60 Premium"
                            viewModel.setVideoQuality(nextQ)
                        }
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Lightbulb,
                        title = "Ambient mode",
                        subtitle = "Immersive lighting glow beneath the video player",
                        checked = isAmbientMode,
                        onCheckedChange = { viewModel.toggleAmbientMode() }
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.GraphicEq,
                        title = "Stable volume",
                        subtitle = "Automatically normalizes loud and quiet audio",
                        checked = isStableVolume,
                        onCheckedChange = { viewModel.toggleStableVolume() }
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.ClosedCaption,
                        title = "Always show captions",
                        subtitle = "Include auto-generated subtitles when available",
                        checked = isCaptionsEnabled,
                        onCheckedChange = { viewModel.toggleCaptions() }
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsSectionHeader("History, Privacy & Developer")
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = "Clear watch history",
                        subtitle = "Remove all watched progress from this device",
                        onClick = { viewModel.clearWatchHistory() }
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Outlined.Terminal,
                        title = "Enable Stats for nerds",
                        subtitle = "Show technical codec & buffer overlay on video player",
                        checked = isStatsForNerds,
                        onCheckedChange = { viewModel.toggleStatsForNerds() }
                    )
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Logout,
                        title = "Sign out of Google Account",
                        subtitle = "Switch Google Account or re-authenticate UTube Cloud Sync",
                        onClick = {
                            viewModel.closeSettings()
                            signOutUTube(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = coroutineScope
                            )
                        }
                    )
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Android,
                        title = "Android APK Build & Export Workflow",
                        subtitle = ".github/workflows/build-apk.yml • build-apk.sh • AI Studio Export",
                        onClick = { showApkWorkflowSheet = true }
                    )
                }
                item {
                    SettingsClickableRow(
                        icon = Icons.Outlined.Info,
                        title = "About UTube Unlocked",
                        subtitle = "100% Free Forever • SponsorBlock + Return Dislike + 4K60 HDR",
                        onClick = { viewModel.showSnackbar("UTube Unlocked • All features 100% Free") }
                    )
                }
            }
        }
    }

    if (showApkWorkflowSheet) {
        ApkWorkflowBottomSheet(
            onDismiss = { showApkWorkflowSheet = false },
            onShowSnackbar = { viewModel.showSnackbar(it) }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ExploreTrendingOverlayScreen(viewModel: YouTubeViewModel) {
    val allVideos by viewModel.allVideos.collectAsState()
    var selectedHub by remember { mutableStateOf("Trending") }
    val hubs = listOf("Trending", "Music", "Gaming", "Movies", "Tech", "Cooking", "Nature")

    val rankedVideos = remember(allVideos, selectedHub) {
        val base = allVideos.filter { !it.isShort }.sortedByDescending { it.viewsCount }
        if (selectedHub == "Trending") base
        else base.filter { it.category.equals(selectedHub, ignoreCase = true) }
    }

    BackHandler { viewModel.closeExploreTrending() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_trending_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeExploreTrending() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = YouTubeRed,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Explore & Trending",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hubs) { hub ->
                    FilterChip(
                        selected = selectedHub == hub,
                        onClick = { selectedHub = hub },
                        label = { Text(hub, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(rankedVideos, key = { _, v -> v.id }) { index, video ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = YouTubeRed,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "#${index + 1} ON TRENDING",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    YouTubeVideoCard(
                        video = video,
                        onVideoClick = {
                            viewModel.closeExploreTrending()
                            viewModel.openVideo(video)
                        },
                        onChannelClick = {
                            viewModel.closeExploreTrending()
                            viewModel.openChannelProfile(video.channelId)
                        },
                        onSaveToWatchLater = { viewModel.toggleWatchLater(video) },
                        onSaveToPlaylist = { viewModel.openSaveToPlaylistDialog(video.id) },
                        onDownloadVideo = { viewModel.toggleDownload(video) },
                        onPlayNextInQueue = { viewModel.playNextInQueue(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun GoLiveStudioOverlayScreen(viewModel: YouTubeViewModel) {
    var streamTitle by remember { mutableStateOf("LIVE: Late-Night Q&A & Studio Session 🔴") }
    var streamCategory by remember { mutableStateOf("Tech") }
    var isStreamingNow by remember { mutableStateOf(false) }

    BackHandler { viewModel.closeGoLive() }

    Surface(
        color = Color(0xFF0A0A0A),
        modifier = Modifier
            .fillMaxSize()
            .testTag("go_live_studio_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated Camera Viewfinder Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF263238), Color(0xFF090909))
                        )
                    )
            )

            // Top Live Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeGoLive() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Go Live", tint = Color.White)
                }
                Surface(
                    color = if (isStreamingNow) YouTubeRed else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isStreamingNow) "● LIVE  00:14" else "PREVIEW • 1080p 60fps",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Icon(Icons.Outlined.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
            }

            // Center Viewfinder Info
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(YouTubeRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sensors,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "YouTube Live Producer",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = streamTitle,
                    onValueChange = { streamTitle = it },
                    label = { Text("Live Stream Title", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Tech", "Gaming", "Music", "Cooking")) { cat ->
                        FilterChip(
                            selected = streamCategory == cat,
                            onClick = { streamCategory = cat },
                            label = { Text(cat, color = Color.White) }
                        )
                    }
                }
            }

            // Bottom Go Live Trigger Button
            Button(
                onClick = {
                    viewModel.uploadUserVideo(
                        title = streamTitle,
                        description = "Broadcasted live via YouTube Studio Mobile.",
                        category = streamCategory,
                        isShort = false,
                        customThumbnailUri = "",
                        thumbnailPreset = "img_thumb_tech_1791326396393"
                    )
                    viewModel.closeGoLive()
                },
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .height(54.dp)
            ) {
                Icon(Icons.Filled.Sensors, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go Live Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
