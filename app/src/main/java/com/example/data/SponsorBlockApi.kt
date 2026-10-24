package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.ceil

/** Real segments from the public SponsorBlock database (sponsor.ajay.app). */
object SponsorBlockApi {
    suspend fun fetchSegments(videoId: String): List<SponsorSegmentDoc> = withContext(Dispatchers.IO) {
        val categories = URLEncoder.encode(
            "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"music_offtopic\"]", "UTF-8"
        )
        val actions = URLEncoder.encode("[\"skip\"]", "UTF-8")
        val url = "https://sponsor.ajay.app/api/skipSegments?videoID=${URLEncoder.encode(videoId, "UTF-8")}" +
            "&categories=$categories&actionTypes=$actions"
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) {
                emptyList()
            } else {
                val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val seg = o.optJSONArray("segment") ?: return@mapNotNull null
                    val start = seg.optDouble(0, -1.0)
                    val end = seg.optDouble(1, -1.0)
                    if (start < 0.0 || end <= start) return@mapNotNull null
                    val category = when (o.optString("category")) {
                        "sponsor" -> "Sponsor"
                        "selfpromo" -> "SelfPromo"
                        "interaction" -> "Interaction"
                        "intro" -> "Intro"
                        "outro" -> "Outro"
                        "music_offtopic" -> "MusicOfftopic"
                        else -> return@mapNotNull null
                    }
                    SponsorSegmentDoc(
                        id = o.optString("UUID").ifBlank { "sb_${videoId}_$i" },
                        videoId = videoId,
                        submittedByUid = "sponsorblock.ajay.app",
                        category = category,
                        startTimeSec = start.toInt(),
                        endTimeSec = ceil(end).toInt(),
                        votes = o.optInt("votes")
                    )
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
