package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoEntity::class,
        ChannelEntity::class,
        CommentEntity::class,
        PlaylistEntity::class,
        CommunityPostEntity::class,
        SearchQueryEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class YouTubeDatabase : RoomDatabase() {
    abstract fun dao(): YouTubeDao

    companion object {
        @Volatile
        private var INSTANCE: YouTubeDatabase? = null

        fun getDatabase(context: Context): YouTubeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YouTubeDatabase::class.java,
                    "youtube_app_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
