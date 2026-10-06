package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.*
import com.example.ui.YouTubeViewModel
import com.example.ui.components.ChannelAvatarBadge
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.YouTubeVideoCard
import com.example.ui.theme.YouTubeRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoWatchOverlay(
    video: VideoEntity,
    viewModel: YouTubeViewModel,
    onEnterPipMode: () -> Unit
) {
    val isMinimized by viewModel.isPlayerMinimized.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState()
    val isAmbientMode by viewModel.isAmbientMode.collectAsState()
    val isCaptionsEnabled by viewModel.isCaptionsEnabled.collectAsState()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val comments by viewModel.currentVideoComments.collectAsState()

    val context = LocalContext.current

    var currentPositionMs by remember(video.id) { mutableIntStateOf(0) }
    var totalDurationMs by remember(video.id) { mutableIntStateOf(video.durationSeconds * 1000) }
    var showPlayerControls by remember { mutableStateOf(true) }
    var seekRippleText by remember { mutableStateOf<String?>(null) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val channel = allChannels.find { it.id == video.channelId }
    val isSubscribed = channel?.isSubscribed ?: false
    val chapters = remember(video.id) { video.parseChapters() }
    val upNextVideos = remember(allVideos, video.id) {
        allVideos.filter { it.id != video.id && !it.isShort }
    }

    // Back button minimizes full player to mini-player bar
    BackHandler(enabled = !isMinimized) {
        viewModel.minimizePlayer()
    }

    // Periodically sync VideoView progress
    LaunchedEffect(video.id, isPlaying) {
        while (true) {
            delay(500)
            val vv = videoViewRef
            if (vv != null && vv.isPlaying) {
                currentPositionMs = vv.currentPosition
                if (vv.duration > 0) {
                    totalDurationMs = vv.duration
                }
            }
        }
    }

    LaunchedEffect(seekRippleText) {
        if (seekRippleText != null) {
            delay(650)
            seekRippleText = null
        }
    }

    if (isMinimized) {
        // Persistent Docked Mini-Player Bar above Bottom Navigation
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.expandPlayer() }
                .testTag("mini_player_bar")
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                    ) {
                        VideoThumbnailImage(
                            video = video,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
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
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.closePlayer() },
                        modifier = Modifier.testTag("mini_player_close")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close player"
                        )
                    }
                }
                val fraction = if (totalDurationMs > 0) {
                    (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 0.25f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = YouTubeRed
                )
            }
        }
        return
    }

    // Full-Screen Watch Page
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("full_video_watch_page")
    ) {
        // 16:9 Interactive Video Player Container with Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .pointerInput(video.id) {
                    detectTapGestures(
                        onTap = { showPlayerControls = !showPlayerControls },
                        onDoubleTap = { offset ->
                            val vv = videoViewRef
                            if (offset.x < size.width / 2f) {
                                val nextPos = ((vv?.currentPosition ?: currentPositionMs) - 10_000).coerceAtLeast(0)
                                vv?.seekTo(nextPos)
                                currentPositionMs = nextPos
                                seekRippleText = "⏪ -10 seconds"
                            } else {
                                val nextPos = ((vv?.currentPosition ?: currentPositionMs) + 10_000)
                                    .coerceAtMost(totalDurationMs)
                                vv?.seekTo(nextPos)
                                currentPositionMs = nextPos
                                seekRippleText = "⏩ +10 seconds"
                            }
                        }
                    )
                }
        ) {
            // Fallback high-res thumbnail while stream buffers
            VideoThumbnailImage(
                video = video,
                modifier = Modifier.fillMaxSize()
            )

            // Real Android VideoView streaming MP4
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef = this
                        setVideoURI(Uri.parse(video.videoUrl))
                        setOnPreparedListener { mp ->
                            totalDurationMs = mp.duration.coerceAtLeast(video.durationSeconds * 1000)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                runCatching {
                                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                }
                            }
                            if (isPlaying) start()
                        }
                        setOnCompletionListener {
                            if (isAutoplayEnabled && upNextVideos.isNotEmpty()) {
                                viewModel.openVideo(upNextVideos.first())
                            } else {
                                viewModel.setPlaying(false)
                            }
                        }
                        setOnErrorListener { _, _, _ -> true }
                    }
                },
                update = { vv ->
                    videoViewRef = vv
                    if (isPlaying && !vv.isPlaying) {
                        vv.start()
                    } else if (!isPlaying && vv.isPlaying) {
                        vv.pause()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Captions Overlay
            if (isCaptionsEnabled) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "[♪ Playing ${video.title.take(36)}...]",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Double-Tap +/- 10s Ripple Indicator
            seekRippleText?.let { rippleLabel ->
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = rippleLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Custom YouTube Player Controls Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showPlayerControls,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    // Top Player Row: Minimize chevron, Autoplay switch, Captions, Settings, PiP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.minimizePlayer() },
                            modifier = Modifier.testTag("minimize_player_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Minimize player",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isAutoplayEnabled,
                                onCheckedChange = { viewModel.toggleAutoplay() },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(onClick = { viewModel.toggleCaptions() }) {
                                Icon(
                                    imageVector = if (isCaptionsEnabled) Icons.Filled.ClosedCaption else Icons.Outlined.ClosedCaption,
                                    contentDescription = "Captions",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onEnterPipMode) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("player_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Player Settings",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Center Play/Pause & Skip 10s Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val nextPos = (currentPositionMs - 10_000).coerceAtLeast(0)
                                videoViewRef?.seekTo(nextPos)
                                currentPositionMs = nextPos
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.togglePlayPause() }
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextPos = (currentPositionMs + 10_000).coerceAtMost(totalDurationMs)
                                videoViewRef?.seekTo(nextPos)
                                currentPositionMs = nextPos
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Bottom Scrubber & Timestamp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatDuration(currentPositionMs / 1000)} / ${formatDuration(totalDurationMs / 1000)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${videoQuality} • ${playbackSpeed}x",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                        Slider(
                            value = if (totalDurationMs > 0) {
                                (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { newFraction ->
                                val targetMs = (newFraction * totalDurationMs).toInt()
                                currentPositionMs = targetMs
                                videoViewRef?.seekTo(targetMs)
                                viewModel.updateWatchProgress(video, newFraction)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = YouTubeRed,
                                activeTrackColor = YouTubeRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.height(22.dp)
                        )
                    }
                }
            }
        }

        // Ambient Mode Subtle Glow Bar under Player
        if (isAmbientMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(video.channelAvatarColor).copy(alpha = 0.38f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Scrollable Watch Metadata, Action Pills, Chapters, Comments Preview & Up Next Feed
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Title & Expandable Description Trigger
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDescriptionSheet = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatCompactCount(video.viewsCount)} views  ${video.publishedTimeText}  #YouTube #${video.category}  ...more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Channel Info + Subscribe Pill + Bell
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelAvatarBadge(
                        name = video.channelName,
                        colorLong = video.channelAvatarColor,
                        size = 38.dp,
                        onClick = {
                            viewModel.minimizePlayer()
                            viewModel.openChannelProfile(video.channelId)
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.minimizePlayer()
                                viewModel.openChannelProfile(video.channelId)
                            }
                    ) {
                        Text(
                            text = video.channelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.subscriberCountText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onBackground,
                        contentColor = if (isSubscribed) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable {
                                viewModel.toggleSubscriptionByChannelId(
                                    video.channelId,
                                    video.channelName,
                                    video.channelAvatarColor
                                )
                            }
                            .testTag("watch_subscribe_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSubscribed) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Pills Row: Segmented Like | Dislike, Share, Remix, Download, Save, Clip
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Segmented Like / Dislike Pill
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { viewModel.toggleLike(video) }
                                        .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
                                        .testTag("watch_like_button")
                                ) {
                                    Icon(
                                        imageVector = if (video.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                        contentDescription = "Like",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (video.isLiked) YouTubeRed else MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatCompactCount(video.likesCount),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.height(20.dp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Box(
                                    modifier = Modifier
                                        .clickable { viewModel.toggleDislike(video) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("watch_dislike_button")
                                ) {
                                    Icon(
                                        imageVector = if (video.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                        contentDescription = "Dislike",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Share Pill
                    item {
                        WatchActionPill(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on YouTube: ${video.videoUrl}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share video"))
                            }
                        )
                    }

                    // Remix Pill
                    item {
                        WatchActionPill(
                            icon = Icons.Outlined.AutoAwesomeMotion,
                            label = "Remix",
                            onClick = {
                                viewModel.minimizePlayer()
                                viewModel.openCreateSheet()
                            }
                        )
                    }

                    // Download Pill
                    item {
                        WatchActionPill(
                            icon = if (video.isDownloaded) Icons.Filled.DownloadDone else Icons.Outlined.Download,
                            label = if (video.isDownloaded) "Downloaded" else "Download",
                            onClick = { viewModel.toggleDownload(video) }
                        )
                    }

                    // Save to Playlist Pill
                    item {
                        WatchActionPill(
                            icon = Icons.Outlined.BookmarkBorder,
                            label = "Save",
                            onClick = { viewModel.openSaveToPlaylistDialog(video.id) }
                        )
                    }

                    // Clip Pill
                    item {
                        WatchActionPill(
                            icon = Icons.Outlined.ContentCut,
                            label = "Clip",
                            onClick = { viewModel.showSnackbar("15s Clip saved to your library!") }
                        )
                    }
                }
            }

            // Video Chapters Scrubber Chips
            if (chapters.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chapters) { chapter ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    val targetMs = chapter.timeSeconds * 1000
                                    currentPositionMs = targetMs
                                    videoViewRef?.seekTo(targetMs)
                                    viewModel.showSnackbar("Jumped to chapter: ${chapter.title}")
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = chapter.timestampLabel,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Comments Teaser Card (tappable to open full Comments Sheet)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable { showCommentsSheet = true }
                        .testTag("watch_comments_preview_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${comments.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val previewComment = comments.firstOrNull()
                        if (previewComment != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChannelAvatarBadge(
                                    name = previewComment.authorName,
                                    colorLong = previewComment.authorAvatarColor,
                                    size = 24.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = previewComment.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = "Tap to add a comment...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Up Next Related Videos Header
            item {
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            items(upNextVideos, key = { "upnext_${it.id}" }) { nextVideo ->
                YouTubeVideoCard(
                    video = nextVideo,
                    onVideoClick = { viewModel.openVideo(nextVideo) },
                    onChannelClick = {
                        viewModel.minimizePlayer()
                        viewModel.openChannelProfile(nextVideo.channelId)
                    },
                    onSaveToWatchLater = { viewModel.toggleWatchLater(nextVideo) },
                    onSaveToPlaylist = { viewModel.openSaveToPlaylistDialog(nextVideo.id) },
                    onDownloadVideo = { viewModel.toggleDownload(nextVideo) }
                )
            }
        }
    }

    // Description & Chapters Bottom Sheet
    if (showDescriptionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDescriptionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Key Metrics Row (Likes, Views, Published)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricColumn(value = formatCompactCount(video.likesCount), label = "Likes")
                    MetricColumn(value = formatCompactCount(video.viewsCount), label = "Views")
                    MetricColumn(value = video.publishedTimeText, label = "Published")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Full Comments Bottom Sheet with Top / Newest Sort & Live Input
    if (showCommentsSheet) {
        var commentDraft by remember { mutableStateOf("") }
        var sortByNewest by remember { mutableStateOf(false) }

        val sortedComments = remember(comments, sortByNewest) {
            if (sortByNewest) comments.sortedByDescending { it.id } else comments
        }

        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp, max = 560.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${comments.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !sortByNewest,
                            onClick = { sortByNewest = false },
                            label = { Text("Top") }
                        )
                        FilterChip(
                            selected = sortByNewest,
                            onClick = { sortByNewest = true },
                            label = { Text("Newest") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("watch_comment_input"),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (commentDraft.isNotBlank()) {
                                viewModel.addComment(video.id, commentDraft)
                                commentDraft = ""
                            }
                        },
                        modifier = Modifier.testTag("watch_comment_send")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(sortedComments, key = { it.id }) { comment ->
                        CommentRowItem(
                            comment = comment,
                            onLike = { viewModel.toggleCommentLike(comment) }
                        )
                    }
                }
            }
        }
    }

    // Player Settings Gear Bottom Sheet (Quality, Playback Speed, Ambient Mode, Captions)
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Video settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val qualities = listOf("Auto (1080p)", "1080p60 Premium", "720p60", "480p", "360p")
                    items(qualities) { q ->
                        FilterChip(
                            selected = videoQuality == q,
                            onClick = { viewModel.setVideoQuality(q) },
                            label = { Text(q) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Playback speed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    items(speeds) { s ->
                        FilterChip(
                            selected = playbackSpeed == s,
                            onClick = { viewModel.setPlaybackSpeed(s) },
                            label = { Text("${s}x") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ambient mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Casts soft glow colors from the video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAmbientMode,
                        onCheckedChange = { viewModel.toggleAmbientMode() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Captions (CC)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "English (auto-generated)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCaptionsEnabled,
                        onCheckedChange = { viewModel.toggleCaptions() }
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun WatchActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .height(36.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
