package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class YouTubeApiException(val httpCode: Int, message: String) : Exception(message)

/** Thin client for the YouTube Data API v3 (no third-party dependencies). */
class YouTubeApi(
    private val apiKey: String?,
    private val appPackage: String
) {
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    suspend fun request(
        method: String,
        path: String,
        params: Map<String, String>,
        token: String?,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val query = buildString {
            for ((k, v) in params) {
                if (isNotEmpty()) append('&')
                append(enc(k)).append('=').append(enc(v))
            }
            if (token == null && !apiKey.isNullOrBlank()) {
                if (isNotEmpty()) append('&')
                append("key=").append(enc(apiKey))
            }
        }
        val conn = URL("$BASE/$path?$query").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Accept", "application/json")
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            } else {
                conn.setRequestProperty("X-Android-Package", appPackage)
                conn.setRequestProperty("X-Android-Cert", CERT_SHA1)
            }
            if (method == "POST" || method == "PUT") {
                conn.doOutput = true
                if (body != null) {
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                conn.outputStream.use { out ->
                    if (body != null) out.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching {
                    JSONObject(text).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP $code")
                throw YouTubeApiException(code, msg)
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun JSONObject.items(): List<JSONObject> {
        val arr = optJSONArray("items") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    suspend fun videosByIds(ids: List<String>, token: String?): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        for (chunk in ids.distinct().chunked(50)) {
            val json = request(
                "GET", "videos",
                mapOf(
                    "part" to "snippet,contentDetails,statistics",
                    "id" to chunk.joinToString(","),
                    "maxResults" to "50"
                ),
                token
            )
            out += json.items()
        }
        return out
    }

    suspend fun channelsByIds(ids: List<String>, token: String?): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        for (chunk in ids.distinct().chunked(50)) {
            val json = request(
                "GET", "channels",
                mapOf(
                    "part" to "snippet,statistics,contentDetails",
                    "id" to chunk.joinToString(","),
                    "maxResults" to "50"
                ),
                token
            )
            out += json.items()
        }
        return out
    }

    suspend fun mostPopular(region: String, categoryId: String?, token: String?, max: Int = 25): List<JSONObject> {
        val p = mutableMapOf(
            "part" to "snippet,contentDetails,statistics",
            "chart" to "mostPopular",
            "maxResults" to max.toString(),
            "regionCode" to region
        )
        if (categoryId != null) p["videoCategoryId"] = categoryId
        return request("GET", "videos", p, token).items()
    }

    suspend fun searchVideoIds(query: String, region: String, shortsOnly: Boolean, token: String?, max: Int = 25): List<String> {
        val p = mutableMapOf(
            "part" to "snippet",
            "type" to "video",
            "q" to query,
            "maxResults" to max.toString(),
            "regionCode" to region
        )
        if (shortsOnly) p["videoDuration"] = "short"
        return request("GET", "search", p, token).items().mapNotNull {
            it.optJSONObject("id")?.optString("videoId")?.takeIf { s -> s.isNotBlank() }
        }
    }

    suspend fun mySubscriptions(token: String, maxPages: Int = 3): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        var page: String? = null
        for (i in 0 until maxPages) {
            if (i > 0 && page == null) break
            val p = mutableMapOf(
                "part" to "snippet",
                "mine" to "true",
                "maxResults" to "50",
                "order" to "relevance"
            )
            val current = page
            if (current != null) p["pageToken"] = current
            val json = request("GET", "subscriptions", p, token)
            out += json.items()
            page = json.optString("nextPageToken").takeIf { s -> s.isNotBlank() }
        }
        return out
    }

    suspend fun likedVideos(token: String, max: Int = 50): List<JSONObject> =
        request(
            "GET", "videos",
            mapOf(
                "part" to "snippet,contentDetails,statistics",
                "myRating" to "like",
                "maxResults" to max.toString()
            ),
            token
        ).items()

    suspend fun myPlaylists(token: String): List<JSONObject> =
        request(
            "GET", "playlists",
            mapOf("part" to "snippet,status,contentDetails", "mine" to "true", "maxResults" to "25"),
            token
        ).items()

    suspend fun playlistItems(playlistId: String, token: String?, max: Int): List<JSONObject> =
        request(
            "GET", "playlistItems",
            mapOf("part" to "contentDetails", "playlistId" to playlistId, "maxResults" to max.toString()),
            token
        ).items()

    suspend fun comments(videoId: String, token: String?): List<JSONObject> =
        request(
            "GET", "commentThreads",
            mapOf(
                "part" to "snippet",
                "videoId" to videoId,
                "maxResults" to "40",
                "order" to "relevance",
                "textFormat" to "plainText"
            ),
            token
        ).items()

    suspend fun rate(videoId: String, rating: String, token: String) {
        request("POST", "videos/rate", mapOf("id" to videoId, "rating" to rating), token)
    }

    suspend fun subscribe(channelId: String, token: String) {
        val body = JSONObject().put(
            "snippet", JSONObject().put(
                "resourceId", JSONObject()
                    .put("kind", "youtube#channel")
                    .put("channelId", channelId)
            )
        )
        request("POST", "subscriptions", mapOf("part" to "snippet"), token, body)
    }

    suspend fun unsubscribe(channelId: String, token: String) {
        val id = request(
            "GET", "subscriptions",
            mapOf("part" to "id", "mine" to "true", "forChannelId" to channelId),
            token
        ).items().firstOrNull()?.optString("id")
        if (!id.isNullOrBlank()) {
            request("DELETE", "subscriptions", mapOf("id" to id), token)
        }
    }

    suspend fun postComment(videoId: String, text: String, token: String) {
        val body = JSONObject().put(
            "snippet", JSONObject()
                .put("videoId", videoId)
                .put(
                    "topLevelComment", JSONObject().put(
                        "snippet", JSONObject().put("textOriginal", text)
                    )
                )
        )
        request("POST", "commentThreads", mapOf("part" to "snippet"), token, body)
    }

    companion object {
        private const val BASE = "https://www.googleapis.com/youtube/v3"
        // SHA-1 of the app signing key (debug.keystore.base64), used when an Android-restricted API key is in use
        private const val CERT_SHA1 = "460C6E988EDB4CE3AFF5B6F1AADE3C613DF03485"
    }
}
