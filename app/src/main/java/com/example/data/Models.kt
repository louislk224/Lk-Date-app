package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int,
    val name: String,
    val age: Int,
    val bio: String,
    val avatarUrl: String,
    val isOnline: Boolean,
    val distanceInKm: Int,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int, // The profile ID this message belongs to (chat session)
    val text: String,
    val timestamp: Long,
    val isSentByMe: Boolean
) : Serializable

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Int,
    val contactName: String,
    val contactAvatarUrl: String,
    val callType: String, // "AUDIO" or "VIDEO"
    val status: String,    // "MISSED", "INCOMING", "OUTGOING", "REJECTED", "COMPLETED"
    val timestamp: Long,
    val durationSec: Int
) : Serializable
