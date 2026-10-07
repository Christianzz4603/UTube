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
    val isLoopVideo by viewModel.isLoopVideo.collectAsState()
    val isStableVolume by viewModel.isStableVolume.collectAsState()
    val isStatsForNerds by viewModel.isStatsForNerds.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsState()
    val watchQueueVideos by viewModel.watchQueueVideos.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val comments by viewModel.currentVideoComments.collectAsState()
    val sponsorSegments by viewModel.currentVideoSponsorSegments.collectAsState()
    val isSponsorBlockEnabled by viewModel.isSponsorBlockEnabled.collectAsState()
    val sponsorSkipBehavior by viewModel.sponsorSkipBehavior.collectAsState()
    val isReturnDislikeEnabled by viewModel.isReturnDislikeEnabled.collectAsState()

    val context = LocalContext.current

    var currentPositionMs by remember(video.id) { mutableIntStateOf(0) }
    var totalDurationMs by remember(video.id) { mutableIntStateOf(video.durationSeconds * 1000) }
    var showPlayerControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var isHolding2xSpeed by remember { mutableStateOf(false) }
    var seekRippleText by remember { mutableStateOf<String?>(null) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThanksSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSubmitSponsorSheet by remember { mutableStateOf(false) }
    var activeManualSkipSegment by remember(video.id) { mutableStateOf<SponsorSegmentDoc?>(null) }
    val skippedSegmentIds = remember(video.id) { mutableStateListOf<String>() }
    var showLiveChat by remember(video.id) { mutableStateOf(video.isLive) }
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

    // Periodically sync VideoView progress & SponsorBlock segment detection
    LaunchedEffect(video.id, isPlaying, isSponsorBlockEnabled, sponsorSkipBehavior, sponsorSegments) {
        while (true) {
            delay(400)
            val vv = videoViewRef
            if (vv != null && vv.isPlaying) {
                currentPositionMs = vv.currentPosition
                if (vv.duration > 0) {
                    totalDurationMs = vv.duration
                }
                val currentSec = currentPositionMs / 1000
                if (isSponsorBlockEnabled && sponsorSkipBehavior != SponsorSkipBehavior.DISABLED) {
                    val matchingSeg = sponsorSegments.firstOrNull { seg ->
                        currentSec in seg.startTimeSec until seg.endTimeSec
                    }
                    if (matchingSeg != null) {
                        if (sponsorSkipBehavior == SponsorSkipBehavior.AUTO_SKIP && matchingSeg.id !in skippedSegmentIds) {
                            skippedSegmentIds.add(matchingSeg.id)
                            val targetMs = (matchingSeg.endTimeSec * 1000).coerceAtMost(totalDurationMs)
                            vv.seekTo(targetMs)
                            currentPositionMs = targetMs
                            viewModel.recordSponsorSkip(matchingSeg)
                            activeManualSkipSegment = null
                        } else if (sponsorSkipBehavior == SponsorSkipBehavior.SHOW_SKIP_BUTTON) {
                            activeManualSkipSegment = matchingSeg
                        }
                    } else {
                        activeManualSkipSegment = null
                    }
                } else {
                    activeManualSkipSegment = null
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
                .pointerInput(video.id, isScreenLocked, doubleTapSeekSeconds) {
                    detectTapGestures(
                        onTap = { showPlayerControls = !showPlayerControls },
                        onLongPress = {
                            if (!isScreenLocked) {
                                isHolding2xSpeed = true
                            }
                        },
                        onPress = {
                            tryAwaitRelease()
                            isHolding2xSpeed = false
                        },
                        onDoubleTap = { offset ->
                            if (!isScreenLocked) {
                                val vv = videoViewRef
                                val deltaMs = doubleTapSeekSeconds * 1000
                                if (offset.x < size.width / 2f) {
                                    val nextPos = ((vv?.currentPosition ?: currentPositionMs) - deltaMs).coerceAtLeast(0)
                                    vv?.seekTo(nextPos)
                                    currentPositionMs = nextPos
                                    seekRippleText = "⏪ -${doubleTapSeekSeconds} seconds"
                                } else {
                                    val nextPos = ((vv?.currentPosition ?: currentPositionMs) + deltaMs)
                                        .coerceAtMost(totalDurationMs)
                                    vv?.seekTo(nextPos)
                                    currentPositionMs = nextPos
                                    seekRippleText = "⏩ +${doubleTapSeekSeconds} seconds"
                                }
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
                            mp.isLooping = isLoopVideo
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                runCatching {
                                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                }
                            }
                            if (isPlaying) start()
                        }
                        setOnCompletionListener {
                            if (isLoopVideo) {
                                start()
                            } else if (watchQueueVideos.isNotEmpty()) {
                                val nextQueued = watchQueueVideos.first()
                                viewModel.removeFromQueue(nextQueued.id)
                                viewModel.openVideo(nextQueued)
                            } else if (isAutoplayEnabled && upNextVideos.isNotEmpty()) {
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

            // Hold-to-2x Speed Pill at Top Center (Exact YouTube Gesture Feedback)
            if (isHolding2xSpeed) {
                Surface(
                    color = Color.Black.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2x",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Stats for Nerds Technical Overlay
            if (isStatsForNerds) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stats for nerds",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "✕",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { viewModel.toggleStatsForNerds() }
                            )
                        }
                        Text("Video ID / sCPN: ${video.id} / YT98K-4F2A", color = Color.LightGray, fontSize = 10.sp)
                        Text("Viewport / Frames: 1920x1080*2.75 / 0 dropped of ${(currentPositionMs / 16).coerceAtLeast(60)}", color = Color.LightGray, fontSize = 10.sp)
                        Text("Current / Optimal Res: $videoQuality / $videoQuality", color = Color.LightGray, fontSize = 10.sp)
                        Text("Codecs: vp09.00.51.08.01 (315) / mp4a.40.2 (140)", color = Color.LightGray, fontSize = 10.sp)
                        Text("Network Activity: 18,420 Kbps • Buffer Health: 24.5 s", color = Color(0xFF4CAF50), fontSize = 10.sp)
                    }
                }
            }

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

            // Manual SponsorBlock Skip Button (when in Show Skip Button mode)
            activeManualSkipSegment?.let { seg ->
                val cat = SponsorCategory.fromKey(seg.category)
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 52.dp)
                        .clickable {
                            val targetMs = (seg.endTimeSec * 1000).coerceAtMost(totalDurationMs)
                            videoViewRef?.seekTo(targetMs)
                            currentPositionMs = targetMs
                            viewModel.recordSponsorSkip(seg)
                            activeManualSkipSegment = null
                        }
                        .testTag("manual_sponsor_skip_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Skip ${cat.displayName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                                modifier = Modifier.fillMaxWidth()
                            )
                            // SponsorBlock Color-Coded Segment Bar Overlay
                            if (isSponsorBlockEnabled && sponsorSegments.isNotEmpty() && totalDurationMs > 0) {
                                val totalSec = (totalDurationMs / 1000f).coerceAtLeast(1f)
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp)
                                        .height(4.dp)
                                        .align(Alignment.Center)
                                ) {
                                    val barWidth = maxWidth
                                    sponsorSegments.forEach { seg ->
                                        val startFrac = (seg.startTimeSec / totalSec).coerceIn(0f, 1f)
                                        val endFrac = (seg.endTimeSec / totalSec).coerceIn(startFrac, 1f)
                                        val widthFrac = (endFrac - startFrac).coerceAtLeast(0.02f)
                                        val catColor = Color(SponsorCategory.fromKey(seg.category).colorHex)
                                        Box(
                                            modifier = Modifier
                                                .offset(x = barWidth * startFrac)
                                                .width(barWidth * widthFrac)
                                                .fillMaxHeight()
                                                .background(catColor)
                                        )
                                    }
                                }
                            }
                        }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { viewModel.toggleDislike(video) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("watch_dislike_button")
                                ) {
                                    Icon(
                                        imageVector = if (video.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                        contentDescription = "Dislike",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (video.isDisliked) YouTubeRed else MaterialTheme.colorScheme.onBackground
                                    )
                                    if (isReturnDislikeEnabled) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        val effectiveDislikes = video.dislikesCount.coerceAtLeast(video.likesCount / 38) + (if (video.isDisliked) 1 else 0)
                                        Text(
                                            text = formatCompactCount(effectiveDislikes),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SponsorBlock Submit Segment Pill
                    item {
                        WatchActionPill(
                            icon = Icons.Outlined.Shield,
                            label = "SponsorBlock (${sponsorSegments.size})",
                            onClick = { showSubmitSponsorSheet = true }
                        )
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

                    // Super Thanks Pill
                    item {
                        WatchActionPill(
                            icon = Icons.Outlined.VolunteerActivism,
                            label = "Thanks",
                            onClick = { showThanksSheet = true }
                        )
                    }

                    // Live Chat Pill (for Live streams)
                    if (video.isLive) {
                        item {
                            WatchActionPill(
                                icon = Icons.Outlined.Chat,
                                label = if (showLiveChat) "Hide chat" else "Live chat",
                                onClick = { showLiveChat = !showLiveChat }
                            )
                        }
                    }
                }
            }

            // Active Watch Queue Banner (if items are queued)
            if (watchQueueVideos.isNotEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clickable { showQueueSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Outlined.QueueMusic,
                                    contentDescription = null,
                                    tint = YouTubeRed
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Next in Queue (${watchQueueVideos.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = watchQueueVideos.first().title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "View",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Live Chat Stream Box (when watching a LIVE stream)
            if (video.isLive && showLiveChat) {
                item {
                    LiveChatStreamSection(
                        channelName = video.channelName,
                        onSendChatMessage = { msg ->
                            viewModel.addComment(video.id, "[LIVE CHAT] $msg")
                        }
                    )
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
                    onDownloadVideo = { viewModel.toggleDownload(nextVideo) },
                    onPlayNextInQueue = { viewModel.playNextInQueue(nextVideo) },
                    onNotInterested = { viewModel.markNotInterested(nextVideo.id) }
                )
            }
        }
    }

    // Super Thanks Bottom Sheet
    if (showThanksSheet) {
        var selectedAmount by remember { mutableStateOf("$5.00") }
        var thanksMessage by remember { mutableStateOf("Awesome video! Keep up the great work 🎉") }
        ModalBottomSheet(
            onDismissRequest = { showThanksSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Say Thanks to ${video.channelName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Buy a Super Thanks to directly support ${video.channelName} and stand out in the comments with a highlighted colorful badge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(listOf("$2.00", "$5.00", "$10.00", "$50.00")) { amt ->
                        FilterChip(
                            selected = selectedAmount == amt,
                            onClick = { selectedAmount = amt },
                            label = { Text(amt, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = thanksMessage,
                    onValueChange = { thanksMessage = it },
                    label = { Text("Your highlighted Super Thanks comment") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.addComment(video.id, "💖 [Super Thanks $selectedAmount • Unlocked Free] $thanksMessage")
                        showThanksSheet = false
                        viewModel.showSnackbar("Super Thanks ($selectedAmount) sent for FREE with UTube Unlocked!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Free Super Thanks ($selectedAmount — $0.00)", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // SponsorBlock Segments & Community Submission Sheet
    if (showSubmitSponsorSheet) {
        var selectedCat by remember { mutableStateOf(SponsorCategory.SPONSOR) }
        var startSecInput by remember { mutableStateOf((currentPositionMs / 1000).toString()) }
        var endSecInput by remember { mutableStateOf(((currentPositionMs / 1000) + 5).toString()) }

        ModalBottomSheet(
            onDismissRequest = { showSubmitSponsorSheet = false },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00D400)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SponsorBlock Segments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = isSponsorBlockEnabled,
                        onCheckedChange = { viewModel.toggleSponsorBlock() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active segments in this video (Tap any segment to skip past it immediately):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                sponsorSegments.forEach { seg ->
                    val cat = SponsorCategory.fromKey(seg.category)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                val targetMs = (seg.endTimeSec * 1000).coerceAtMost(totalDurationMs)
                                videoViewRef?.seekTo(targetMs)
                                currentPositionMs = targetMs
                                viewModel.recordSponsorSkip(seg)
                                showSubmitSponsorSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(cat.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${formatDuration(seg.startTimeSec)} – ${formatDuration(seg.endTimeSec)} • ${seg.votes} votes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "Skip >",
                                color = Color(0xFF00D400),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Submit New Segment to SponsorBlock Cloud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SponsorCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startSecInput,
                        onValueChange = { startSecInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Start (sec)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endSecInput,
                        onValueChange = { endSecInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("End (sec)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val s = startSecInput.toIntOrNull() ?: 0
                        val e = (endSecInput.toIntOrNull() ?: (s + 5)).coerceAtLeast(s + 1)
                        viewModel.submitSponsorSegment(video.id, selectedCat.key, s, e)
                        showSubmitSponsorSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D400)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Segment to Cloud", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Watch Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
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
                    Text(
                        text = "Queue (${watchQueueVideos.size} videos)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        viewModel.clearQueue()
                        showQueueSheet = false
                    }) {
                        Text("Clear")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(watchQueueVideos, key = { it.id }) { queuedVid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.removeFromQueue(queuedVid.id)
                                    showQueueSheet = false
                                    viewModel.openVideo(queuedVid)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CompactVideoRow(
                                    video = queuedVid,
                                    onClick = {
                                        viewModel.removeFromQueue(queuedVid.id)
                                        showQueueSheet = false
                                        viewModel.openVideo(queuedVid)
                                    }
                                )
                            }
                            IconButton(onClick = { viewModel.removeFromQueue(queuedVid.id) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove from queue")
                            }
                        }
                    }
                }
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

                Text("Quality (100% Unlocked Free)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val qualities = listOf(
                        "4K60 HDR (Unlocked Free)",
                        "1080p60 Enhanced Bitrate (Free)",
                        "1080p60",
                        "720p60",
                        "480p"
                    )
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

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Loop video", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Repeat this video continuously",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLoopVideo,
                        onCheckedChange = { viewModel.toggleLoopVideo() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Stable volume", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Balances range between quiet and loud audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isStableVolume,
                        onCheckedChange = { viewModel.toggleStableVolume() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Sleep timer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    items(listOf(0 to "Off", 15 to "15 min", 30 to "30 min", 60 to "60 min")) { (mins, label) ->
                        FilterChip(
                            selected = sleepTimerMinutes == mins,
                            onClick = { viewModel.setSleepTimer(mins) },
                            label = { Text(label) }
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
                        Text("Stats for nerds", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Display codec, bitrate, and viewport telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isStatsForNerds,
                        onCheckedChange = { viewModel.toggleStatsForNerds() }
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LiveChatStreamSection(
    channelName: String,
    onSendChatMessage: (String) -> Unit
) {
    var chatInput by remember { mutableStateOf("") }
    val liveMessages = remember {
        mutableStateListOf(
            "Alex_RTX" to "That analog sub-bass filter sweep is unreal 🔥",
            "SynthRider99" to "Listening from Tokyo at 4AM while writing Kotlin!",
            "ElenaVance" to "Can we get a closeup of the Prophet-6 patch bay?",
            "Kaelen_VFX" to "Audio quality on this stream is crystal clear 10/10"
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(YouTubeRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live chat • Top messages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "4.2K watching",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            liveMessages.takeLast(5).forEach { (user, msg) ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "$user: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Chat publicly as Christian Studio...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            liveMessages.add("Christian Studio" to chatInput.trim())
                            onSendChatMessage(chatInput.trim())
                            chatInput = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send live chat")
                }
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
