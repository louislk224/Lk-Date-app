package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    private val profileDao = db.userProfileDao()
    private val messageDao = db.chatMessageDao()
    private val callLogDao = db.callLogDao()

    val allProfiles: Flow<List<UserProfile>> = profileDao.getAllProfiles()
    val allCallLogs: Flow<List<CallLog>> = callLogDao.getAllCallLogs()

    fun getMessagesForUser(userId: Int): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForUser(userId)
    }

    suspend fun insertMessage(message: ChatMessage) {
        messageDao.insertMessage(message)
        // Also update the last message in user profile to keep it in sync on the dashboard
        profileDao.updateLastMessage(message.userId, message.text, message.timestamp)
    }

    suspend fun insertCallLog(callLog: CallLog) {
        callLogDao.insertCallLog(callLog)
    }

    suspend fun getProfileById(id: Int): UserProfile? {
        return profileDao.getProfileById(id)
    }

    suspend fun insertProfiles(profiles: List<UserProfile>) {
        profileDao.insertProfiles(profiles)
    }

    suspend fun clearAllCallLogs() {
        callLogDao.clearAllCallLogs()
    }
}
