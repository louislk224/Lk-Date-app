package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserProfile::class, ChatMessage::class, CallLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lk_date_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-seed database with dating profiles
                        INSTANCE?.let { appDb ->
                            CoroutineScope(Dispatchers.IO).launch {
                                seedDatabase(appDb)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val profiles = listOf(
                UserProfile(
                    id = 1,
                    name = "Priya",
                    age = 23,
                    bio = "I love filter coffee, slow music, and deep conversations. Let's talk! ☕✨",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                    isOnline = true,
                    distanceInKm = 4,
                    lastMessageText = "Hi there! Welcome to LK Date 🌸",
                    lastMessageTime = System.currentTimeMillis() - 600000
                ),
                UserProfile(
                    id = 2,
                    name = "Sneha",
                    age = 22,
                    bio = "UX designer and occasional dancer. Always up for a surprise video call! 💃🎨",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
                    isOnline = true,
                    distanceInKm = 12,
                    lastMessageText = "Hey! Let's get to know each other, video call me anytime!",
                    lastMessageTime = System.currentTimeMillis() - 1200000
                ),
                UserProfile(
                    id = 3,
                    name = "Keerthi",
                    age = 24,
                    bio = "Software engineer looking for meaningful connections. Love reading books and visiting cafes. 📚🍃",
                    avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300",
                    isOnline = false,
                    distanceInKm = 8,
                    lastMessageText = "Tell me about your favorite movies!",
                    lastMessageTime = System.currentTimeMillis() - 3600000
                ),
                UserProfile(
                    id = 4,
                    name = "Vijay",
                    age = 26,
                    bio = "Fitness Trainer & Travel Blogger. Let's go on exciting weekend getaways. 🏃‍♂️✈️",
                    avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300",
                    isOnline = true,
                    distanceInKm = 3,
                    lastMessageText = "Up for a workout chat or cafe date?",
                    lastMessageTime = System.currentTimeMillis() - 7200000
                ),
                UserProfile(
                    id = 5,
                    name = "Deepika",
                    age = 25,
                    bio = "Art lover and pet parent 🐾. Looking for sweet, genuine friendship that grows.",
                    avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300",
                    isOnline = false,
                    distanceInKm = 15,
                    lastMessageText = "Wish you a great day ahead!",
                    lastMessageTime = System.currentTimeMillis() - 86400000
                )
            )
            db.userProfileDao().insertProfiles(profiles)

            // Seed initial mock chat messages to make history feel rich!
            val messages = listOf(
                ChatMessage(userId = 1, text = "Hey there! I am Priya.", timestamp = System.currentTimeMillis() - 700000, isSentByMe = false),
                ChatMessage(userId = 1, text = "Nice to meet you! Tell me more about yourself.", timestamp = System.currentTimeMillis() - 650000, isSentByMe = true),
                ChatMessage(userId = 1, text = "Hi there! Welcome to LK Date 🌸", timestamp = System.currentTimeMillis() - 600000, isSentByMe = false),

                ChatMessage(userId = 2, text = "Hey, nice profile! Do you like video calls?", timestamp = System.currentTimeMillis() - 1300000, isSentByMe = false),
                ChatMessage(userId = 2, text = "Hey! Let's get to know each other, video call me anytime!", timestamp = System.currentTimeMillis() - 1200000, isSentByMe = false)
            )

            for (msg in messages) {
                db.chatMessageDao().insertMessage(msg)
            }
        }
    }
}
