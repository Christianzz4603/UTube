package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
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
import com.example.data.CommentEntity
import com.example.data.VideoEntity
import com.example.data.formatCompactCount
import com.example.ui.YouTubeViewModel
import com.example.ui.components.ChannelAvatarBadge
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.theme.YouTubeRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: YouTubeViewModel
) {
    val shorts by viewModel.shortsVideos.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val context = LocalContext.current

    var activeCommentsShort by remember { mutableStateOf<VideoEntity?>(null) }

    if (shorts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = YouTubeRed)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { shorts.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_screen")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val shortVideo = shorts[page]
            val isCurrentPage = pagerState.currentPage == page
            val channel = allChannels.find { it.id == shortVideo.channelId }
            val isSubscribed = channel?.isSubscribed ?: false

            ShortVideoPage(
                shortVideo = shortVideo,
                isActive = isCurrentPage,
                isSubscribed = isSubscribed,
                onLike = { viewModel.toggleLike(shortVideo) },
                onDislike = { viewModel.toggleDislike(shortVideo) },
                onOpenComments = { activeCommentsShort = shortVideo },
                onShare = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Watch this Short: ${shortVideo.title} ${shortVideo.videoUrl}")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Short"))
                },
                onRemix = {
                    viewModel.openCreateSheet()
                },
                onSubscribeToggle = {
                    viewModel.toggleSubscriptionByChannelId(
                        shortVideo.channelId,
                        shortVideo.channelName,
                        shortVideo.channelAvatarColor
                    )
                },
                onChannelClick = {
                    viewModel.openChannelProfile(shortVideo.channelId)
                }
            )
        }

        // Top overlay header (Shorts title + Search + Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shorts",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = { viewModel.openSearch() }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search Shorts", tint = Color.White)
                }
                IconButton(onClick = { viewModel.openCreateSheet() }) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Create Short", tint = Color.White)
                }
            }
        }
    }

    // Comments Bottom Sheet for Shorts
    activeCommentsShort?.let { targetShort ->
        ModalBottomShapeCommentsSheet(
            videoId = targetShort.id,
            viewModel = viewModel,
            onDismiss = { activeCommentsShort = null }
        )
    }
}

@Composable
private fun ShortVideoPage(
    shortVideo: VideoEntity,
    isActive: Boolean,
    isSubscribed: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onRemix: () -> Unit,
    onSubscribeToggle: () -> Unit,
    onChannelClick: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }
    var isVideoReady by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(750)
            showHeartBurst = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(shortVideo.id) {
                detectTapGestures(
                    onTap = { isPaused = !isPaused },
                    onDoubleTap = {
                        if (!shortVideo.isLiked) onLike()
                        showHeartBurst = true
                    }
                )
            }
    ) {
        // Fallback high-res thumbnail behind the video stream
        VideoThumbnailImage(
            video = shortVideo,
            modifier = Modifier.fillMaxSize()
        )

        if (isActive) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(shortVideo.videoUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            isVideoReady = true
                            if (!isPaused) start()
                        }
                        setOnErrorListener { _, _, _ ->
                            isVideoReady = false
                            true
                        }
                    }
                },
                update = { view ->
                    if (isPaused && view.isPlaying) {
                        view.pause()
                    } else if (!isPaused && !view.isPlaying && isVideoReady) {
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom gradient overlay for legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Center Play/Pause indicator
        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Short",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Double-tap Heart Burst Animation
        AnimatedVisibility(
            visible = showHeartBurst,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = null,
                tint = YouTubeRed,
                modifier = Modifier.size(96.dp)
            )
        }

        // Right-side Vertical Action Rail (Like, Dislike, Comment, Share, Remix, Spinning Audio Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ShortActionItem(
                icon = if (shortVideo.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                label = formatCompactCount(shortVideo.likesCount),
                tint = if (shortVideo.isLiked) YouTubeRed else Color.White,
                onClick = onLike,
                testTag = "short_like_${shortVideo.id}"
            )

            ShortActionItem(
                icon = if (shortVideo.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                label = "Dislike",
                tint = Color.White,
                onClick = onDislike,
                testTag = "short_dislike_${shortVideo.id}"
            )

            ShortActionItem(
                icon = Icons.AutoMirrored.Filled.Comment,
                label = "412",
                tint = Color.White,
                onClick = onOpenComments,
                testTag = "short_comments_${shortVideo.id}"
            )

            ShortActionItem(
                icon = Icons.Outlined.Share,
                label = "Share",
                tint = Color.White,
                onClick = onShare,
                testTag = "short_share_${shortVideo.id}"
            )

            ShortActionItem(
                icon = Icons.Outlined.AutoAwesomeMotion,
                label = "Remix",
                tint = Color.White,
                onClick = onRemix,
                testTag = "short_remix_${shortVideo.id}"
            )

            // Audio Track Album Square
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .background(Color(shortVideo.channelAvatarColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Audio Track",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom-left Channel Info, Subscribe Pill, Caption & Audio Marquee
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.80f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelAvatarBadge(
                    name = shortVideo.channelName,
                    colorLong = shortVideo.channelAvatarColor,
                    size = 36.dp,
                    onClick = onChannelClick
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = shortVideo.channelHandle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onChannelClick() }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    color = if (isSubscribed) Color.White.copy(alpha = 0.22f) else Color.White,
                    contentColor = if (isSubscribed) Color.White else Color.Black,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { onSubscribeToggle() }
                        .testTag("short_subscribe_${shortVideo.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = shortVideo.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = shortVideo.audioTrackTitle,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Red Thin Progress Indicator at Bottom of Short
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            color = YouTubeRed
        )
    }
}

@Composable
private fun ShortActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomShapeCommentsSheet(
    videoId: String,
    viewModel: YouTubeViewModel,
    onDismiss: () -> Unit
) {
    val allComments by viewModel.allVideos.collectAsState()
    var commentText by remember { mutableStateOf("") }
    // Observe comments specifically for this videoId
    val commentsFlow = remember(videoId) { viewModel.currentVideoComments }
    val comments by commentsFlow.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 520.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(videoId, commentText)
                            commentText = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "Be the first to comment on this Short!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { c ->
                        CommentRowItem(
                            comment = c,
                            onLike = { viewModel.toggleCommentLike(c) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRowItem(
    comment: CommentEntity,
    onLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        ChannelAvatarBadge(
            name = comment.authorName,
            colorLong = comment.authorAvatarColor,
            size = 32.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (comment.isPinned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pinned by creator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${comment.authorHandle} • ${comment.timestampText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLike() }
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like comment",
                        modifier = Modifier.size(16.dp),
                        tint = if (comment.isLiked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCompactCount(comment.likesCount.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Icon(
                    imageVector = Icons.Outlined.ThumbDown,
                    contentDescription = "Dislike comment",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (comment.isHeartedByCreator) {
                    Spacer(modifier = Modifier.width(18.dp))
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Hearted by creator",
                        modifier = Modifier.size(15.dp),
                        tint = YouTubeRed
                    )
                }
                if (comment.replyCount > 0) {
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = "${comment.replyCount} replies",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
