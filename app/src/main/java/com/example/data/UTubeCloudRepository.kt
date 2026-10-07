package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

data class UserWatchHistoryDoc(
    val id: String = "",
    val userId: String = "",
    val videoId: String = "",
    val title: String = "",
    val channelName: String = "",
    val thumbnailResName: String = "",
    val durationSeconds: Int = 0,
    val progressFraction: Float = 0f,
    val isLiked: Boolean = false,
    val isSavedToWatchLater: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class SponsorSegmentDoc(
    val id: String = "",
    val videoId: String = "",
    val submittedByUid: String = "",
    val category: String = "Sponsor",
    val startTimeSec: Int = 0,
    val endTimeSec: Int = 5,
    val votes: Int = 1,
    val createdAt: Timestamp? = null
)

class UTubeCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserWatchHistory(): Flow<List<UserWatchHistoryDoc>> = flow {
        val uid = requireUserId()
        val path = "user_watch_history"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.map { doc -> doc.toUserWatchHistoryDoc() }
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getUserWatchHistory(): Result<List<UserWatchHistoryDoc>> = runCatching {
        val uid = requireUserId()
        val path = "user_watch_history"
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.documents.map { it.toUserWatchHistoryDoc() }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getWatchHistoryById(docId: String): Result<UserWatchHistoryDoc?> = runCatching {
        val path = "user_watch_history/$docId"
        try {
            val doc = db.collection("user_watch_history").document(docId).get().await()
            if (doc.exists()) doc.toUserWatchHistoryDoc() else null
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun syncWatchHistoryEntry(
        video: VideoEntity,
        progressFraction: Float = video.watchProgressFraction.coerceIn(0.05f, 1f)
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "wh_${uid}_${video.id}_${System.currentTimeMillis()}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docRef = db.collection("user_watch_history").document(docId)
        val createMap = mapOf(
            "userId" to uid,
            "videoId" to video.id.take(90),
            "title" to video.title.take(280),
            "channelName" to video.channelName.take(140),
            "thumbnailResName" to video.thumbnailResName.take(280),
            "durationSeconds" to video.durationSeconds.coerceIn(0, 86400),
            "progressFraction" to progressFraction.toDouble().coerceIn(0.0, 1.0),
            "isLiked" to video.isLiked,
            "isSavedToWatchLater" to video.isSavedToWatchLater,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(createMap).await()
            docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun clearAllCloudWatchHistory(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val path = "user_watch_history"
        try {
            val snap = db.collection(path).whereEqualTo("userId", uid).get().await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    fun observeSponsorSegments(videoId: String): Flow<List<SponsorSegmentDoc>> = flow {
        val path = "sponsor_segments"
        emitAll(
            db.collection(path)
                .whereEqualTo("videoId", videoId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.map { it.toSponsorSegmentDoc() }
                        .sortedBy { it.startTimeSec }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun submitSponsorSegment(
        videoId: String,
        category: String,
        startTimeSec: Int,
        endTimeSec: Int
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val safeStart = startTimeSec.coerceAtLeast(0)
        val safeEnd = endTimeSec.coerceAtLeast(safeStart + 1)
        val docId = "sb_${videoId}_${safeStart}_${safeEnd}_${System.currentTimeMillis()}"
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docRef = db.collection("sponsor_segments").document(docId)
        val payload = mapOf(
            "videoId" to videoId.take(90),
            "submittedByUid" to uid,
            "category" to category,
            "startTimeSec" to safeStart,
            "endTimeSec" to safeEnd,
            "votes" to 1,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun upvoteSponsorSegment(segment: SponsorSegmentDoc): Result<Unit> = runCatching {
        val docRef = db.collection("sponsor_segments").document(segment.id)
        try {
            docRef.update(mapOf("votes" to segment.votes + 1)).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    private fun DocumentSnapshot.toUserWatchHistoryDoc(): UserWatchHistoryDoc {
        return UserWatchHistoryDoc(
            id = id,
            userId = getString("userId") ?: "",
            videoId = getString("videoId") ?: "",
            title = getString("title") ?: "",
            channelName = getString("channelName") ?: "",
            thumbnailResName = getString("thumbnailResName") ?: "",
            durationSeconds = (getLong("durationSeconds") ?: 0L).toInt(),
            progressFraction = (getDouble("progressFraction") ?: 0.0).toFloat(),
            isLiked = getBoolean("isLiked") ?: false,
            isSavedToWatchLater = getBoolean("isSavedToWatchLater") ?: false,
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun DocumentSnapshot.toSponsorSegmentDoc(): SponsorSegmentDoc {
        return SponsorSegmentDoc(
            id = id,
            videoId = getString("videoId") ?: "",
            submittedByUid = getString("submittedByUid") ?: "",
            category = getString("category") ?: "Sponsor",
            startTimeSec = (getLong("startTimeSec") ?: 0L).toInt(),
            endTimeSec = (getLong("endTimeSec") ?: 5L).toInt(),
            votes = (getLong("votes") ?: 0L).toInt(),
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }
}
