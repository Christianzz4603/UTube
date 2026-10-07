package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UTubeCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private val sampleVideo = VideoEntity(
        id = "vid_quantum_phone",
        title = "The Transparent Glass Smartphone Is Actually Here!",
        description = "Review",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        thumbnailResName = "img_thumb_tech_1791326396393",
        channelId = "ch_mkbhd_style",
        channelName = "Apex Tech Lab",
        channelHandle = "@ApexTechLab",
        channelAvatarColor = 0xFFE53935,
        subscriberCountText = "19.4M subscribers",
        viewsCount = 4820000,
        likesCount = 248000,
        publishedTimeText = "5 hours ago",
        durationSeconds = 15,
        category = "Tech"
    )

    @Test
    fun syncWatchHistory_authenticatedOwner_createsAndReadsDocument() = runBlocking {
        signInTestUser("alice@utube.test")
        val repository = UTubeCloudRepository(firestore)

        val syncRes = withTimeout(5000L) {
            repository.syncWatchHistoryEntry(sampleVideo, 0.65f)
        }
        assertTrue(syncRes.isSuccess)

        val listRes = withTimeout(5000L) {
            repository.getUserWatchHistory()
        }
        assertTrue(listRes.isSuccess)
        assertTrue(listRes.getOrThrow().any { it.videoId == "vid_quantum_phone" })
    }

    @Test
    fun getWatchHistoryById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser("alice2@utube.test")
        val aliceRepo = UTubeCloudRepository(firestore)
        val docId = withTimeout(5000L) {
            aliceRepo.syncWatchHistoryEntry(sampleVideo, 0.5f).getOrThrow()
        }

        signInTestUser("bob2@utube.test")
        val bobRepo = UTubeCloudRepository(firestore)
        val bobRead = withTimeout(5000L) {
            bobRepo.getWatchHistoryById(docId)
        }
        assertTrue(bobRead.isFailure)
        val ex = bobRead.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeUserWatchHistory_unauthenticated_failsWithError() = runBlocking {
        auth.signOut()
        val repository = UTubeCloudRepository(firestore)
        try {
            withTimeout(3000L) {
                repository.observeUserWatchHistory().first()
            }
            fail("Expected unauthenticated access to fail")
        } catch (expected: Exception) {
            assertTrue(expected is IllegalStateException || expected is FirebaseFirestoreException)
        }
    }

    @Test
    fun submitSponsorSegment_authenticatedUser_succeedsAndEmits() = runBlocking {
        signInTestUser("alice3@utube.test")
        val repository = UTubeCloudRepository(firestore)

        val submitRes = withTimeout(5000L) {
            repository.submitSponsorSegment(
                videoId = "vid_quantum_phone",
                category = "Sponsor",
                startTimeSec = 2,
                endTimeSec = 6
            )
        }
        assertTrue(submitRes.isSuccess)

        val segments = withTimeout(3000L) {
            repository.observeSponsorSegments("vid_quantum_phone").first { it.isNotEmpty() }
        }
        assertTrue(segments.any { it.category == "Sponsor" })
    }
}
