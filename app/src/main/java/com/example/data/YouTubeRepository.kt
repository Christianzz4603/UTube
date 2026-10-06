package com.example.data

import kotlinx.coroutines.flow.Flow

class YouTubeRepository(private val dao: YouTubeDao) {
    val homeVideos: Flow<List<VideoEntity>> = dao.getHomeVideos()
    val shortsVideos: Flow<List<VideoEntity>> = dao.getShortsVideos()
    val allVideos: Flow<List<VideoEntity>> = dao.getAllVideos()
    val watchHistory: Flow<List<VideoEntity>> = dao.getWatchHistory()
    val likedVideos: Flow<List<VideoEntity>> = dao.getLikedVideos()
    val watchLaterVideos: Flow<List<VideoEntity>> = dao.getWatchLaterVideos()
    val downloadedVideos: Flow<List<VideoEntity>> = dao.getDownloadedVideos()
    val allChannels: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val subscribedChannels: Flow<List<ChannelEntity>> = dao.getSubscribedChannels()
    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val communityPosts: Flow<List<CommunityPostEntity>> = dao.getAllCommunityPosts()
    val searchHistory: Flow<List<SearchQueryEntity>> = dao.getSearchHistory()
    val notifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()

    suspend fun ensureSeedData() {
        if (dao.getVideoCount() == 0) {
            dao.insertChannels(SeedData.initialChannels)
            dao.insertVideos(SeedData.initialVideos)
            dao.insertComments(SeedData.initialComments)
            dao.insertPlaylists(SeedData.initialPlaylists)
            dao.insertCommunityPosts(SeedData.initialCommunityPosts)
            SeedData.initialSearchHistory.forEach { dao.insertSearchQuery(it) }
            dao.insertNotifications(SeedData.initialNotifications)
        }
    }

    fun getCommentsForVideo(videoId: String): Flow<List<CommentEntity>> =
        dao.getCommentsForVideo(videoId)

    suspend fun toggleLike(video: VideoEntity) {
        val nowLiked = !video.isLiked
        val newLikes = if (nowLiked) video.likesCount + 1 else (video.likesCount - 1).coerceAtLeast(0)
        dao.updateVideo(
            video.copy(
                isLiked = nowLiked,
                likesCount = newLikes,
                isDisliked = if (nowLiked) false else video.isDisliked
            )
        )
    }

    suspend fun toggleDislike(video: VideoEntity) {
        val nowDisliked = !video.isDisliked
        val wasLiked = video.isLiked
        val newLikes = if (nowDisliked && wasLiked) (video.likesCount - 1).coerceAtLeast(0) else video.likesCount
        dao.updateVideo(
            video.copy(
                isDisliked = nowDisliked,
                isLiked = if (nowDisliked) false else video.isLiked,
                likesCount = newLikes
            )
        )
    }

    suspend fun toggleWatchLater(video: VideoEntity) {
        dao.updateVideo(video.copy(isSavedToWatchLater = !video.isSavedToWatchLater))
    }

    suspend fun toggleDownload(video: VideoEntity) {
        dao.updateVideo(video.copy(isDownloaded = !video.isDownloaded))
    }

    suspend fun recordWatchProgress(video: VideoEntity, progressFraction: Float) {
        dao.updateVideo(
            video.copy(
                watchProgressFraction = progressFraction.coerceIn(0.05f, 1f),
                lastWatchedTimestamp = System.currentTimeMillis(),
                viewsCount = video.viewsCount + 1
            )
        )
    }

    suspend fun clearWatchHistory() {
        dao.clearWatchHistory()
    }

    suspend fun toggleSubscription(channel: ChannelEntity) {
        val nowSubscribed = !channel.isSubscribed
        val newSubs = if (nowSubscribed) channel.subscriberCount + 1 else (channel.subscriberCount - 1).coerceAtLeast(0)
        dao.updateChannel(channel.copy(isSubscribed = nowSubscribed, subscriberCount = newSubs))
    }

    suspend fun cycleBellMode(channel: ChannelEntity) {
        val nextMode = when (channel.notificationBellMode) {
            "ALL" -> "PERSONALIZED"
            "PERSONALIZED" -> "NONE"
            else -> "ALL"
        }
        dao.updateChannel(channel.copy(notificationBellMode = nextMode))
    }

    suspend fun addComment(videoId: String, authorName: String, authorHandle: String, text: String) {
        dao.insertComment(
            CommentEntity(
                videoId = videoId,
                authorName = authorName,
                authorHandle = authorHandle,
                authorAvatarColor = 0xFF00ACC1,
                text = text.trim(),
                timestampText = "Just now",
                likesCount = 1,
                isLiked = true
            )
        )
    }

    suspend fun toggleCommentLike(comment: CommentEntity) {
        val nowLiked = !comment.isLiked
        val newLikes = if (nowLiked) comment.likesCount + 1 else (comment.likesCount - 1).coerceAtLeast(0)
        dao.updateComment(comment.copy(isLiked = nowLiked, likesCount = newLikes))
    }

    suspend fun createPlaylist(title: String, description: String, privacy: String, initialVideoId: String? = null) {
        dao.insertPlaylist(
            PlaylistEntity(
                title = title.trim(),
                description = description.trim(),
                privacy = privacy,
                videoIdsCsv = initialVideoId ?: ""
            )
        )
    }

    suspend fun toggleVideoInPlaylist(playlist: PlaylistEntity, videoId: String) {
        val currentIds = playlist.videoIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (currentIds.contains(videoId)) {
            currentIds.remove(videoId)
        } else {
            currentIds.add(videoId)
        }
        dao.updatePlaylist(playlist.copy(videoIdsCsv = currentIds.joinToString(",")))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun voteOnPoll(post: CommunityPostEntity, optionIndex: Int) {
        val votes = post.pollVotesPipe.split("|").mapNotNull { it.toIntOrNull() }.toMutableList()
        if (optionIndex in votes.indices) {
            if (post.selectedPollIndex in votes.indices && post.selectedPollIndex != optionIndex) {
                votes[post.selectedPollIndex] = (votes[post.selectedPollIndex] - 1).coerceAtLeast(0)
            }
            if (post.selectedPollIndex != optionIndex) {
                votes[optionIndex] = votes[optionIndex] + 1
            }
            dao.updateCommunityPost(
                post.copy(
                    selectedPollIndex = optionIndex,
                    pollVotesPipe = votes.joinToString("|")
                )
            )
        }
    }

    suspend fun togglePostLike(post: CommunityPostEntity) {
        val nowLiked = !post.isLiked
        val newLikes = if (nowLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
        dao.updateCommunityPost(post.copy(isLiked = nowLiked, likesCount = newLikes))
    }

    suspend fun createCommunityPost(channelName: String, channelHandle: String, contentText: String, pollOptions: List<String>) {
        val cleanOptions = pollOptions.map { it.trim() }.filter { it.isNotEmpty() }
        val votesPipe = if (cleanOptions.isNotEmpty()) cleanOptions.joinToString("|") { "1" } else ""
        dao.insertCommunityPost(
            CommunityPostEntity(
                channelId = "ch_user_creator",
                channelName = channelName,
                channelHandle = channelHandle,
                channelAvatarColor = 0xFF00ACC1,
                timestampText = "Just now",
                contentText = contentText.trim(),
                pollOptionsPipe = cleanOptions.joinToString("|"),
                pollVotesPipe = votesPipe,
                selectedPollIndex = if (cleanOptions.isNotEmpty()) 0 else -1,
                likesCount = 1,
                isLiked = true,
                commentsCount = 0
            )
        )
    }

    suspend fun uploadUserVideo(
        title: String,
        description: String,
        category: String,
        isShort: Boolean,
        customThumbnailUri: String,
        thumbnailPreset: String
    ): VideoEntity {
        val newVideo = VideoEntity(
            id = "vid_user_${System.currentTimeMillis()}",
            title = title.trim(),
            description = description.trim().ifBlank { "Uploaded via YouTube Studio on Android." },
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailResName = thumbnailPreset,
            customThumbnailUri = customThumbnailUri,
            channelId = "ch_user_creator",
            channelName = "Christian Studio",
            channelHandle = "@christianjaydelica",
            channelAvatarColor = 0xFF00ACC1,
            subscriberCountText = "12.4K subscribers",
            viewsCount = 1,
            likesCount = 1,
            publishedTimeText = "Just now",
            durationSeconds = 15,
            category = category,
            isShort = isShort,
            isLiked = true,
            isUserUploaded = true,
            chaptersJson = "0:00 Intro|0:05 Main Highlight|0:11 Outro"
        )
        dao.insertVideo(newVideo)
        return newVideo
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isNotBlank()) {
            dao.insertSearchQuery(SearchQueryEntity(query.trim(), System.currentTimeMillis()))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        dao.deleteSearchQuery(query)
    }

    suspend fun markNotificationRead(id: Long) {
        dao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsRead()
    }
}
