package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val thumbnailResName: String,
    val customThumbnailUri: String = "",
    val channelId: String,
    val channelName: String,
    val channelHandle: String,
    val channelAvatarColor: Long,
    val subscriberCountText: String,
    val viewsCount: Long,
    val likesCount: Long,
    val dislikesCount: Long = 0,
    val publishedTimeText: String,
    val durationSeconds: Int,
    val category: String,
    val isShort: Boolean = false,
    val isLive: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isSavedToWatchLater: Boolean = false,
    val isDownloaded: Boolean = false,
    val watchProgressFraction: Float = 0f,
    val lastWatchedTimestamp: Long = 0L,
    val audioTrackTitle: String = "Original Audio",
    val chaptersJson: String = "", // Format: "0:00 Intro|1:15 Setup|3:40 Deep Dive|7:10 Verdict"
    val isUserUploaded: Boolean = false
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val handle: String,
    val avatarColor: Long,
    val bannerColor: Long,
    val subscriberCount: Long,
    val videosCount: Int,
    val bio: String,
    val isVerified: Boolean = true,
    val isSubscribed: Boolean = false,
    val notificationBellMode: String = "ALL" // ALL, PERSONALIZED, NONE
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarColor: Long,
    val text: String,
    val timestampText: String,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val isHeartedByCreator: Boolean = false,
    val isPinned: Boolean = false,
    val replyCount: Int = 0,
    val parentCommentId: Long? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val privacy: String = "Public", // Public, Unlisted, Private
    val videoIdsCsv: String = "", // Comma-separated video IDs
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val channelName: String,
    val channelHandle: String,
    val channelAvatarColor: Long,
    val timestampText: String,
    val contentText: String,
    val pollOptionsPipe: String = "", // "Option A|Option B|Option C"
    val pollVotesPipe: String = "",   // "450|1200|310"
    val selectedPollIndex: Int = -1,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0
)

@Entity(tableName = "search_history")
data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val channelAvatarColor: Long,
    val title: String,
    val timeAgoText: String,
    val videoId: String,
    val thumbnailResName: String,
    val isRead: Boolean = false
)

data class VideoChapter(
    val timeSeconds: Int,
    val timestampLabel: String,
    val title: String
)

fun VideoEntity.parseChapters(): List<VideoChapter> {
    if (chaptersJson.isBlank()) return emptyList()
    return chaptersJson.split("|").mapNotNull { entry ->
        val parts = entry.trim().split(" ", limit = 2)
        if (parts.size == 2) {
            val timeParts = parts[0].split(":")
            val seconds = if (timeParts.size == 2) {
                (timeParts[0].toIntOrNull() ?: 0) * 60 + (timeParts[1].toIntOrNull() ?: 0)
            } else 0
            VideoChapter(
                timeSeconds = seconds,
                timestampLabel = parts[0],
                title = parts[1]
            )
        } else null
    }
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "LIVE"
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%d:%02d", mins, secs)
    }
}

fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0).replace(".0M", "M")
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0).replace(".0K", "K")
        else -> count.toString()
    }
}
