package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class YouTubeTab {
    HOME, SHORTS, SUBSCRIPTIONS, YOU
}

enum class SubFilter {
    ALL, TODAY, VIDEOS, SHORTS, LIVE, POSTS
}

@OptIn(ExperimentalCoroutinesApi::class)
class YouTubeViewModel(private val repository: YouTubeRepository) : ViewModel() {

    // Navigation & UI state
    private val _selectedTab = MutableStateFlow(YouTubeTab.HOME)
    val selectedTab: StateFlow<YouTubeTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories = listOf(
        "All", "New to you", "Tech", "Gaming", "Cooking", "Nature", "Music", "Movies", "Live", "Recently uploaded", "Watched"
    )

    // Search state
    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchDurationFilter = MutableStateFlow("Any") // Any, Under 4 mins, 4-20 mins
    val searchDurationFilter: StateFlow<String> = _searchDurationFilter.asStateFlow()

    // Notifications Sheet / Screen
    private val _isNotificationsOpen = MutableStateFlow(false)
    val isNotificationsOpen: StateFlow<Boolean> = _isNotificationsOpen.asStateFlow()

    // Create / Upload Bottom Sheet
    private val _isCreateSheetOpen = MutableStateFlow(false)
    val isCreateSheetOpen: StateFlow<Boolean> = _isCreateSheetOpen.asStateFlow()

    // Channel Profile Page
    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    // Active Video Player & Mini-player
    private val _currentPlayingVideoId = MutableStateFlow<String?>(null)
    val currentPlayingVideoId: StateFlow<String?> = _currentPlayingVideoId.asStateFlow()

    private val _isPlayerMinimized = MutableStateFlow(false)
    val isPlayerMinimized: StateFlow<Boolean> = _isPlayerMinimized.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _videoQuality = MutableStateFlow("1080p60 Premium")
    val videoQuality: StateFlow<String> = _videoQuality.asStateFlow()

    private val _isAmbientMode = MutableStateFlow(true)
    val isAmbientMode: StateFlow<Boolean> = _isAmbientMode.asStateFlow()

    private val _isCaptionsEnabled = MutableStateFlow(false)
    val isCaptionsEnabled: StateFlow<Boolean> = _isCaptionsEnabled.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    // Save to Playlist Dialog
    private val _saveToPlaylistVideoId = MutableStateFlow<String?>(null)
    val saveToPlaylistVideoId: StateFlow<String?> = _saveToPlaylistVideoId.asStateFlow()

    // Theme & Settings
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()

    // Subscriptions filter
    private val _selectedSubChannelId = MutableStateFlow<String?>(null)
    val selectedSubChannelId: StateFlow<String?> = _selectedSubChannelId.asStateFlow()

    private val _selectedSubFilter = MutableStateFlow(SubFilter.ALL)
    val selectedSubFilter: StateFlow<SubFilter> = _selectedSubFilter.asStateFlow()

    // Snackbar feedback message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Database Flows
    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortsVideos: StateFlow<List<VideoEntity>> = repository.shortsVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<VideoEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedVideos: StateFlow<List<VideoEntity>> = repository.likedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchLaterVideos: StateFlow<List<VideoEntity>> = repository.watchLaterVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedVideos: StateFlow<List<VideoEntity>> = repository.downloadedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscribedChannels: StateFlow<List<ChannelEntity>> = repository.subscribedChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityPosts: StateFlow<List<CommunityPostEntity>> = repository.communityPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchQueryEntity>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredHomeVideos: StateFlow<List<VideoEntity>> = combine(
        repository.homeVideos,
        _selectedCategory
    ) { videos, category ->
        when (category) {
            "All", "New to you" -> videos
            "Live" -> videos.filter { it.isLive }
            "Watched" -> videos.filter { it.lastWatchedTimestamp > 0L }
            "Recently uploaded" -> videos.sortedBy { it.publishedTimeText }
            else -> videos.filter { it.category.equals(category, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<VideoEntity>> = combine(
        allVideos,
        _searchQuery,
        _searchDurationFilter
    ) { videos, query, durationFilter ->
        val q = query.trim()
        val matched = if (q.isEmpty()) {
            videos
        } else {
            videos.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.channelName.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true)
            }
        }
        when (durationFilter) {
            "Under 4 mins" -> matched.filter { it.durationSeconds < 240 }
            "4-20 mins" -> matched.filter { it.durationSeconds in 240..1200 }
            "Shorts" -> matched.filter { it.isShort }
            else -> matched
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPlayingVideo: StateFlow<VideoEntity?> = combine(
        allVideos,
        _currentPlayingVideoId
    ) { videos, currentId ->
        videos.find { it.id == currentId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentVideoComments: StateFlow<List<CommentEntity>> = _currentPlayingVideoId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCommentsForVideo(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    fun selectTab(tab: YouTubeTab) {
        _selectedTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun openVideo(video: VideoEntity) {
        if (video.isShort) {
            _selectedTab.value = YouTubeTab.SHORTS
            _currentPlayingVideoId.value = null
            return
        }
        _currentPlayingVideoId.value = video.id
        _isPlayerMinimized.value = false
        _isPlaying.value = true
        if (!_isIncognito.value) {
            viewModelScope.launch {
                val nextProgress = if (video.watchProgressFraction > 0.1f) video.watchProgressFraction else 0.25f
                repository.recordWatchProgress(video, nextProgress)
            }
        }
    }

    fun minimizePlayer() {
        if (_currentPlayingVideoId.value != null) {
            _isPlayerMinimized.value = true
        }
    }

    fun expandPlayer() {
        if (_currentPlayingVideoId.value != null) {
            _isPlayerMinimized.value = false
        }
    }

    fun closePlayer() {
        _currentPlayingVideoId.value = null
        _isPlayerMinimized.value = false
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        showSnackbar("Playback speed set to ${speed}x")
    }

    fun setVideoQuality(quality: String) {
        _videoQuality.value = quality
        showSnackbar("Quality changed to $quality")
    }

    fun toggleAmbientMode() {
        _isAmbientMode.value = !_isAmbientMode.value
        showSnackbar(if (_isAmbientMode.value) "Ambient mode On" else "Ambient mode Off")
    }

    fun toggleCaptions() {
        _isCaptionsEnabled.value = !_isCaptionsEnabled.value
        showSnackbar(if (_isCaptionsEnabled.value) "Captions turned on (English auto-generated)" else "Captions turned off")
    }

    fun toggleAutoplay() {
        _isAutoplayEnabled.value = !_isAutoplayEnabled.value
    }

    fun updateWatchProgress(video: VideoEntity, fraction: Float) {
        if (!_isIncognito.value) {
            viewModelScope.launch {
                repository.recordWatchProgress(video, fraction)
            }
        }
    }

    fun toggleLike(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleLike(video)
            if (!video.isLiked) showSnackbar("Added to Liked videos")
        }
    }

    fun toggleDislike(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleDislike(video)
        }
    }

    fun toggleWatchLater(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleWatchLater(video)
            showSnackbar(
                if (!video.isSavedToWatchLater) "Saved to Watch Later" else "Removed from Watch Later"
            )
        }
    }

    fun toggleDownload(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleDownload(video)
            showSnackbar(
                if (!video.isDownloaded) "Downloading '${video.title.take(24)}...' for offline viewing"
                else "Removed from Downloads"
            )
        }
    }

    fun toggleSubscription(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleSubscription(channel)
            showSnackbar(
                if (!channel.isSubscribed) "Subscribed to ${channel.name}"
                else "Unsubscribed from ${channel.name}"
            )
        }
    }

    fun toggleSubscriptionByChannelId(channelId: String, channelName: String, avatarColor: Long) {
        viewModelScope.launch {
            val existing = allChannels.value.find { it.id == channelId }
            if (existing != null) {
                toggleSubscription(existing)
            } else {
                val created = ChannelEntity(
                    id = channelId,
                    name = channelName,
                    handle = "@${channelName.replace(" ", "")}",
                    avatarColor = avatarColor,
                    bannerColor = avatarColor,
                    subscriberCount = 12_500,
                    videosCount = 1,
                    bio = "Creator channel on YouTube.",
                    isVerified = true,
                    isSubscribed = true
                )
                repository.toggleSubscription(created)
                showSnackbar("Subscribed to $channelName")
            }
        }
    }

    fun cycleBellMode(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.cycleBellMode(channel)
            val nextLabel = when (channel.notificationBellMode) {
                "ALL" -> "Personalized notifications"
                "PERSONALIZED" -> "Notifications turned off"
                else -> "All notifications turned on"
            }
            showSnackbar(nextLabel)
        }
    }

    fun addComment(videoId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(
                videoId = videoId,
                authorName = "Christian Studio",
                authorHandle = "@christianjaydelica",
                text = text
            )
            showSnackbar("Comment posted")
        }
    }

    fun toggleCommentLike(comment: CommentEntity) {
        viewModelScope.launch {
            repository.toggleCommentLike(comment)
        }
    }

    fun openSaveToPlaylistDialog(videoId: String) {
        _saveToPlaylistVideoId.value = videoId
    }

    fun closeSaveToPlaylistDialog() {
        _saveToPlaylistVideoId.value = null
    }

    fun createPlaylist(title: String, description: String, privacy: String, initialVideoId: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(title, description, privacy, initialVideoId)
            showSnackbar("Playlist '$title' created")
        }
    }

    fun toggleVideoInPlaylist(playlist: PlaylistEntity, videoId: String) {
        viewModelScope.launch {
            repository.toggleVideoInPlaylist(playlist, videoId)
            showSnackbar("Updated playlist '${playlist.title}'")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            showSnackbar("Playlist deleted")
        }
    }

    fun voteOnPoll(post: CommunityPostEntity, optionIndex: Int) {
        viewModelScope.launch {
            repository.voteOnPoll(post, optionIndex)
        }
    }

    fun togglePostLike(post: CommunityPostEntity) {
        viewModelScope.launch {
            repository.togglePostLike(post)
        }
    }

    fun createCommunityPost(contentText: String, pollOptions: List<String>) {
        if (contentText.isBlank()) return
        viewModelScope.launch {
            repository.createCommunityPost(
                channelName = "Christian Studio",
                channelHandle = "@christianjaydelica",
                contentText = contentText,
                pollOptions = pollOptions
            )
            showSnackbar("Community post published!")
        }
    }

    fun uploadUserVideo(
        title: String,
        description: String,
        category: String,
        isShort: Boolean,
        customThumbnailUri: String,
        thumbnailPreset: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val uploaded = repository.uploadUserVideo(
                title = title,
                description = description,
                category = category,
                isShort = isShort,
                customThumbnailUri = customThumbnailUri,
                thumbnailPreset = thumbnailPreset
            )
            _isCreateSheetOpen.value = false
            showSnackbar(if (isShort) "Short uploaded to Shorts feed!" else "Video published to your channel!")
            if (isShort) {
                _selectedTab.value = YouTubeTab.SHORTS
            } else {
                openVideo(uploaded)
            }
        }
    }

    fun openSearch() {
        _isSearchOpen.value = true
    }

    fun closeSearch() {
        _isSearchOpen.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.addSearchQuery(query)
        }
    }

    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(query)
        }
    }

    fun setSearchDurationFilter(filter: String) {
        _searchDurationFilter.value = filter
    }

    fun openNotifications() {
        _isNotificationsOpen.value = true
    }

    fun closeNotifications() {
        _isNotificationsOpen.value = false
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            showSnackbar("All notifications marked as read")
        }
    }

    fun openCreateSheet() {
        _isCreateSheetOpen.value = true
    }

    fun closeCreateSheet() {
        _isCreateSheetOpen.value = false
    }

    fun openChannelProfile(channelId: String) {
        _selectedChannelId.value = channelId
    }

    fun closeChannelProfile() {
        _selectedChannelId.value = null
    }

    fun selectSubChannel(channelId: String?) {
        _selectedSubChannelId.value = if (_selectedSubChannelId.value == channelId) null else channelId
    }

    fun selectSubFilter(filter: SubFilter) {
        _selectedSubFilter.value = filter
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
            showSnackbar("Watch history cleared")
        }
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleIncognito() {
        _isIncognito.value = !_isIncognito.value
        showSnackbar(if (_isIncognito.value) "Turned on Incognito mode" else "Turned off Incognito mode")
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    class Factory(private val repository: YouTubeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return YouTubeViewModel(repository) as T
        }
    }
}
