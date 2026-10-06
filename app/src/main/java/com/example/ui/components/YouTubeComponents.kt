package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.theme.OswaldFontFamily
import com.example.ui.theme.YouTubeLiveBadge
import com.example.ui.theme.YouTubeRed

@Composable
fun resolveThumbnailDrawable(resName: String): Int {
    return when {
        resName.contains("tech") -> R.drawable.img_thumb_tech_1791326396393
        resName.contains("gaming") -> R.drawable.img_thumb_gaming_1791326409204
        resName.contains("cooking") -> R.drawable.img_thumb_cooking_1791326420737
        resName.contains("nature") -> R.drawable.img_thumb_nature_1791326431475
        else -> R.drawable.img_thumb_tech_1791326396393
    }
}

@Composable
fun VideoThumbnailImage(
    video: VideoEntity,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (video.customThumbnailUri.isNotBlank()) {
        AsyncImage(
            model = video.customThumbnailUri,
            contentDescription = video.title,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        val drawableRes = resolveThumbnailDrawable(video.thumbnailResName)
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = video.title,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun ChannelAvatarBadge(
    name: String,
    colorLong: Long,
    size: Dp = 36.dp,
    isLive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "YT" }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(colorLong),
                        Color(colorLong).copy(alpha = 0.7f)
                    )
                )
            )
            .then(
                if (isLive) Modifier.border(2.dp, YouTubeRed, CircleShape) else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

@Composable
fun YouTubeTopBar(
    unreadNotificationsCount: Int,
    isIncognito: Boolean,
    onCastClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // YouTube Play Pill + Oswald Wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onLogoClick() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("youtube_logo_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 21.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(YouTubeRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YouTube",
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.6).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isIncognito) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "INCOGNITO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Action Icons: Cast, Notifications with badge, Search
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCastClick,
                    modifier = Modifier.testTag("top_cast_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tv,
                        contentDescription = "Cast to TV",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("top_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    containerColor = YouTubeRed,
                                    contentColor = Color.White
                                ) {
                                    Text(unreadNotificationsCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search YouTube",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onExploreShortsClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onExploreShortsClick() }
                    .testTag("explore_compass_chip")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = "Explore Trending",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        items(categories) { category ->
            val isSelected = category == selectedCategory
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onSelectCategory(category) }
                    .testTag("category_chip_${category.lowercase().replace(" ", "_")}")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeVideoCard(
    video: VideoEntity,
    onVideoClick: () -> Unit,
    onChannelClick: () -> Unit,
    onSaveToWatchLater: () -> Unit,
    onSaveToPlaylist: () -> Unit,
    onDownloadVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onVideoClick() }
            .padding(bottom = 16.dp)
            .testTag("video_card_${video.id}")
    ) {
        // 16:9 Thumbnail Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            VideoThumbnailImage(
                video = video,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle bottom vignette for badge legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            // Duration or LIVE badge
            Surface(
                color = if (video.isLive) YouTubeLiveBadge else Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    if (video.isLive) {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (video.isLive) "LIVE" else formatDuration(video.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Red Watch Progress Bar at bottom of thumbnail
            if (video.watchProgressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { video.watchProgressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = YouTubeRed,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        // Metadata Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            ChannelAvatarBadge(
                name = video.channelName,
                colorLong = video.channelAvatarColor,
                size = 38.dp,
                isLive = video.isLive,
                onClick = onChannelClick
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${video.channelName} • ${formatCompactCount(video.viewsCount)} views • ${video.publishedTimeText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.testTag("video_menu_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (video.isSavedToWatchLater) "Remove from Watch Later" else "Save to Watch Later") },
                        leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onSaveToWatchLater()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save to playlist") },
                        leadingIcon = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onSaveToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (video.isDownloaded) "Remove download" else "Download video") },
                        leadingIcon = {
                            Icon(
                                if (video.isDownloaded) Icons.Filled.DownloadDone else Icons.Outlined.Download,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onDownloadVideo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on YouTube: ${video.videoUrl}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share video"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ShortsShelfSection(
    shorts: List<VideoEntity>,
    onShortClick: (VideoEntity) -> Unit
) {
    if (shorts.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(YouTubeRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Shorts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shorts, key = { it.id }) { shortVideo ->
                Box(
                    modifier = Modifier
                        .width(152.dp)
                        .height(256.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShortClick(shortVideo) }
                        .testTag("shelf_short_${shortVideo.id}")
                ) {
                    VideoThumbnailImage(
                        video = shortVideo,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = shortVideo.title,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatCompactCount(shortVideo.viewsCount)} views",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPostEntity,
    onVote: (Int) -> Unit,
    onLike: () -> Unit,
    onChannelClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelAvatarBadge(
                    name = post.channelName,
                    colorLong = post.channelAvatarColor,
                    size = 36.dp,
                    onClick = onChannelClick
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.channelName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = post.timestampText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.contentText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            val options = post.pollOptionsPipe.split("|").filter { it.isNotBlank() }
            val votes = post.pollVotesPipe.split("|").mapNotNull { it.toIntOrNull() }
            if (options.isNotEmpty() && votes.size == options.size) {
                val totalVotes = votes.sum().coerceAtLeast(1)
                Spacer(modifier = Modifier.height(12.dp))
                options.forEachIndexed { index, optionText ->
                    val optionVotes = votes[index]
                    val fraction = optionVotes.toFloat() / totalVotes.toFloat()
                    val pct = (fraction * 100).toInt()
                    val isSelected = post.selectedPollIndex == index

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onVote(index) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                                .height(42.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatCompactCount(totalVotes.toLong())} votes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like post",
                        tint = if (post.isLiked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = formatCompactCount(post.likesCount.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(24.dp))
                Icon(
                    imageVector = Icons.Outlined.Comment,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatCompactCount(post.commentsCount.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
