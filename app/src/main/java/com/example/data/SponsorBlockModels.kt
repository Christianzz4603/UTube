package com.example.data

import androidx.compose.ui.graphics.Color

enum class SponsorCategory(
    val key: String,
    val displayName: String,
    val colorHex: Long,
    val description: String
) {
    SPONSOR("Sponsor", "Sponsor", 0xFF00D400, "Paid promotion, unpaid referrals, and direct advertisements"),
    SELF_PROMO("SelfPromo", "Unpaid / Self Promotion", 0xFFFFFF00, "Merchandise, Patreon, or channel plugs"),
    INTERACTION("Interaction", "Interaction Reminder", 0xFFCC00FF, "Reminders to like, subscribe, or follow"),
    INTRO("Intro", "Intermission / Intro Animation", 0xFF00FFFF, "Intro sequence or static interval without content"),
    OUTRO("Outro", "Endcards / Credits", 0xFF0202ED, "Credits or endcard segments"),
    MUSIC_OFFTOPIC("MusicOfftopic", "Music: Non-Music Section", 0xFFFF9900, "Skits or non-music intros in music videos");

    companion object {
        fun fromKey(key: String): SponsorCategory {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: SPONSOR
        }
    }
}

enum class SponsorSkipBehavior(val label: String) {
    AUTO_SKIP("Auto-Skip"),
    SHOW_SKIP_BUTTON("Show Skip Button"),
    SHOW_IN_SEEKBAR_ONLY("Show in Seekbar Only"),
    DISABLED("Disable")
}

object DefaultSponsorSegments {
    fun getDefaultSegmentsForVideo(videoId: String, durationSeconds: Int): List<SponsorSegmentDoc> {
        val total = durationSeconds.coerceAtLeast(15)
        return when (videoId) {
            "vid_quantum_phone" -> listOf(
                SponsorSegmentDoc(
                    id = "def_sb_1",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "Intro",
                    startTimeSec = 0,
                    endTimeSec = 2,
                    votes = 142
                ),
                SponsorSegmentDoc(
                    id = "def_sb_2",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "Sponsor",
                    startTimeSec = 5,
                    endTimeSec = 8,
                    votes = 389
                ),
                SponsorSegmentDoc(
                    id = "def_sb_3",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "Outro",
                    startTimeSec = (total - 2).coerceAtLeast(10),
                    endTimeSec = total,
                    votes = 98
                )
            )
            "vid_cyber_engine" -> listOf(
                SponsorSegmentDoc(
                    id = "def_sb_4",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "Sponsor",
                    startTimeSec = 3,
                    endTimeSec = 7,
                    votes = 512
                ),
                SponsorSegmentDoc(
                    id = "def_sb_5",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "Interaction",
                    startTimeSec = 11,
                    endTimeSec = 14,
                    votes = 204
                )
            )
            "vid_ramen_masterclass" -> listOf(
                SponsorSegmentDoc(
                    id = "def_sb_6",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "SelfPromo",
                    startTimeSec = 4,
                    endTimeSec = 7,
                    votes = 176
                )
            )
            else -> listOf(
                SponsorSegmentDoc(
                    id = "def_sb_generic_$videoId",
                    videoId = videoId,
                    submittedByUid = "system_sb",
                    category = "Sponsor",
                    startTimeSec = (total * 0.25).toInt().coerceAtLeast(2),
                    endTimeSec = (total * 0.45).toInt().coerceAtLeast(5),
                    votes = 84
                )
            )
        }
    }
}
