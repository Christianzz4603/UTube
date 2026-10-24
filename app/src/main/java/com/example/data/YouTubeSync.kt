package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Loads real data from the YouTube Data API and stores it in the local Room cache.
 * All feeds (home, shorts, search, subscriptions, liked videos, playlists, comments) are real.
 */
class YouTubeSync(
    private val dao: YouTubeDao,
    private val api: YouTubeApi
) {
    // ---------------------------------------------------------------- parsing helpers

    private val avatarPalette = listOf(
        0xFFE53935L, 0xFF8E24AAL, 0xFF3949ABL, 0xFF00897BL, 0xFFF4511EL,
        0xFF6D4C41L, 0xFF039BE5L, 0xFF7CB342L, 0xFFD81B60L, 0xFF5E35B1L
    )

    private fun colorFor(key: String): Long =
        avatarPalette[(key.hashCode() and Int.MAX_VALUE) % avatarPalette.size]

    private fun parseMillis(iso: String?): Long {
        if (iso.isNullOrBlank() || iso.length < 19) return 0L
        return runCatching {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            f.timeZone = TimeZone.getTimeZone("UTC")
            f.parse(iso.substring(0, 19))?.time ?: 0L
        }.getOrDefault(0L)
    }

    private fun relativeTime(millis: Long): String {
        if (millis <= 0L) return ""
        val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0L) / 1000L
        fun unit(v: Long, name: String) = "$v $name${if (v == 1L) "" else "s"} ago"
        return when {
            diff < 60L -> "Just now"
            diff < 3_600L -> unit(diff / 60L, "minute")
            diff < 86_400L -> unit(diff / 3_600L, "hour")
            diff < 7L * 86_400L -> unit(diff / 86_400L, "day")
            diff < 30L * 86_400L -> unit(diff / (7L * 86_400L), "week")
            diff < 365L * 86_400L -> unit(diff / (30L * 86_400L), "month")
            else -> unit(diff / (365L * 86_400L), "year")
        }
    }

    private val durationRegex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")

    private fun parseDuration(iso: String?): Int {
        if (iso.isNullOrBlank()) return 0
        val m = durationRegex.matchEntire(iso) ?: return 0
        val h = m.groupValues[1].toIntOrNull() ?: 0
        val min = m.groupValues[2].toIntOrNull() ?: 0
        val s = m.groupValues[3].toIntOrNull() ?: 0
        return h * 3600 + min * 60 + s
    }

    private val chapterRegex = Regex("""^\W*(\d{1,3}:\d{2})\W*\s+(.+)$""")

    private fun extractChapters(description: String): String {
        val found = description.lines().mapNotNull { line ->
            chapterRegex.find(line.trim())?.let { it.groupValues[1] to it.groupValues[2].trim() }
        }
        if (found.size < 3) return ""
        val first = found.first().first
        if (first != "0:00" && first != "00:00") return ""
        return found.joinToString("|") { "${it.first} ${it.second.replace("|", "/")}" }
    }

    private fun categoryName(id: String): String = when (id) {
        "1" -> "Film"
        "2" -> "Autos"
        "10" -> "Music"
        "15" -> "Pets"
        "17" -> "Sports"
        "19" -> "Travel"
        "20" -> "Gaming"
        "22" -> "People"
        "23" -> "Comedy"
        "24" -> "Entertainment"
        "25" -> "News"
        "26" -> "Howto"
        "27" -> "Education"
        "28" -> "Tech"
        else -> "Other"
    }

    private fun parseChannel(item: JSONObject): ChannelEntity? {
        val id = item.optString("id").takeIf { it.isNotBlank() } ?: return null
        val sn = item.optJSONObject("snippet")
        val st = item.optJSONObject("statistics")
        val name = sn?.optString("title").orEmpty().ifBlank { "Unknown channel" }
        val custom = sn?.optString("customUrl").orEmpty()
        val handle = when {
            custom.startsWith("@") -> custom
            custom.isNotBlank() -> "@$custom"
            else -> "@" + name.replace(" ", "")
        }
        val hidden = st?.optBoolean("hiddenSubscriberCount") == true
        val subs = if (hidden) 0L else (st?.optString("subscriberCount")?.toLongOrNull() ?: 0L)
        val videos = st?.optString("videoCount")?.toIntOrNull() ?: 0
        return ChannelEntity(
            id = id,
            name = name,
            handle = handle,
            avatarColor = colorFor(id),
            bannerColor = colorFor(id.reversed()),
            subscriberCount = subs,
            videosCount = videos,
            bio = sn?.optString("description").orEmpty(),
            isVerified = false
        )
    }

    private fun parseVideo(item: JSONObject, channels: Map<String, ChannelEntity>): VideoEntity? {
        val id = item.optString("id").takeIf { it.isNotBlank() } ?: return null
        val sn = item.optJSONObject("snippet") ?: return null
        val liveState = sn.optString("liveBroadcastContent")
        if (liveState == "upcoming") return null
        val isLive = liveState == "live"
        val duration = if (isLive) 0 else parseDuration(item.optJSONObject("contentDetails")?.optString("duration"))
        val st = item.optJSONObject("statistics")
        val channelId = sn.optString("channelId")
        val channel = channels[channelId]
        val channelName = sn.optString("channelTitle").ifBlank { channel?.name.orEmpty() }
        val description = sn.optString("description")
        val thumbs = sn.optJSONObject("thumbnails")
        fun thumb(key: String): String? =
            thumbs?.optJSONObject(key)?.optString("url")?.takeIf { it.isNotBlank() }
        val thumbnail = thumb("maxres") ?: thumb("high") ?: thumb("medium") ?: thumb("default")
            ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        val isShort = !isLive && duration in 1..60
        val published = parseMillis(sn.optString("publishedAt"))
        val subsText = channel?.takeIf { it.subscriberCount > 0L }
            ?.let { "${formatCompactCount(it.subscriberCount)} subscribers" }.orEmpty()
        return VideoEntity(
            id = id,
            title = sn.optString("title"),
            description = description,
            videoUrl = if (isShort) "https://www.youtube.com/shorts/$id" else "https://www.youtube.com/watch?v=$id",
            thumbnailResName = "",
            customThumbnailUri = thumbnail,
            channelId = channelId,
            channelName = channelName,
            channelHandle = channel?.handle ?: ("@" + channelName.replace(" ", "")),
            channelAvatarColor = channel?.avatarColor ?: colorFor(channelId),
            subscriberCountText = subsText,
            viewsCount = st?.optString("viewCount")?.toLongOrNull() ?: 0L,
            likesCount = st?.optString("likeCount")?.toLongOrNull() ?: 0L,
            publishedTimeText = if (isLive) "Live now" else relativeTime(published),
            durationSeconds = duration,
            category = categoryName(sn.optString("categoryId")),
            isShort = isShort,
            isLive = isLive,
            chaptersJson = extractChapters(description)
        )
    }

    // ---------------------------------------------------------------- persistence

    private suspend fun upsertChannels(channels: List<ChannelEntity>) {
        if (channels.isEmpty()) return
        val merged = channels.map { c ->
            val old = dao.getChannelById(c.id)
            if (old == null) c else c.copy(
                isSubscribed = old.isSubscribed || c.isSubscribed,
                notificationBellMode = old.notificationBellMode
            )
        }
        dao.insertChannels(merged)
    }

    private suspend fun upsert(videos: List<VideoEntity>) {
        if (videos.isEmpty()) return
        val merged = videos.map { v ->
            val old = dao.getVideoById(v.id)
            if (old == null) v else v.copy(
                isLiked = old.isLiked,
                isDisliked = old.isDisliked,
                isSavedToWatchLater = old.isSavedToWatchLater,
                isDownloaded = old.isDownloaded,
                watchProgressFraction = old.watchProgressFraction,
                lastWatchedTimestamp = old.lastWatchedTimestamp,
                homeOrder = old.homeOrder,
                shortsOrder = old.shortsOrder,
                isUserUploaded = old.isUserUploaded
            )
        }
        dao.insertVideos(merged)
    }

    /** Turns raw API video items into entities (fetching missing channel info) and caches the channels. */
    private suspend fun buildVideos(rawItems: List<JSONObject>, token: String?): List<VideoEntity> {
        val items = rawItems.distinctBy { it.optString("id") }
        val channelIds = items.mapNotNull {
            it.optJSONObject("snippet")?.optString("channelId")?.takeIf { s -> s.isNotBlank() }
        }.distinct()
        val known = channelIds.mapNotNull { dao.getChannelById(it) }.associateBy { it.id }
        val missing = channelIds.filter { id ->
            val c = known[id]
            c == null || (c.subscriberCount == 0L && c.bio.isEmpty())
        }
        val fetched: List<ChannelEntity> = if (missing.isEmpty()) emptyList() else {
            try {
                api.channelsByIds(missing, token).mapNotNull { parseChannel(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList<ChannelEntity>()
            }
        }
        upsertChannels(fetched)
        val channelMap = known + fetched.associateBy { it.id }
        return items.mapNotNull { parseVideo(it, channelMap) }
    }

    // ---------------------------------------------------------------- feeds

    suspend fun refreshHome(token: String?, region: String, subscribedChannelIds: List<String>) {
        val ordered = ArrayList<String>()
        if (token != null && subscribedChannelIds.isNotEmpty()) {
            try {
                ordered += subscriptionFeed(token, subscribedChannelIds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // fall back to popular videos only
            }
        }
        var firstError: Exception? = null
        val items = ArrayList<JSONObject>()
        for (category in listOf<String?>(null, "10", "20", "28", "25", "17", "24")) {
            try {
                items += api.mostPopular(region, category, token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (category == null) firstError = e
            }
        }
        if (items.isEmpty() && ordered.isEmpty()) {
            throw firstError ?: IllegalStateException("YouTube returned no videos")
        }
        val built = buildVideos(items, token)
        upsert(built)
        for (v in built) if (!v.isShort) ordered += v.id
        dao.clearHomeOrder()
        ordered.distinct().forEachIndexed { index, id -> dao.setHomeOrder(id, index) }
    }

    private suspend fun subscriptionFeed(token: String, channelIds: List<String>): List<String> {
        val channels = api.channelsByIds(channelIds.take(40), token)
        val uploads = channels.mapNotNull {
            it.optJSONObject("contentDetails")?.optJSONObject("relatedPlaylists")
                ?.optString("uploads")?.takeIf { s -> s.isNotBlank() }
        }
        val entries = coroutineScope {
            uploads.chunked(8).flatMap { chunk ->
                chunk.map { playlistId ->
                    async {
                        try {
                            api.playlistItems(playlistId, token, 3)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            emptyList<JSONObject>()
                        }
                    }
                }.awaitAll().flatten()
            }
        }
        val videoIds = entries.mapNotNull { e ->
            val cd = e.optJSONObject("contentDetails") ?: return@mapNotNull null
            val vid = cd.optString("videoId").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            vid to parseMillis(cd.optString("videoPublishedAt"))
        }.sortedByDescending { it.second }.map { it.first }.distinct().take(60)
        if (videoIds.isEmpty()) return emptyList()
        val built = buildVideos(api.videosByIds(videoIds, token), token)
        upsert(built)
        val byId = built.associateBy { it.id }
        return videoIds.mapNotNull { byId[it] }.filter { !it.isShort }.map { it.id }
    }

    suspend fun refreshShorts(token: String?, region: String) {
        val ids = api.searchVideoIds("#shorts", region, true, token, 30)
        if (ids.isEmpty()) return
        val built = buildVideos(api.videosByIds(ids, token), token)
        val shorts = built.filter { it.durationSeconds in 1..60 }
            .ifEmpty { built.filter { it.durationSeconds in 1..180 } }
            .map { it.copy(isShort = true, videoUrl = "https://www.youtube.com/shorts/${it.id}") }
        upsert(shorts)
        dao.clearShortsOrder()
        shorts.forEachIndexed { index, v -> dao.setShortsOrder(v.id, index) }
    }

    suspend fun search(query: String, token: String?, region: String): List<VideoEntity> {
        val ids = api.searchVideoIds(query, region, false, token, 25)
        if (ids.isEmpty()) return emptyList()
        val built = buildVideos(api.videosByIds(ids, token), token)
        upsert(built)
        val byId = built.associateBy { it.id }
        return ids.mapNotNull { dao.getVideoById(it) ?: byId[it] }
    }

    // ---------------------------------------------------------------- account (needs OAuth token)

    /** Loads subscriptions, liked videos and playlists. Returns the subscribed channel ids (most relevant first). */
    suspend fun refreshLibrary(token: String): List<String> {
        val subs = api.mySubscriptions(token)
        val ids = subs.mapNotNull {
            it.optJSONObject("snippet")?.optJSONObject("resourceId")?.optString("channelId")
                ?.takeIf { s -> s.isNotBlank() }
        }.distinct()
        dao.clearSubscriptions()
        if (ids.isNotEmpty()) {
            val fetched = api.channelsByIds(ids, token).mapNotNull { parseChannel(it) }
            val fetchedIds = fetched.map { it.id }.toSet()
            val stubs = subs.mapNotNull { s ->
                val sn = s.optJSONObject("snippet") ?: return@mapNotNull null
                val cid = sn.optJSONObject("resourceId")?.optString("channelId").orEmpty()
                if (cid.isBlank() || cid in fetchedIds) return@mapNotNull null
                val name = sn.optString("title").ifBlank { "Unknown channel" }
                ChannelEntity(
                    id = cid, name = name, handle = "@" + name.replace(" ", ""),
                    avatarColor = colorFor(cid), bannerColor = colorFor(cid.reversed()),
                    subscriberCount = 0L, videosCount = 0, bio = "", isVerified = false
                )
            }
            upsertChannels((fetched + stubs).map { it.copy(isSubscribed = true) })
        }
        try {
            refreshLiked(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // liked videos are optional
        }
        try {
            refreshPlaylists(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // playlists are optional
        }
        return ids
    }

    private suspend fun refreshLiked(token: String) {
        val built = buildVideos(api.likedVideos(token), token)
        upsert(built)
        dao.clearLikes()
        built.forEach { dao.setLiked(it.id) }
    }

    private fun stablePlaylistId(id: String): Long = 1_000_000L + (id.hashCode().toLong() and 0x7fffffffL)

    private suspend fun refreshPlaylists(token: String) {
        val playlists = api.myPlaylists(token)
        val allIds = LinkedHashSet<String>()
        val rows = ArrayList<PlaylistEntity>()
        for (pl in playlists) {
            val pid = pl.optString("id")
            if (pid.isBlank()) continue
            val sn = pl.optJSONObject("snippet")
            val ids: List<String> = try {
                api.playlistItems(pid, token, 50).mapNotNull {
                    it.optJSONObject("contentDetails")?.optString("videoId")?.takeIf { s -> s.isNotBlank() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList<String>()
            }
            allIds += ids
            val created = parseMillis(sn?.optString("publishedAt"))
            rows += PlaylistEntity(
                id = stablePlaylistId(pid),
                title = sn?.optString("title").orEmpty(),
                description = sn?.optString("description").orEmpty(),
                privacy = (pl.optJSONObject("status")?.optString("privacyStatus") ?: "public")
                    .replaceFirstChar { it.uppercase() },
                videoIdsCsv = ids.joinToString(","),
                createdAt = if (created > 0L) created else System.currentTimeMillis()
            )
        }
        if (allIds.isNotEmpty()) {
            upsert(buildVideos(api.videosByIds(allIds.take(150), token), token))
        }
        if (rows.isNotEmpty()) dao.insertPlaylists(rows)
    }

    // ---------------------------------------------------------------- comments & actions

    suspend fun loadComments(videoId: String, token: String?) {
        val items = try {
            api.comments(videoId, token)
        } catch (e: YouTubeApiException) {
            dao.deleteCommentsForVideo(videoId)
            return
        }
        val rows = items.mapNotNull { thread ->
            val sn = thread.optJSONObject("snippet") ?: return@mapNotNull null
            val c = sn.optJSONObject("topLevelComment")?.optJSONObject("snippet") ?: return@mapNotNull null
            val author = c.optString("authorDisplayName")
            CommentEntity(
                videoId = videoId,
                authorName = author.removePrefix("@"),
                authorHandle = if (author.startsWith("@")) author else "@" + author.replace(" ", ""),
                authorAvatarColor = colorFor(author),
                text = c.optString("textDisplay"),
                timestampText = relativeTime(parseMillis(c.optString("publishedAt"))),
                likesCount = c.optInt("likeCount"),
                replyCount = sn.optInt("totalReplyCount")
            )
        }
        dao.deleteCommentsForVideo(videoId)
        dao.insertComments(rows)
    }

    suspend fun postComment(videoId: String, text: String, token: String) = api.postComment(videoId, text, token)

    suspend fun rate(videoId: String, rating: String, token: String) = api.rate(videoId, rating, token)

    suspend fun setSubscribed(channelId: String, subscribe: Boolean, token: String) {
        if (subscribe) api.subscribe(channelId, token) else api.unsubscribe(channelId, token)
    }
}
